/*
 * Copyright (c) 2019 ThanksMister LLC
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

    var cameraPermissionsShown: Boolean
        get() = sharedPreferences.getBoolean(PREF_CAMERA_PERMISSIONS, false)
        set(value) {
            sharedPreferences.edit().putBoolean(PREF_CAMERA_PERMISSIONS, value).apply()
        }

    var cameraRotate: Float
        get() = this.sharedPreferences.getString(PREF_CAMERA_ROTATE, "0f").toFloat()
        set(value) = this.sharedPreferences.edit().putString(PREF_CAMERA_ROTATE, value.toString()).apply()

    var appLaunchUrl: String
        get() = getStringPref(R.string.key_setting_app_launchurl,
                R.string.default_setting_app_launchurl)
        set(launchUrl) {
            sharedPreferences.edit().putString(context.getString(R.string.key_setting_app_launchurl), launchUrl).apply()
        }

    val appShowActivity: Boolean
        get() = getBoolPref(R.string.key_setting_app_showactivity,
                R.string.default_setting_app_showactivity)

    var useGeckoBrowser: Boolean
        get() = getBoolPref(R.string.key_setting_use_gecko, R.string.default_gecko_enabled)
        set(value) {
            sharedPreferences.edit().putBoolean(context.getString(R.string.key_setting_use_gecko), value).apply()
        }

    var cameraEnabled: Boolean
        get() = getBoolPref(R.string.key_setting_camera_enabled, R.string.default_setting_camera_enabled)
        set(value) {
            sharedPreferences.edit().putBoolean(context.getString(R.string.key_setting_camera_enabled), value).apply()
        }

    var cameraId: Int
        get() = this.sharedPreferences.getInt(context.getString(R.string.setting_camera_cameraid),0)
        set(value) {
            this.sharedPreferences.edit().putInt(context.getString(R.string.setting_camera_cameraid), value).apply()
        }

    var cameraMotionEnabled: Boolean
        get() = getBoolPref(R.string.key_setting_camera_motionenabled, R.string.default_setting_camera_motionenabled)
        set(value) {
            sharedPreferences.edit().putBoolean(context.getString(R.string.key_setting_camera_motionenabled), value).apply()
        }

    val cameraMotionLeniency: Int
        get() = getStringPref(R.string.key_setting_camera_motionleniency,
                R.string.default_setting_camera_motionleniency).trim().toInt()

    val cameraMotionMinLuma: Int
        get() = Integer.valueOf(getStringPref(R.string.key_setting_camera_motionminluma, R.string.default_setting_camera_motionminluma).trim().toInt())

    val cameraMotionOnTime: Int
        get() = getStringPref(R.string.key_setting_camera_motionontime,
                R.string.default_setting_camera_motionontime).trim().toInt()

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
        get() = getStringPref(R.string.key_setting_motion_clear,
                R.string.default_motion_clear).trim().toInt()

    val httpEnabled: Boolean
        get() = httpRestEnabled || httpMJPEGEnabled

    val httpPort: Int
        get() =
            try {
                getStringPref(R.string.key_setting_http_port, R.string.default_setting_http_port).trim().toInt()
            } catch (e: NumberFormatException) {
                context.getString(R.string.default_setting_http_port).toInt()
            }

    val httpRestEnabled: Boolean
        get() = getBoolPref(R.string.key_setting_http_restenabled,
                R.string.default_setting_http_restenabled)

    val httpMJPEGEnabled: Boolean
        get() = getBoolPref(R.string.key_setting_http_mjpegenabled,
                R.string.default_setting_http_mjpegenabled)

    fun setHttpMJPEGEnabled(value: Boolean?) {
        sharedPreferences.edit().putBoolean(context.getString(R.string.key_setting_http_mjpegenabled), value!!).apply()
    }

    val httpMJPEGMaxStreams: Int
        get() = getStringPref(R.string.key_setting_http_mjpegmaxstreams, R.string.default_setting_http_mjpegmaxstreams).trim().toInt()

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
        get() = getStringPref(R.string.key_setting_mqtt_serverport, R.string.default_setting_mqtt_serverport).trim().toInt()

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
        get() = getStringPref(R.string.key_setting_mqtt_sensorfrequency,
                R.string.default_setting_mqtt_sensorfrequency).trim().toInt()

    val mqttDiscovery: Boolean
        get() = getBoolPref(R.string.key_setting_mqtt_home_assistant_discovery,  R.string.default_setting_mqtt_home_assistant_discovery)

    val mqttDiscoveryTopic: String
        get() = getStringPref(R.string.key_setting_mqtt_home_assistant_topic, R.string.default_setting_mqtt_home_assistant_topic)

    val mqttDiscoveryDeviceName: String
        get() = getStringPref(R.string.key_setting_mqtt_home_assistant_name, R.string.default_setting_mqtt_home_assistant_name)

    val androidStartOnBoot: Boolean
        get() = getBoolPref(R.string.key_setting_android_startonboot,
                R.string.default_setting_android_startonboot)

    val ttsEnabled: Boolean
        get() = getBoolPref(R.string.key_pref_tts_notification,
                R.string.default_tts_notification)

    val sensorsEnabled: Boolean
        get() = getBoolPref(R.string.key_setting_sensors_enabled,
                R.string.default_setting_sensors_value)

    val hardwareAccelerated: Boolean
        get() = getBoolPref(R.string.key_hadware_accelerated_enabled,
                R.string.default_hardware_accelerated_value)

    val browserUserAgent: String
        get() = getStringPref(R.string.key_setting_browser_user_agent,
                R.string.default_browser_user_agent)

    var browserRefresh: Boolean
        get() = getBoolPref(R.string.key_pref_browser_refresh, R.string.default_browser_refresh)
        set(value) {
            sharedPreferences.edit().putBoolean(context.getString(R.string.key_pref_browser_refresh), value).apply()
        }

    val cameraFPS: Float
        get() = try {
            getStringPref(R.string.key_setting_camera_fps, R.string.default_camera_fps).trim().toFloat()
        } catch (e: Exception) {
            15.0F
        }

    val testZoomLevel: Float
        get() = getStringPref(R.string.key_setting_test_zoomlevel, R.string.default_setting_test_zoomlevel).trim().toFloat()

    var inactivityTime: Long
        get() = sharedPreferences.getLong(context.getString(R.string.key_screensaver_inactivity_time), 30000)
        set(value) {
            sharedPreferences.edit().putLong(context.getString(R.string.key_screensaver_inactivity_time), value).apply()
        }

    var screenSaverDimValue: Int
        get() = sharedPreferences.getInt(context.getString(R.string.key_screensaver_dim_value), 25)
        set(value) {
            sharedPreferences.edit().putInt(context.getString(R.string.key_screensaver_dim_value), value).apply()
        }

    var imageRotation: Int
        get() = sharedPreferences.getInt(PREF_IMAGE_ROTATION, ROTATE_TIME_IN_MINUTES)
        set(value) = this.sharedPreferences.edit().putInt(PREF_IMAGE_ROTATION, value).apply()

    val hasClockScreenSaver: Boolean
        get() = getBoolPref(R.string.key_screensaver, R.string.default_screensaver)

    val hasScreenSaverWallpaper: Boolean
        get() = getBoolPref(R.string.key_screensaver_wallpaper, R.string.default_screensaver_wallpaper)

    var screenBrightness: Int
        get() = sharedPreferences.getInt(context.getString(R.string.key_setting_screen_brightness), 150)
        set(value) {
            sharedPreferences.edit().putInt(context.getString(R.string.key_setting_screen_brightness), value).apply()
        }

    var screenScreenSaverBrightness: Int
        get() = sharedPreferences.getInt(context.getString(R.string.key_setting_screensaver_brightness), 0)
        set(value) {
            sharedPreferences.edit().putInt(context.getString(R.string.key_setting_screensaver_brightness), value).apply()
        }

    var useScreenBrightness: Boolean
        get() = this.sharedPreferences.getBoolean(PREF_SCREEN_BRIGHTNESS, false)
        set(value) = this.sharedPreferences.edit().putBoolean(PREF_SCREEN_BRIGHTNESS, value).apply()

    val ignoreSSLErrors: Boolean
        get() = getBoolPref(R.string.key_setting_ignore_ssl_errors,
                R.string.default_setting_ignore_ssl_errors)

    fun hasCameraDetections() : Boolean {
        return cameraEnabled && (cameraMotionEnabled || cameraQRCodeEnabled || cameraFaceEnabled || httpMJPEGEnabled)
    }

    private fun getStringPref(resId: Int, defId: Int): String {
        val def = context.getString(defId)
        val pref = sharedPreferences.getString(context.getString(resId), "")
        return if (pref!!.isEmpty()) def else pref
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
        const val PREF_CAMERA_PERMISSIONS = "pref_camera_permissions"
        const val PREF_CAMERA_ROTATE = "pref_camera_rotate"
        const val PREF_SCREEN_BRIGHTNESS = "pref_use_screen_brightness"
        const val PREF_SCREEN_INACTIVITY_TIME = "pref_screensaver_inactivity_time"
        const val PREF_SCREENSAVER_DIM_VALUE = "pref_screensaver_dim_value"
        private val ROTATE_TIME_IN_MINUTES = 15
        const val PREF_IMAGE_ROTATION = "pref_image_rotation"
    }
}
