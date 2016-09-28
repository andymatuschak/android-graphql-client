package org.khanacademy.relay;

import org.json.JSONException;
import rx.Completable;
import rx.Single;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;

import android.net.Uri;
import android.support.annotation.Keep;
import android.util.Pair;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Created by charliemarsh on 9/28/16.
 */

public class GraphQLVirtualMachine {
    private static final Uri BASE_TEMPLATE_URI = Uri.parse(
            "file:///android_asset/webview/templates/"
    );

    @JavascriptInterface
    @Keep
    public void respond(final String jsonString, final String callId) {
        System.out.println("Received response: " + jsonString + ", " + callId);
        mResultSubject.onNext(Pair.create(jsonString, callId));
    }

    private final BehaviorSubject<Boolean> mInitializationSubject = BehaviorSubject.create(false);

    private final WebView mWebView;

    public GraphQLVirtualMachine(final WebView webView) {
        mWebView = webView;

        // Enable the appropriate settings.
        final WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setDatabaseEnabled(true);

        // Load the template, which initializes all the JS, etc.
        mWebView.loadUrl(BASE_TEMPLATE_URI
                .buildUpon()
                .appendEncodedPath("relay.html")
                .build()
                .toString());

        mWebView.addJavascriptInterface(this, "nativeHost");

        mWebView.setWebViewClient(new WebViewClient(){
            public void onPageFinished(final WebView view, final String url){
                mInitializationSubject.onNext(true);
            }
        });
    }

    private static final String FETCH_METHOD = "Executor.execute";
    private final BehaviorSubject<Pair<String, String>> mResultSubject = BehaviorSubject.create();
    private int methodCounter = 0;

    public Single<String> renderDataFor(final RelayContainer<Human> relayContainer) {
        final String methodId = String.valueOf(methodCounter++);
        final String javascript = FETCH_METHOD + "('" + relayContainer.getQueryFragment() + "', '" + methodId + "');";

        mInitializationSubject.filter(new Func1<Boolean, Boolean>() {
            @Override
            public Boolean call(final Boolean aBoolean) {
                return aBoolean;
            }
        }).first().subscribe(new Action1<Boolean>() {
            @Override
            public void call(final Boolean aBoolean) {
                System.out.println("Evaluating JavaScript: " + javascript);
                mWebView.evaluateJavascript(javascript, new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(final String s) {}
                });
            }
        });

        return mResultSubject.filter(new Func1<Pair<String, String>, Boolean>() {
            @Override
            public Boolean call(Pair<String, String> stringStringPair) {
                return stringStringPair.second.equals(methodId);
            }
        })
                .map(new Func1<Pair<String,String>, String>() {
                    @Override
                    public String call(Pair<String, String> stringStringPair) {
                        System.out.println("Mapping");
                        return stringStringPair.first;
                    }
                })
                .first()
                .toSingle();
    }
}
