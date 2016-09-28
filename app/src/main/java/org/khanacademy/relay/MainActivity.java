package org.khanacademy.relay;

import org.json.JSONException;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements RelayContainer<Human> {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new GraphQLVirtualMachine((WebView) findViewById(R.id.webview))
                .renderDataFor(this)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(final String s) {
                        try {
                            renderScreen(Human.fromJsonString(s));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    @Override
    public String getQueryFragment() {
        return "{ human(id: \"1000\") { id name homePlanet } }";
    }

    @Override
    public void renderScreen(final Human human) {
        System.out.println("Rendering screen with: " + human.homePlanet);
        ((TextView) findViewById(R.id.home_planet)).setText(human.homePlanet);
    }
}
