package org.wallpanelproject.android;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;

import org.wallpanelproject.android.R;

public class WelcomeActivity extends AppCompatActivity {

    private final static int MY_PERMISSIONS_REQUEST_CAMERA = 23;

    private final String TAG = BrowserActivity.class.getName();
    private static boolean startup = true;
    private static SettingsActivity.GeneralPreferenceFragment prefs = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestCameraPermissions();

        if (startup) {
            if (savedInstanceState == null) {
                prefs = new SettingsActivity.GeneralPreferenceFragment();
                //setContentView(R.layout.activity_welcome);
                getFragmentManager().beginTransaction().add(android.R.id.content,
                        prefs).commit();
            }
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

    private void requestCameraPermissions(){
        if(PackageManager.PERMISSION_DENIED == ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)){
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Snackbar snackbar = Snackbar.make(getWindow().getDecorView().getRootView(),
                            "Camera permission granted.",
                            Snackbar.LENGTH_LONG);
                    snackbar.show();
                    prefs.updateCameraList();
                } else {
                    Snackbar snackbar = Snackbar.make(getWindow().getDecorView().getRootView(),
                            "Camera permission not granted.",
                            Snackbar.LENGTH_LONG);
                    snackbar.show();
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

}
