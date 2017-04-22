package de.rhuber.homedash;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;

public class WelcomeActivity extends AppCompatActivity {

    private final String TAG = BrowserActivity.class.getName();
    private static boolean startup = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_welcome);
        getFragmentManager().beginTransaction().add(android.R.id.content,
                new SettingsActivity.GeneralPreferenceFragment()).commit();

        if (startup) {
            Log.i(TAG, "Starting Browser on Startup");
            startBrowserActivity();
        }

        startup = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_welcome, menu);
        return true;
    }

    private void startBrowserActivity() {
        Log.d(TAG, "startBrowserActivity Called");
        Log.i(TAG, String.valueOf(WallPanelService.getInstance()));
        String browserType = (new Config(this.getApplicationContext())).getAndroidBrowserType();
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

    @SuppressWarnings("UnusedParameters")
    public void onLaunchDashboard(MenuItem mi) {
        Log.d(TAG, "onLaunchDashboard Called");
        startBrowserActivity();
    }

}
