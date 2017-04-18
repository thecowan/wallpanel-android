package de.rhuber.homedash;


import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.jjoe64.motiondetection.motiondetection.MotionDetector;

import java.util.ArrayList;

public class SettingsActivity extends AppCompatPreferenceActivity {

    @SuppressWarnings("unused")
    private final String TAG = WallPanelService.class.getName();

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || CameraPreferenceFragment.class.getName().equals(fragmentName);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);

            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_startup_url)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_start_on_boot)));

            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_browser_type)));

            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_display_progress_enable)));

            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_prevent_sleep)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_keep_wifi_on)));

            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_sensor_pressure_enable)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_sensor_battery_enable)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_sensor_light_enable)));

            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_mqtt_host)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_mqtt_topic)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_mqtt_username)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_enable_mqtt)));

            Preference preference = findPreference(getString(R.string.key_setting_sensor_update_frequency));
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
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class CameraPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_camera);
            setHasOptionsMenu(true);

            ListPreference cameras = (ListPreference) findPreference(getString(R.string.key_setting_motion_detection_camera));
            ArrayList<String> cameraList = MotionDetector.getCameras();
            cameras.setEntries(cameraList.toArray(new CharSequence[cameraList.size()]));
            CharSequence[] vals = new CharSequence[cameraList.size()];
            for (int i=0; i<cameraList.size(); i++) { vals[i] = Integer.toString(i); }
            cameras.setEntryValues(vals);

            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_motion_detection_enable)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_motion_detection_interval)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_motion_detection_leniency)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_motion_detection_min_luma)));
            bindPreferenceSummaryToValue(cameras);
        }
    }

}
