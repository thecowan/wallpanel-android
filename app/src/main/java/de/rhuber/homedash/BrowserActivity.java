package de.rhuber.homedash;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class BrowserActivity extends AppCompatActivity  {
    public static final String BROADCAST_ACTION_LOAD_URL = "BROADCAST_ACTION_LOAD_URL";
    public static final String BROADCAST_ACTION_SCREEN_ON = "BROADCAST_ACTION_SCREEN_ON";
    public static final String BROADCAST_ACTION_JS_EXEC = "BROADCAST_ACTION_JS_EXEC";
    public static final String BROADCAST_ACTION_CLEAR_BROWSER_CACHE = "BROADCAST_ACTION_CLEAR_BROWSER_CACHE";

    private final  String TAG = BrowserActivity.class.getName();


    final private String DEBUG_TAG = BrowserActivity.class.getName();
    private WebView mWebView;
    View decorView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);

        mWebView = (WebView) findViewById(R.id.activity_browser_webview);

        // Force links and redirects to open in the WebView instead of in a browser
        mWebView.setWebChromeClient(new WebChromeClient(){


            Snackbar snackbar;

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if(newProgress == 100 && snackbar != null){
                    snackbar.dismiss();
                    return;
                }
                String text = "Loading "+ newProgress+ "% " + view.getUrl().toString();
                if(snackbar == null){
                    snackbar = Snackbar.make(view, text, Snackbar.LENGTH_INDEFINITE);
                } else {
                    snackbar.setText(text);
                }
                snackbar.show();
            }

        });

        mWebView.setWebViewClient(new WebViewClient(){
            //If you will not use this method url links are opeen in new brower not in webview

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return true;
            }

        });

        // Enable Javascript
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setMixedContentMode(webSettings.MIXED_CONTENT_ALWAYS_ALLOW);


        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String url = sharedPreferences.getString(getString(R.string.key_setting_startup_url),"");
        mWebView.loadUrl(url);
        Log.i("WebView", webSettings.getUserAgentString());

        decorView = getWindow().getDecorView();
        // Hide both the navigation bar and the status bar.
// SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
// a general rule, you should design your app to hide the status bar whenever you
// hide the navigation bar.
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN ;
        decorView.setSystemUiVisibility(uiOptions);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BROADCAST_ACTION_LOAD_URL);
        filter.addAction(BROADCAST_ACTION_SCREEN_ON);
        filter.addAction(BROADCAST_ACTION_JS_EXEC);
        filter.addAction(BROADCAST_ACTION_CLEAR_BROWSER_CACHE);
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(this);
        bm.registerReceiver(mBroadcastReceiver, filter);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
    }

    // handler for received data from service
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BROADCAST_ACTION_LOAD_URL)) {
                final String url = intent.getStringExtra(BROADCAST_ACTION_LOAD_URL);
                mWebView.loadUrl(url);
            }
            if (intent.getAction().equals(BROADCAST_ACTION_JS_EXEC)) {
                final String js = intent.getStringExtra(BROADCAST_ACTION_JS_EXEC);
                Log.d(TAG, "Executing javascript in current browser: " +js);
                mWebView.evaluateJavascript(js,null);
            }

            if (intent.getAction().equals(BROADCAST_ACTION_CLEAR_BROWSER_CACHE)) {
                mWebView.clearCache(true);
                CookieManager.getInstance().removeAllCookies(null);
                Log.i(TAG, "Browser cache cleared.");
            }
            if (intent.getAction().equals(BROADCAST_ACTION_SCREEN_ON)) {
                screenOn();
            }
        }
    };

    private final int wakeupWindowFlags =  WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON;

    public void screenOn(){
        final Window win = getWindow();
        win.addFlags(wakeupWindowFlags);


        PowerManager pm = (PowerManager) getSystemService(getApplicationContext().POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK|PowerManager.ACQUIRE_CAUSES_WAKEUP|PowerManager.ON_AFTER_RELEASE, "ScreenWakeup");
        wl.acquire();
        wl.release();
    }

    @Override
    protected void onPause() {
        super.onPause();

        final Window win = getWindow();
        win.clearFlags(wakeupWindowFlags);
    }
}
