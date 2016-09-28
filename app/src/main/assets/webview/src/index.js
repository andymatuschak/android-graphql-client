const DataLoader = require('dataloader');
const GraphQL = require('graphql');
const request = require('request');

/* GraphQL schema. */
const humanType = new GraphQL.GraphQLObjectType({
  name: 'Human',
  description: 'A humanoid creature in the Star Wars universe.',
  fields: () => ({
    id: {
      type: new GraphQL.GraphQLNonNull(GraphQL.GraphQLString),
      description: 'The id of the human.',
    },
    name: {
      type: GraphQL.GraphQLString,
      description: 'The name of the human.',
    },
    homePlanet: {
      type: GraphQL.GraphQLString,
      description: 'The home planet of the human, or null if unknown.',
    },
  }),
});

const queryType = new GraphQL.GraphQLObjectType({
  name: 'Query',
  fields: () => ({
    human: {
      type: humanType,
      args: {
        id: {
          description: 'id of the human',
          type: new GraphQL.GraphQLNonNull(GraphQL.GraphQLString)
        }
      },
      resolve: (root, args) => humansLoader.load(args.id),
    },
  })
});

const schema = new GraphQL.GraphQLSchema({
  query: queryType,
  types: [humanType],
});

/* Fetching. */
const db = window.openDatabase('content_db', '1.0', 'Content database', 2 * 1024 * 1024);

const fetchAndWriteHumanWithId = (id) => {
  return new Promise(resolve => {
    const allHumanFields = Object.keys(humanType.getFields());
    const allDataQuery = '{ human(id: "' + id + '") { ' + allHumanFields.join(' ') + ' } }';

    // Kick off the network request, for all fields for the given object.
    console.log("Querying network with:", allDataQuery);
    request({
      url: 'http://10.0.2.2:8080/graphql',
      method: 'POST',
      json: true,
      body: { query: allDataQuery },
    }, (error, response, body) => {
      console.log("Received response from API: " + body);

      // Write the response to the DB.
      db.transaction(function(tx) {
        const data = body.data.human;
        const fieldString = allHumanFields.join();
        const valueString = allHumanFields.map(field => '"' + data[field] + '"').join();
        const query = `INSERT INTO humans (${fieldString}) VALUES (${valueString})`;

        console.log("Writing to disk with query:", query);
        tx.executeSql(query, [], function(tx, result) {
          resolve();
        });
      });
    });
  });
}

/* DataLoader. */
const humansLoader = new DataLoader(ids => {
  // Ugliness. We first need to check which IDs are already in the DB. We could
  // improve this, I'm sure.
  const allIdsPromise = new Promise(resolve => {
    db.transaction(tx => {
      tx.executeSql('SELECT id FROM humans', [], (tx, result) => {
        const allIds = [];
        for (let i = 0; i < result.rows.length; i++) {
          allIds.push(result.rows.item(i).id);
        }
        resolve(allIds);
      })
    });
  });

  // Fetch all the missing IDs in parallel. Surely there's a way to do this
  // with a single query. It's not obvious, though...
  const fetchMissingIdsPromise = allIdsPromise.then(presentIds => {
    const idsToFetch = ids.filter(id => presentIds.indexOf(id) === -1);
    return Promise.all(idsToFetch.map(fetchAndWriteHumanWithId));
  });

  return fetchMissingIdsPromise.then(() => {
    const params = ids.map(id => '?' ).join();
    const query = `SELECT * FROM humans WHERE id IN (${params})`;
    return queryLoader.load([query, ids]).then(
      rows => ids.map(
        id => {
          for (let i = 0; i < rows.length; i++) {
            if (rows.item(i).id === id) {
              return rows.item(i);
            }
          }
          throw new Error(`Row not found: ${id}`);
        }
      )
    );
  });
});

const queryLoader = new DataLoader(queries => new Promise(resolve => {
  let waitingOn = queries.length;
  const results = [];
  db.transaction((tx) => {
    queries.forEach((query, index) => {
      tx.executeSql.apply(tx, query.concat((tx, result) => {
        results[index] = result.rows;
        if (--waitingOn === 0) {
          resolve(results);
        }
      }));
    });
  });
}), { cache: false });

// /* Relay -- why do we need this? */
// const Relay = require('relay');
// const BaseNetworkLayer = new Relay.DefaultNetworkLayer('http://10.0.2.2:8080/graphql');

// const localFirstNetworkLayer = {
//   sendMutation(mutationRequest) {
//     throw new Error("We don't support mutations.");
//   },
//   sendQueries(queryRequests) {
//     for (let query of queryRequests) {
//       const queryString = query.getQueryString();
//       GraphQL.graphql(schema, query).then(data => console.log(data));
//     }
//   },
//   supports(...options) {
//     // ...
//   },
// };

window.Executor = {
  execute(query, callId) {
    GraphQL.graphql(schema, query).then(data => {
      window.nativeHost.respond(JSON.stringify(data.data.human), callId);
    });
  },
};
