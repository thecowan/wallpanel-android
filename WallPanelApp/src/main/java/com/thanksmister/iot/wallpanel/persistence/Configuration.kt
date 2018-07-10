/*
 * Copyright (c) 2018 ThanksMister LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wallpanelproject.android.persistence

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

import org.wallpanelproject.android.R

class Configuration(private val myContext: Context) {

    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(myContext)
    private var prefsChangedListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    // APP
    val isFirstTime: Boolean
        get() = sharedPreferences.getBoolean(PREF_FIRST_TIME, true)

    val appPreventSleep: Boolean
        get() = getBoolPref(R.string.key_setting_app_preventsleep,
                R.string.default_setting_app_preventsleep)

    var appLaunchUrl: String
        get() = getStringPref(R.string.key_setting_app_launchurl,
                R.string.default_setting_app_launchurl)
        set(launchUrl) {
            val ed = sharedPreferences.edit()
            ed.putString(myContext.getString(R.string.key_setting_app_launchurl), launchUrl)
            ed.apply()
        }

    val appShowActivity: Boolean
        get() = getBoolPref(R.string.key_setting_app_showactivity,
                R.string.default_setting_app_showactivity)

    val cameraEnabled: Boolean
        get() = getBoolPref(R.string.key_setting_camera_enabled,
                R.string.default_setting_camera_enabled)

    val cameraCameraId: Int
        get() = Integer.valueOf(getStringPref(R.string.key_setting_camera_cameraid,
                R.string.default_setting_camera_cameraid))

    val cameraMotionEnabled: Boolean
        get() = getBoolPref(R.string.key_setting_camera_motionenabled, R.string.default_setting_camera_motionenabled)

    val cameraProcessingInterval: Long
        get() = java.lang.Long.valueOf(getStringPref(R.string.key_setting_camera_processinginterval,
                R.string.default_setting_camera_processinginterval))

    val cameraMotionLeniency: Int
        get() = Integer.valueOf(getStringPref(R.string.key_setting_camera_motionleniency,
                R.string.default_setting_camera_motionleniency))

    val cameraMotionMinLuma: Int
        get() = Integer.valueOf(getStringPref(R.string.key_setting_camera_motionminluma,
                R.string.default_setting_camera_motionminluma))

    val cameraMotionOnTime: Int
        get() = Integer.valueOf(getStringPref(R.string.key_setting_camera_motionontime,
                R.string.default_setting_camera_motionontime))

    val cameraMotionWake: Boolean
        get() = getBoolPref(R.string.key_setting_camera_motionwake,
                R.string.default_setting_camera_motionwake)

    val cameraMotionBright: Boolean
        get() = getBoolPref(R.string.key_setting_camera_motionbright,
                R.string.default_setting_camera_motionbright)

    val cameraFaceEnabled: Boolean
        get() = getBoolPref(R.string.key_setting_camera_faceenabled,
                R.string.default_setting_camera_faceenabled)

    val cameraFaceWake: Boolean
        get() = getBoolPref(R.string.key_setting_camera_facewake,
                R.string.default_setting_camera_facewake)

    val cameraQRCodeEnabled: Boolean
        get() = getBoolPref(R.string.key_setting_camera_qrcodeenabled,
                R.string.default_setting_camera_qrcodeenabled)

    val httpEnabled: Boolean
        get() = httpRestEnabled || httpMJPEGEnabled

    val httpPort: Int
        get() = Integer.valueOf(getStringPref(R.string.key_setting_http_port,
                R.string.default_setting_http_port))

    val httpRestEnabled: Boolean
        get() = getBoolPref(R.string.key_setting_http_restenabled,
                R.string.default_setting_http_restenabled)

    val httpMJPEGEnabled: Boolean
        get() = getBoolPref(R.string.key_setting_http_mjpegenabled,
                R.string.default_setting_http_mjpegenabled)

    val httpMJPEGMaxStreams: Int
        get() = Integer.valueOf(getStringPref(R.string.key_setting_http_mjpegmaxstreams, R.string.default_setting_http_mjpegmaxstreams))

    val mqttEnabled: Boolean
        get() = getBoolPref(R.string.key_setting_mqtt_enabled, R.string.default_setting_mqtt_enabled)

    val mqttUrl: String
        @SuppressLint("DefaultLocale")
        get() = String.format("tcp://%s:%d", mqttServerName, mqttServerPort)

    val mqttServerName: String
        get() = getStringPref(R.string.key_setting_mqtt_servername, R.string.default_setting_mqtt_servername)

    val mqttServerPort: Int
        get() = Integer.valueOf(getStringPref(R.string.key_setting_mqtt_serverport, R.string.default_setting_mqtt_serverport))

    val mqttBaseTopic: String
        get() = getStringPref(R.string.key_setting_mqtt_basetopic,
                R.string.default_setting_mqtt_basetopic)

    val mqttClientId: String
        get() = getStringPref(R.string.key_setting_mqtt_clientid,
                R.string.default_setting_mqtt_clientid)

    val mqttUsername: String
        get() = getStringPref(R.string.key_setting_mqtt_username,
                R.string.default_setting_mqtt_username)

    val mqttPassword: String
        get() = getStringPref(R.string.key_setting_mqtt_password,
                R.string.default_setting_mqtt_password)

    val mqttSensorFrequency: Int
        get() = Integer.valueOf(getStringPref(R.string.key_setting_mqtt_sensorfrequency,
                R.string.default_setting_mqtt_sensorfrequency))

    val androidStartOnBoot: Boolean
        get() = getBoolPref(R.string.key_setting_android_startonboot,
                R.string.default_setting_android_startonboot)

    val androidBrowserType: String
        get() = getStringPref(R.string.key_setting_android_browsertype,
                R.string.default_setting_android_browsertype)

    val testZoomLevel: Float
        get() = java.lang.Float.valueOf(getStringPref(R.string.key_setting_test_zoomlevel,
                R.string.default_setting_test_zoomlevel))!!

    fun startListeningForConfigChanges(prefsChangedListener: SharedPreferences.OnSharedPreferenceChangeListener) {
        this.prefsChangedListener = prefsChangedListener
        sharedPreferences.registerOnSharedPreferenceChangeListener(prefsChangedListener)
    }

    fun stopListeningForConfigChanges() {
        sharedPreferences.registerOnSharedPreferenceChangeListener(prefsChangedListener)
    }

    private fun getStringPref(resId: Int, defId: Int): String {
        val def = myContext.getString(defId)
        val pref = sharedPreferences.getString(myContext.getString(resId), "")
        return if (pref!!.length == 0) def else pref
    }

    private fun getBoolPref(resId: Int, defId: Int): Boolean {
        return sharedPreferences.getBoolean(
                myContext.getString(resId),
                java.lang.Boolean.valueOf(myContext.getString(defId))
        )
    }

    fun setFirstTime(value: Boolean?) {
        sharedPreferences.edit().putBoolean(PREF_FIRST_TIME, value!!).apply()
    }

    companion object {
        const val PREF_FIRST_TIME = "pref_first_time"
    }
}
