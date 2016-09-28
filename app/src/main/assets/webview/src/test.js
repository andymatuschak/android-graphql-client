var request = require('request');
var ids = ["1001"];
var allHumanFields = ['id', 'name', 'homePlanet'];
var allDataQuery = '{ human(id: "' + ids[0] + '") { ' + allHumanFields.join(' ') + ' } }';
console.log("Executing query:", allDataQuery);
request({
    url: 'http://localhost:4000/graphql',
    method: 'POST',
    json: true,
    body: { query: allDataQuery },
    }, function (error, response, body){
    console.log(body);
});