package de.rhuber.homedash;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

class Config {

    private final String TAG = this.getClass().getName();

    private Context myContext;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener prefsChangedListener;

    public Config(Context appContext)
    {
        myContext = appContext;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);

        prefsChangedListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                if (s.contains("mqtt") || s.equals(myContext.getString(R.string.key_setting_sensor_update_frequency))) {
                    Log.i(TAG, "A MQTT Setting Changed");
                    WallPanelService.getInstance().configureMqtt();
                } else if (s.contains("motion")) {
                    Log.i(TAG, "A Motion Detection Setting Changed");
                    if (s.equals(myContext.getString(R.string.key_setting_motion_detection_camera)))
                        WallPanelService.getInstance().configureMotionDetection(true);
                    else
                        WallPanelService.getInstance().configureMotionDetection(false);
                } else if (s.equals(myContext.getString(R.string.key_setting_prevent_sleep)) ||
                        s.equals(myContext.getString(R.string.key_setting_keep_wifi_on))) {
                    Log.i(TAG, "A Power Option Changed");
                    WallPanelService.getInstance().configurePowerOptions();
                }
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(prefsChangedListener);
    }

    public boolean getPreventSleep() {
        return sharedPreferences.getBoolean(myContext.getString(R.string.key_setting_prevent_sleep), false);
    }

    public boolean getKeepWiFiOn() {
        return sharedPreferences.getBoolean(myContext.getString(R.string.key_setting_keep_wifi_on), true);
    }

    public boolean getMqttEnabled() {
        return sharedPreferences.getBoolean(myContext.getString(R.string.key_setting_enable_mqtt), false);
    }

    public String getMqttTopic() {
        return sharedPreferences.getString(myContext.getString(R.string.key_setting_mqtt_topic), "");
    }

    public String getMqttUrl() {
        return sharedPreferences.getString(myContext.getString(R.string.key_setting_mqtt_host), "");
    }

    public String getMqttClientId() {
        return "WallPanel-" + Build.DEVICE;
    }

    public String getMqttUsername() {
        return sharedPreferences.getString(myContext.getString(R.string.key_setting_mqtt_username), "");
    }

    public String getMqttPassword() {
        return sharedPreferences.getString(myContext.getString(R.string.key_setting_mqtt_password), "");
    }

    public int getMqttSensorUpdateFrequency() {
        return Integer.parseInt(sharedPreferences.getString(myContext.getString(R.string.key_setting_sensor_update_frequency),"60"));
    }

    public boolean getCameraMotionDetectionEnabled() {
        return sharedPreferences.getBoolean(myContext.getString(R.string.key_setting_motion_detection_enable),false);
    }

    public long getCameraMotionCheckInterval() {
        return Long.valueOf(sharedPreferences.getString(myContext.getString(R.string.key_setting_motion_detection_interval), "500"));
    }

    public int getCameraMotionLeniency() {
        return Integer.valueOf(sharedPreferences.getString(myContext.getString(R.string.key_setting_motion_detection_leniency), "20"));
    }

    public int getCameraMinLuma() {
        return Integer.valueOf(sharedPreferences.getString(myContext.getString(R.string.key_setting_motion_detection_min_luma), "1000"));
    }

    public int getCameraId() {
        return Integer.valueOf(sharedPreferences.getString(myContext.getString(R.string.key_setting_motion_detection_camera),"0"));
    }

    public boolean getStartOnBoot() {
        return sharedPreferences.getBoolean(myContext.getString(R.string.key_setting_start_on_boot), false);
    }

    public boolean getShowProgress() {
        return sharedPreferences.getBoolean(myContext.getString(R.string.key_setting_display_progress_enable),false);
    }

    public String getLaunchUrl() {
        return sharedPreferences.getString(myContext.getString(R.string.key_setting_startup_url),"");
    }

    public void setLaunchUrl(String launchUrl) {
        SharedPreferences.Editor ed = sharedPreferences.edit();
        ed.putString(myContext.getString(R.string.key_setting_startup_url), launchUrl);
        ed.apply();
    }

    public String getBrowserType() {
        return sharedPreferences.getString(myContext.getString(R.string.key_setting_browser_type),
                myContext.getString(R.string.default_setting_browser_type));
    }
}
