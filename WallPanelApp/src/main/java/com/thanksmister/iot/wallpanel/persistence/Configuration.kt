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

package com.thanksmister.iot.wallpanel.persistence

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import com.thanksmister.iot.wallpanel.R
import javax.inject.Inject

class Configuration @Inject
constructor(private val context: Context, private val sharedPreferences: SharedPreferences) {

    // APP
    val isFirstTime: Boolean
        get() = sharedPreferences.getBoolean(PREF_FIRST_TIME, true)

    val appPreventSleep: Boolean
        get() = getBoolPref(R.string.key_setting_app_preventsleep,
                R.string.default_setting_app_preventsleep)

    var writeScreenPermissionsShown: Boolean
        get() = sharedPreferences.getBoolean(PREF_WRITE_SCREEN_PERMISSIONS, false)
        set(value) {
            sharedPreferences.edit().putBoolean(PREF_WRITE_SCREEN_PERMISSIONS, value).apply()
        }

    /*var adjustBackBehavior: Boolean
        get() = this.sharedPreferences.getBoolean("PREF_PLATFORM_BACK_BEHAVIOR", true)
        set(value) = this.sharedPreferences.edit().putBoolean("PREF_PLATFORM_BACK_BEHAVIOR", value).apply()

    var hideAdminMenu: Boolean
        get() = this.sharedPreferences.getBoolean("PREF_PLATFORM_ADMIN_MENU", true)
        set(value) = this.sharedPreferences.edit().putBoolean("PREF_PLATFORM_ADMIN_MENU", value).apply()
*/
    var appLaunchUrl: String
        get() = getStringPref(R.string.key_setting_app_launchurl,
                R.string.default_setting_app_launchurl)
        set(launchUrl) {
            sharedPreferences.edit().putString(context.getString(R.string.key_setting_app_launchurl), launchUrl).apply()
        }

    val appShowActivity: Boolean
        get() = getBoolPref(R.string.key_setting_app_showactivity,
                R.string.default_setting_app_showactivity)

    var cameraEnabled: Boolean
        get() = getBoolPref(R.string.key_setting_camera_enabled, R.string.default_setting_camera_enabled)
        set(value) {
            sharedPreferences.edit().putBoolean(context.getString(R.string.key_setting_camera_enabled), value).apply()
        }

    var hasTextToSpeech: Boolean
        get() = getBoolPref(R.string.key_setting_tts_enabled, R.string.default_setting_tss_enabled)
        set(value) {
            sharedPreferences.edit().putBoolean(context.getString(R.string.key_setting_tts_enabled), value).apply()
        }

    val cameraId: Int
        get() = Integer.valueOf(getStringPref(R.string.key_setting_camera_cameraid,
                R.string.default_setting_camera_cameraid))

    var cameraMotionEnabled: Boolean
        get() = getBoolPref(R.string.key_setting_camera_motionenabled, R.string.default_setting_camera_motionenabled)
        set(value) {
            sharedPreferences.edit().putBoolean(context.getString(R.string.key_setting_camera_motionenabled), value).apply()
        }

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

    var cameraFaceEnabled: Boolean
        get() = getBoolPref(R.string.key_setting_camera_faceenabled,
                R.string.default_setting_camera_faceenabled)
        set(value) {
            sharedPreferences.edit().putBoolean(context.getString(R.string.key_setting_camera_faceenabled), value).apply()
        }

    val cameraFaceWake: Boolean
        get() = getBoolPref(R.string.key_setting_camera_facewake,
                R.string.default_setting_camera_facewake)

    var cameraQRCodeEnabled: Boolean
        get() = getBoolPref(R.string.key_setting_camera_qrcodeenabled,
                R.string.default_setting_camera_qrcodeenabled)
        set(value) {
            sharedPreferences.edit().putBoolean(context.getString(R.string.key_setting_camera_qrcodeenabled), value).apply()
        }

    val motionResetTime: Int
        get() = Integer.valueOf(getStringPref(R.string.key_setting_motion_clear,
                R.string.default_motion_clear))

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

    val mqttTlsEnabled: Boolean
        get() = getBoolPref(R.string.key_setting_mqtt_tls_enabled, R.string.default_setting_mqtt_tts_enabled)

    val mqttUrl: String
        @SuppressLint("DefaultLocale")
        get() = String.format("tcp://%s:%d", mqttBroker, mqttServerPort)

    val mqttBroker: String
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

    val ttsEnabled: Boolean
        get() = getBoolPref(R.string.key_pref_tts_notification,
                R.string.default_tts_notification)

    val sensorsEnabled: Boolean
        get() = getBoolPref(R.string.key_setting_sensors_enabled,
                R.string.default_setting_sensors_value)

    val androidBrowserType: String
        get() = getStringPref(R.string.key_setting_android_browsertype,
                R.string.default_setting_android_browsertype)

    val browserUserAgent: String
        get() = getStringPref(R.string.key_setting_browser_user_agent,
                R.string.default_browser_user_agent)

    val cameraFPS: Float
        get() = try {
            getStringPref(R.string.key_setting_camera_fps, R.string.default_camera_fps).toFloat()
        } catch (e: Exception) {
            15.0F
        }

    val testZoomLevel: Float
        get() = java.lang.Float.valueOf(getStringPref(R.string.key_setting_test_zoomlevel,
                R.string.default_setting_test_zoomlevel))!!

    private fun getStringPref(resId: Int, defId: Int): String {
        val def = context.getString(defId)
        val pref = sharedPreferences.getString(context.getString(resId), "")
        return if (pref!!.length == 0) def else pref
    }

    private fun getBoolPref(resId: Int, defId: Int): Boolean {
        return sharedPreferences.getBoolean(
                context.getString(resId),
                java.lang.Boolean.valueOf(context.getString(defId))
        )
    }

    fun setFirstTime(value: Boolean?) {
        sharedPreferences.edit().putBoolean(PREF_FIRST_TIME, value!!).apply()
    }

    companion object {
        const val PREF_FIRST_TIME = "pref_first_time"
        const val PREF_WRITE_SCREEN_PERMISSIONS = "pref_write_screen_permissions"
        const val PREF_BROWSER_LEGACY = "Legacy"
        const val PREF_BROWSER_NATIVE = "Native"
        const val PREF_BROWSER_AUTO = "Auto"
    }
}
