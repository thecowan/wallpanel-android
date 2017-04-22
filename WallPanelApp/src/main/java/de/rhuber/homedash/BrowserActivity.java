package de.rhuber.homedash;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

abstract class BrowserActivity extends AppCompatActivity  {
    public static final String BROADCAST_ACTION_LOAD_URL = "BROADCAST_ACTION_LOAD_URL";
    public static final String BROADCAST_ACTION_JS_EXEC = "BROADCAST_ACTION_JS_EXEC";
    public static final String BROADCAST_ACTION_CLEAR_BROWSER_CACHE = "BROADCAST_ACTION_CLEAR_BROWSER_CACHE";
    public static final String BROADCAST_ACTION_RELOAD_PAGE = "BROADCAST_ACTION_RELOAD_PAGE";

    final String TAG = BrowserActivity.class.getName();

    private View decorView;

    boolean displayProgress = true;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Config config = new Config(this.getApplicationContext());

        displayProgress = config.getAppShowActivity();

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
        filter.addAction(BROADCAST_ACTION_JS_EXEC);
        filter.addAction(BROADCAST_ACTION_CLEAR_BROWSER_CACHE);
        filter.addAction(BROADCAST_ACTION_RELOAD_PAGE);
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(this);
        bm.registerReceiver(mBroadcastReceiver, filter);

        String url = config.getAppLaunchUrl();
        loadUrl(url);
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
                Log.i(TAG, "Browsing to " + url);
                loadUrl(url);
            }
            if (intent.getAction().equals(BROADCAST_ACTION_JS_EXEC)) {
                final String js = intent.getStringExtra(BROADCAST_ACTION_JS_EXEC);
                Log.i(TAG, "Executing javascript in current browser: " +js);
                evaluateJavascript(js);
            }
            if (intent.getAction().equals(BROADCAST_ACTION_CLEAR_BROWSER_CACHE)) {
                Log.i(TAG, "Clearing browser cache");
                clearCache();
            }
            if (intent.getAction().equals(BROADCAST_ACTION_RELOAD_PAGE)) {
                Log.i(TAG, "Browser page reloading.");
                reload();
            }
        }
    };

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            Log.i(TAG, "Back button pressed");
            finish();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    void pageLoadComplete(final String url) {
        Log.d(TAG, "pageLoadComplete Called");
        Intent intent = new Intent(WallPanelService.BROADCAST_EVENT_URL_CHANGE);
        intent.putExtra(WallPanelService.BROADCAST_EVENT_URL_CHANGE, url);
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getApplicationContext());
        bm.sendBroadcast(intent);
    }

    protected abstract void loadUrl(final String url);
    protected abstract void evaluateJavascript(final String js);
    protected abstract void clearCache();
    protected abstract void reload();
}
