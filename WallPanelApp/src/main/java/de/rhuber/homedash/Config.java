package de.rhuber.homedash;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

class Config {

    private final String TAG = this.getClass().getName();

    private final Context myContext;
    private final SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener prefsChangedListener;

    public Config(Context appContext)
    {
        myContext = appContext;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
    }

    public void startListeningForConfigChanges() {
        prefsChangedListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                if (s.contains("_mqtt_")) {
                    Log.i(TAG, "A MQTT Setting Changed");
                    WallPanelService.getInstance().configureMqtt();
                } else if (s.contains("_camera_")) {
                    Log.i(TAG, "A Camera Setting Changed");
                    if (s.equals(myContext.getString(R.string.key_setting_camera_motionenabled)))
                        WallPanelService.getInstance().configureMotionDetection(true);
                    else
                        WallPanelService.getInstance().configureMotionDetection(false);
                } else if (s.equals(myContext.getString(R.string.key_setting_app_preventsleep))) {
                    Log.i(TAG, "A Power Option Changed");
                    WallPanelService.getInstance().configurePowerOptions();
                } else if (s.contains("_http_")) {
                    Log.i(TAG, "A HTTP Option Changed");
                    WallPanelService.getInstance().configureRest();
                }
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(prefsChangedListener);
    }

    public void stopListeningForConfigChanges() {
        sharedPreferences.registerOnSharedPreferenceChangeListener(prefsChangedListener);
    }

    private String getStringPref(int resId, int defId) {
        final String def = myContext.getString(defId);
        final String pref = sharedPreferences.getString(myContext.getString(resId), "");
        return pref.length() == 0 ? def : pref;
    }

    private boolean getBoolPref(int resId, int defId) {
        return sharedPreferences.getBoolean(
                myContext.getString(resId),
                Boolean.valueOf(myContext.getString(defId))
        );
    }

    // APP

    public String getAppDeviceId() {
        return getStringPref(R.string.key_setting_app_deviceid,
                R.string.default_setting_app_deviceid);
    }

    public boolean getAppPreventSleep() {
        return getBoolPref(R.string.key_setting_app_preventsleep,
                R.string.default_setting_app_preventsleep);
    }

    public String getAppLaunchUrl() {
        return getStringPref(R.string.key_setting_app_launchurl,
                R.string.default_setting_app_launchurl);
    }

    public void setAppLaunchUrl(String launchUrl) {
        SharedPreferences.Editor ed = sharedPreferences.edit();
        ed.putString(myContext.getString(R.string.key_setting_app_launchurl), launchUrl);
        ed.apply();
    }

    public boolean getAppShowActivity() {
        return getBoolPref(R.string.key_setting_app_showactivity,
                R.string.default_setting_app_showactivity);
    }

    // CAMERA

    public int getCameraCameraId() {
        return Integer.valueOf(getStringPref(R.string.key_setting_camera_cameraid,
                R.string.default_setting_camera_cameraid));
    }

    public boolean getCameraMotionEnabled() {
        return getBoolPref(R.string.key_setting_camera_motionenabled,
                R.string.default_setting_camera_motionenabled);
    }

    public long getCameraMotionCheckInterval() {
        return Long.valueOf(getStringPref(R.string.key_setting_camera_motioncheckinterval,
                R.string.default_setting_camera_motioncheckinterval));
    }

    public int getCameraMotionLeniency() {
        return Integer.valueOf(getStringPref(R.string.key_setting_camera_motionleniency,
                R.string.default_setting_camera_motionleniency));
    }

    public int getCameraMotionMinLuma() {
        return Integer.valueOf(getStringPref(R.string.key_setting_camera_motionminluma,
                R.string.default_setting_camera_motionminluma));
    }

    public boolean getCameraMotionWake() {
        return getBoolPref(R.string.key_setting_camera_motionwake,
                R.string.default_setting_camera_motionwake);
    }

    public boolean getCameraWebcamEnabled() { //TODO
        return false;
    }

    // HTTP

    public boolean getHttpEnabled() {
        return getBoolPref(R.string.key_setting_http_enabled,
                R.string.default_setting_http_enabled);
    }

    public int getHttpPort() {
        return Integer.valueOf(getStringPref(R.string.key_setting_http_port,
                R.string.default_setting_http_port));
    }

    // MQTT

    public boolean getMqttEnabled() {
        return getBoolPref(R.string.key_setting_mqtt_enabled,
                R.string.default_setting_mqtt_enabled);
    }

    @SuppressLint("DefaultLocale")
    public String getMqttUrl() {
        return String.format("tcp://%s:%d", getMqttServerName(), getMqttServerPort());
    }

    public String getMqttServerName() {
        return getStringPref(R.string.key_setting_mqtt_servername,
                R.string.default_setting_mqtt_servername);
    }

    public int getMqttServerPort() {
        return Integer.valueOf(getStringPref(R.string.key_setting_mqtt_serverport,
                R.string.default_setting_mqtt_serverport));
    }

    public String getMqttBaseTopic() {
        return getStringPref(R.string.key_setting_mqtt_basetopic,
                R.string.default_setting_mqtt_basetopic);
    }

    public String getMqttClientId() {
        return getStringPref(R.string.key_setting_mqtt_clientid,
                R.string.default_setting_mqtt_clientid);
    }

    public String getMqttUsername() {
        return getStringPref(R.string.key_setting_mqtt_username,
                R.string.default_setting_mqtt_username);
    }

    public String getMqttPassword() {
        return getStringPref(R.string.key_setting_mqtt_password,
                R.string.default_setting_mqtt_password);
    }

    public int getMqttSensorFrequency() {
        return Integer.valueOf(getStringPref(R.string.key_setting_mqtt_sensorfrequency,
                R.string.default_setting_mqtt_sensorfrequency));
    }

    // ANDROID SPECIFIC

    public boolean getAndroidStartOnBoot() {
        return getBoolPref(R.string.key_setting_android_startonboot,
                R.string.default_setting_android_startonboot);
    }

    public String getAndroidBrowserType() {
        return getStringPref(R.string.key_setting_android_browsertype,
                R.string.default_setting_android_browsertype);
    }
}
