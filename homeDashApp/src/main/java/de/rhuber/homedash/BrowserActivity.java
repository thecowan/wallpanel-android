package de.rhuber.homedash;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

abstract class BrowserActivity extends AppCompatActivity  {
    public static final String BROADCAST_ACTION_LOAD_URL = "BROADCAST_ACTION_LOAD_URL";
    public static final String BROADCAST_ACTION_SCREEN_ON = "BROADCAST_ACTION_SCREEN_ON";
    public static final String BROADCAST_ACTION_JS_EXEC = "BROADCAST_ACTION_JS_EXEC";
    public static final String BROADCAST_ACTION_CLEAR_BROWSER_CACHE = "BROADCAST_ACTION_CLEAR_BROWSER_CACHE";
    public static final String BROADCAST_ACTION_RELOAD_PAGE = "BROADCAST_ACTION_RELOAD_PAGE";

    final String TAG = BrowserActivity.class.getName();
    private View decorView;

    boolean displayProgress = true;
    private boolean preventSleep = false;
    private boolean keepWiFiOn = false;

    private PowerManager.WakeLock fullWakeLock;
    private PowerManager.WakeLock partialWakeLock;
    private WifiLock wifiLock;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        displayProgress = sharedPreferences.getBoolean(getString(R.string.key_setting_display_progress_enable),true);
        preventSleep = sharedPreferences.getBoolean(getString(R.string.key_setting_prevent_sleep), false);
        keepWiFiOn = sharedPreferences.getBoolean(getString(R.string.key_setting_keep_wifi_on),false);

        // prepare the lock types we may use
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        //noinspection deprecation
        fullWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                PowerManager.ON_AFTER_RELEASE |
                PowerManager.ACQUIRE_CAUSES_WAKEUP, "fullWakeLock");
        partialWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "partialWakeLock");
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "wifiLock");

        // if we are preventing sleep, then we will grab that lock immediately
        if (preventSleep) fullWakeLock.acquire();

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
        filter.addAction(BROADCAST_ACTION_RELOAD_PAGE);
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(this);
        bm.registerReceiver(mBroadcastReceiver, filter);

        String url = sharedPreferences.getString(getString(R.string.key_setting_startup_url),"");
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
            if (intent.getAction().equals(BROADCAST_ACTION_SCREEN_ON)) {
                Log.i(TAG, "Turning screen on");
                screenOn();
            }
            if (intent.getAction().equals(BROADCAST_ACTION_RELOAD_PAGE)) {
                Log.i(TAG, "Browser page reloading.");
                reload();
            }
        }
    };

    private void screenOn(){
        // redundant if the screen is already being kept on
        if (!fullWakeLock.isHeld()) {
            fullWakeLock.acquire();
            fullWakeLock.release();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Browser is no longer in foreground so

        // a. disable keep screen alive
        if(fullWakeLock.isHeld()) fullWakeLock.release();
        // b. acquire the partial lock mode
        partialWakeLock.acquire();
        // c. keep wifi on
        if (keepWiFiOn) wifiLock.acquire();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Browser is back! so...

        // a. enable keep screen alive
        if(!fullWakeLock.isHeld() && preventSleep) fullWakeLock.acquire();
        // b. release the partial lock mode
        if(partialWakeLock.isHeld()) partialWakeLock.release();
        // c. release the wifi lock
        if(wifiLock.isHeld()) wifiLock.release();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            Log.i(TAG, "Back button pressed");
            finish();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    protected abstract void loadUrl(final String url);
    protected abstract void evaluateJavascript(final String js);
    protected abstract void clearCache();
    protected abstract void reload();
}
