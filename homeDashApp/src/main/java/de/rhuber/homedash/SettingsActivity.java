package de.rhuber.homedash;


import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArrayMap;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.Manifest.permission;


import java.util.List;

public class SettingsActivity extends AppCompatPreferenceActivity {

    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 1;

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if(preference instanceof SwitchPreference){
                //((SwitchPreference) preference).setChecked((boolean)value);
                //preference.setSummary(value.toString());
                return true;
            }

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.

                preference.setSummary(stringValue);
            }
            return true;
        }
    };
    SharedPreferences sharedPreferences = null;
    ArrayMap<String,SharedPreferences.OnSharedPreferenceChangeListener> onSharedPreferenceChangeListeners = new ArrayMap<>();
    HomeDashService homeDashService;
    boolean mBound = false;

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            HomeDashService.MqttServiceBinder binder = (HomeDashService.MqttServiceBinder) service;
            homeDashService = binder.getService();
            mBound = true;

            bindBoolPreferenceToHomeDashService(R.string.key_setting_enable_mqtt, new BoolPreferenceAction() {
                @Override
                public void action(Boolean newValue) {
                    if (newValue == true) {
                        final String topic = sharedPreferences.getString(getString(R.string.key_setting_mqtt_topic), "");
                        final String url = sharedPreferences.getString(getString(R.string.key_setting_mqtt_host), "");
                        final String clientId = "homeDash-" + Build.DEVICE;
                        final String username = sharedPreferences.getString(getString(R.string.key_setting_mqtt_username), "");
                        final String password = sharedPreferences.getString(getString(R.string.key_setting_mqtt_password), "");
                        homeDashService.startMqttConnection(url, clientId, topic, username, password);
                    } else {
                        homeDashService.stopMqttConnection();
                    }
                }
            });

            bindBoolPreferenceToHomeDashService(R.string.key_setting_motion_detection_enable, new BoolPreferenceAction() {
                @Override
                public void action(Boolean newValue) {
                    if(newValue == true ){
                        homeDashService.startMotionDetection();
                    } else {
                        homeDashService.stopMotionDetection();
                    }
                }
            });


        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        if (preference instanceof SwitchPreference) {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getBoolean(preference.getKey(), false));
        } else {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
        }

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        Boolean startBrowser = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(getString(R.string.key_setting_direct_browser_enable),false);
        if(startBrowser){
            startActivity(new Intent(getApplicationContext(), BrowserActivity.class));
        }

    }



    private interface StringPreferenceAction{
        void action(String newValue);
    }

    private interface BoolPreferenceAction{
        void action(Boolean newValue);
    }


    private void bindBoolPreferenceToHomeDashService(final int preferenceId, final BoolPreferenceAction boolPreferenceAction) {
        final String preferenceKey = getString(preferenceId);
        if(!onSharedPreferenceChangeListeners.containsKey(preferenceKey)) {
            SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                    if (s.equals(preferenceKey)) {
                        boolean newValue = sharedPreferences.getBoolean(s, false);
                        if (mBound) {
                            boolPreferenceAction.action(newValue);
                        }
                    }
                    ;
                }
            };
            onSharedPreferenceChangeListeners.put(preferenceKey, onSharedPreferenceChangeListener);
            sharedPreferences.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
            onSharedPreferenceChangeListener.onSharedPreferenceChanged(sharedPreferences, getString(preferenceId));
        }
    }

    private void bindStringPreferenceToHomeDashService(final int preferenceId, final StringPreferenceAction stringPreferenceAction) {
        final String preferenceKey = getString(preferenceId);
        if(!onSharedPreferenceChangeListeners.containsKey(preferenceKey)) {
            SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                    if (s.equals(preferenceKey)) {

                        String newValue = sharedPreferences.getString(s, "");
                        if (mBound) {
                            stringPreferenceAction.action(newValue);
                        }
                    }
                    ;
                }
            };
            //sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            onSharedPreferenceChangeListeners.put(preferenceKey,onSharedPreferenceChangeListener);
            sharedPreferences.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
            onSharedPreferenceChangeListener.onSharedPreferenceChanged(sharedPreferences, getString(preferenceId));
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(SettingsActivity.this, HomeDashService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.actionbar, menu);
        return true;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || SensorPreferenceFragment.class.getName().equals(fragmentName);
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestAppPermissions();
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_mqtt_host)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_direct_browser_enable)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_mqtt_topic)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_mqtt_username)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_enable_mqtt)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_startup_url)));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_start_browser){
            startActivity(new Intent(getApplicationContext(), BrowserActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SensorPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_sensors);
            setHasOptionsMenu(true);


            Preference preference = findPreference(getString(R.string.key_setting_sensor_update_frequency));
            Preference.OnPreferenceChangeListener preferenceChangeListener = new Preference.OnPreferenceChangeListener(){
                    @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String summary = (String)newValue+" seconds";
                    preference.setSummary(summary);
                    return false;
                }
            };
            preference.setOnPreferenceChangeListener(preferenceChangeListener);
            preferenceChangeListener.onPreferenceChange(preference,
                    PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.setting_sensor_pressure_enable)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.setting_sensor_battery_enable)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.setting_sensor_light_enable)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_motion_detection_enable)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_motion_detection_intervall)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_motion_detection_leniency)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_motion_detection_min_luma)));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    public void requestAppPermissions(){
        if(PackageManager.PERMISSION_DENIED == ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)){
            ActivityCompat.requestPermissions(this,
                    new String[]{permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Snackbar snackbar = Snackbar.make(getListView(), "Camera permission granted.", Snackbar.LENGTH_LONG);
                    snackbar.show();
                } else {
                    Snackbar snackbar = Snackbar.make(getListView(), "Camera permission not granted. Motion detection disabled.", Snackbar.LENGTH_LONG);
                    snackbar.show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}
