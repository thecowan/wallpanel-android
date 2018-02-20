package org.wallpanelproject.android;

import android.annotation.SuppressLint;
import android.net.http.SslError;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.os.Bundle;
import android.webkit.WebViewClient;

import org.wallpanelproject.android.R;

public class BrowserActivityNative extends BrowserActivity {
    private WebView mWebView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setContentView(R.layout.activity_browser);
        mWebView = (WebView) findViewById(R.id.activity_browser_webview_native);
        mWebView.setVisibility(View.VISIBLE);

        // WebChromeClient overrides
        mWebView.setWebChromeClient(new WebChromeClient(){

            Snackbar snackbar;

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (!displayProgress) return;

                if(newProgress == 100 && snackbar != null){
                    snackbar.dismiss();
                    pageLoadComplete(view.getUrl());
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

        // WebViewClient overrides
        mWebView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                Log.i(TAG, "SSL Error reported and ignored");
                handler.proceed(); // Ignore SSL certificate errors
                //TODO: add option to enable/disable this behavior
            }
        });

        mWebView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        resetScreen();
                    case MotionEvent.ACTION_UP:
                        if (!v.hasFocus()) {
                            v.requestFocus();
                        }
                        break;
                }
                return false;
            }
        });

        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        Log.i(TAG, webSettings.getUserAgentString());

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void loadUrl(final String url) {
        if (zoomLevel != 1.0) { mWebView.setInitialScale((int)(zoomLevel * 100)); }
        mWebView.loadUrl(url);
    }

    @Override
    protected void evaluateJavascript(final String js) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mWebView.evaluateJavascript(js, null);
        }
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
