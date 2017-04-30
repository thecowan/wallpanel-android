package org.wallpanelproject.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.wallpanelproject.android.R;

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

    public void startListeningForConfigChanges(SharedPreferences.OnSharedPreferenceChangeListener prefsChangedListener) {
        this.prefsChangedListener = prefsChangedListener;
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

    public boolean getCameraEnabled() { //TODO
        return true;
    }

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

    public boolean getCameraFaceEnabled() { //TODO
        return true;
    }

    public boolean getCameraFaceWake() { //TODO
        return true;
    }

    // HTTP

    public boolean getHttpEnabled() { //TODO
        return getHttpRestEnabled() || getHttpMJPEGEnabled();
    }

    public int getHttpPort() {
        return Integer.valueOf(getStringPref(R.string.key_setting_http_port,
                R.string.default_setting_http_port));
    }

    public boolean getHttpRestEnabled() { //TODO
        return getBoolPref(R.string.key_setting_http_enabled,
                R.string.default_setting_http_enabled);
    }

    public boolean getHttpMJPEGEnabled() { //TODO
        return true;
    } // TODO

    public int getHttpMJPEGMaxStreams() { //TODO
        return 1;
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

    // TEST
    public float getTestZoomLevel() {
        return Float.valueOf(getStringPref(R.string.key_setting_test_zoomlevel,
                R.string.default_setting_test_zoomlevel));
    }
    
}
