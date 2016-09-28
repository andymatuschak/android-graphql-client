package org.khanacademy.relay;

import org.json.JSONException;
import org.json.JSONObject;

public class Human {
    public final String id;
    public final String name;
    public final String homePlanet;

    public Human(final String id, final String name, final String homePlanet) {
        this.id = id;
        this.name = name;
        this.homePlanet = homePlanet;
    }

    public static Human fromJsonString(final String jsonString) throws JSONException {
        final JSONObject jsonObject = new JSONObject(jsonString);
        return new Human(
                jsonObject.getString("id"),
                jsonObject.getString("name"),
                jsonObject.getString("homePlanet")
        );
    }
}
