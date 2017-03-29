package de.rhuber.homedash;

import android.annotation.SuppressLint;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.os.Bundle;
import android.webkit.WebViewClient;

public class BrowserActivityNative extends BrowserActivity {
    private WebView mWebView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setContentView(R.layout.activity_browser_native);
        mWebView = (WebView) findViewById(R.id.activity_browser_webview);

        // Force links and redirects to open in the WebView instead of in a browser
        mWebView.setWebChromeClient(new WebChromeClient(){

            Snackbar snackbar;

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (!displayProgress) return;

                if(newProgress == 100 && snackbar != null){
                    snackbar.dismiss();
                    return;
                }
                String text = "Loading "+ newProgress+ "% " + view.getUrl();
                if(snackbar == null){
                    snackbar = Snackbar.make(view, text, Snackbar.LENGTH_INDEFINITE);
                } else {
                    snackbar.setText(text);
                }
                snackbar.show();
            }

        });

        mWebView.setWebViewClient(new WebViewClient(){
            //If you will not use this method url links are open in new browser not in webview

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return true;
            }

        });

        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAppCacheEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        Log.i(TAG, webSettings.getUserAgentString());

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void loadUrl(final String url) {
        mWebView.loadUrl(url);
    }

    @Override
    protected void evaluateJavascript(final String js) {
        mWebView.evaluateJavascript(js, null);
    }

    @Override
    protected void clearCache() {
        mWebView.clearCache(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().removeAllCookies(null);
        }
    }

    @Override
    protected void reload() {
        mWebView.reload();
    }
}
