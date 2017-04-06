package de.rhuber.homedash;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.util.Log;

public class WelcomeActivity extends AppCompatActivity {

    private final String TAG = BrowserActivity.class.getName();
    private SharedPreferences sharedPreferences;
    private static boolean startup = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_welcome);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean startBrowser = sharedPreferences.getBoolean(getString(R.string.key_setting_direct_browser_enable),false);

        if (startBrowser && startup) {
            Log.i(TAG, "Starting Browser on Startup");
            startBrowserActivity();
        }

        startup = false;
    }

    private void startBrowserActivity() {
        Log.d(TAG, "startBrowserActivity Called");
        String browserType = sharedPreferences.getString(getString(R.string.key_setting_browser_type),getString(R.string.default_setting_browser_type));
        Class targetClass;
        switch (browserType) {
            case "Native":
                Log.i(TAG, "Explicitly using native browser");
                targetClass = BrowserActivityNative.class;
                break;
            case "Legacy":
                Log.i(TAG, "Explicitly using legacy browser");
                targetClass = BrowserActivityLegacy.class;
                break;
            case "Auto":
            default:
                Log.i(TAG, "Auto-selecting dashboard browser");
                targetClass = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) ?
                        BrowserActivityNative.class
                        :
                        BrowserActivityLegacy.class;
                break;
        }
        startActivity(new Intent(getApplicationContext(), targetClass));
    }

    private void startSettingsActivity() {
        Log.d(TAG, "startSettingsActivity Called");
        startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
    }

    private void startMotionActivity() {
        Log.d(TAG, "startMotionActivity Called");
        startActivity(new Intent(getApplicationContext(), MotionActivity.class));
    }

    @SuppressWarnings("UnusedParameters")
    public void buttonBrowserClick(View view) {
        Log.d(TAG, "buttonBrowserClick Called");
        startBrowserActivity();
    }

    @SuppressWarnings("UnusedParameters")
    public void buttonSettingsClick(View view) {
        Log.d(TAG, "buttonSettingsClick Called");
        startSettingsActivity();
    }

    @SuppressWarnings("UnusedParameters")
    public void buttonMotionClick(View view) {
        Log.d(TAG, "buttonMotionClick Called");
        startMotionActivity();
    }

}
