package de.rhuber.homedash;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.jjoe64.motiondetection.motiondetection.MotionDetector;

import java.util.ArrayList;

public class SettingsActivity extends AppCompatPreferenceActivity {

    private static final Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
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

    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || CameraPreferenceFragment.class.getName().equals(fragmentName);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {

        private final String TAG = this.getClass().getName();

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);

            Preference pref = findPreference("motion_settings");
            pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startMotionActivity(preference.getContext());
                    return false;
                }
            });

            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_app_deviceid)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_app_preventsleep)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_app_launchurl)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_app_showactivity)));

            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_android_startonboot)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_android_browsertype)));

            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_http_enabled)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_http_port)));

            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_mqtt_enabled)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_mqtt_servername)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_mqtt_serverport)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_mqtt_basetopic)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_mqtt_clientid)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_mqtt_username)));
            // password
            Preference preference = findPreference(getString(R.string.key_setting_mqtt_sensorfrequency));
            Preference.OnPreferenceChangeListener preferenceChangeListener = new Preference.OnPreferenceChangeListener(){
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String summary = newValue+" seconds";
                    preference.setSummary(summary);
                    return true;
                }
            };
            preference.setOnPreferenceChangeListener(preferenceChangeListener);
            preferenceChangeListener.onPreferenceChange(preference,
                    PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
        }

        private void startMotionActivity(Context c) {
            Log.d(TAG, "startMotionActivity Called");
            startActivity(new Intent(c, MotionActivity.class));
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class CameraPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_camera);
            setHasOptionsMenu(true);

            ListPreference cameras = (ListPreference) findPreference(getString(R.string.key_setting_camera_cameraid));
            ArrayList<String> cameraList = MotionDetector.getCameras();
            cameras.setEntries(cameraList.toArray(new CharSequence[cameraList.size()]));
            CharSequence[] vals = new CharSequence[cameraList.size()];
            for (int i=0; i<cameraList.size(); i++) { vals[i] = Integer.toString(i); }
            cameras.setEntryValues(vals);

            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_camera_motionenabled)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_camera_motionwake)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_camera_motioncheckinterval)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_camera_motionleniency)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_camera_motionminluma)));
            bindPreferenceSummaryToValue(cameras);
        }
    }

}
