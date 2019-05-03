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

package com.thanksmister.iot.wallpanel.utils

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.provider.Settings
import com.thanksmister.iot.wallpanel.persistence.Configuration
import timber.log.Timber

class ScreenUtils(base: Context) : ContextWrapper(base) {

    fun getScreenBrightness(configuration: Configuration): Int{
        Timber.d("getScreenBrightness")
        var brightness = 0
        try {
            brightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            if (brightness in 1..255) {
                configuration.screenBrightness = brightness
                configuration.screenScreenSaverBrightness = (brightness * Configuration.PREF_BRIGHTNESS_FACTOR).toInt()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Timber.d("getScreenBrightness value $brightness")
        return brightness
    }

    fun resetScreenBrightness(screenSaver: Boolean = false, configuration: Configuration, isFinishing: Boolean = false) {
        Timber.d("resetScreenBrightness $isFinishing")
        if(!isFinishing) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.System.canWrite(applicationContext)) {
                var mode = -1
                try {
                    mode = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE) //this will return integer (0 or 1)
                } catch (e: Settings.SettingNotFoundException) {
                    Timber.e(e.message)
                }
                try {
                    if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                        //Automatic mode, need to be in manual to change brightness
                        Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                    }
                    if (configuration.screenBrightness in 1..255 && !screenSaver) {
                        Timber.d("calculated brightness ${configuration.screenBrightness}")
                        Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, configuration.screenBrightness)
                    } else if (configuration.screenScreenSaverBrightness in 1..255 && screenSaver) {
                        Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, configuration.screenScreenSaverBrightness)
                        Timber.d("calculated brightness ${configuration.screenScreenSaverBrightness}")
                    }
                } catch (e: SecurityException) {
                    Timber.e(e.message)
                }
            } else {
                try {
                    Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                    if (configuration.screenBrightness in 1..255 && !screenSaver) {
                        Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, configuration.screenBrightness)
                        Timber.d("calculated brightness ${configuration.screenBrightness}")
                    } else if (configuration.screenScreenSaverBrightness in 1..255 && screenSaver) {
                        Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, configuration.screenScreenSaverBrightness)
                        Timber.d("calculated brightness ${configuration.screenScreenSaverBrightness}")
                    }
                } catch (e: SecurityException) {
                    Timber.e(e.message)
                }
            }
        }
    }

    fun setScreenBrightness(brightness: Int, configuration: Configuration) {
        Timber.d("setScreenBrightness $brightness")
        var mode = -1
        try {
            mode = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE) //this will return integer (0 or 1)
        } catch (e: Settings.SettingNotFoundException) {
            Timber.e(e.message)
        }

        try {
            if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                //Automatic mode, need to be in manual to change brightness
                Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            }
            if (brightness in 1..255) {
                Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness)
                configuration.screenBrightness = brightness
                configuration.screenScreenSaverBrightness = (brightness * Configuration.PREF_BRIGHTNESS_FACTOR).toInt()
            }
        } catch (e: SecurityException) {
            Timber.e(e.message)
        }
    }

    fun configureBrightnessLevels(configuration: Configuration) {
        Timber.d("configureBrightnessLevels")
        var mode = -1
        try {
            mode = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE) //this will return integer (0 or 1)
        } catch (e: Settings.SettingNotFoundException) {
            Timber.e(e.message)
        }
        try {
            if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                //Automatic mode, need to be in manual to change brightness
                Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            }
            val brightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            Timber.d("configureBrightnessLevels brightness $brightness")
            if (brightness in 1..255) {
                configuration.screenBrightness = brightness
                configuration.screenScreenSaverBrightness = (brightness * Configuration.PREF_BRIGHTNESS_FACTOR).toInt()
                Timber.d("configureBrightnessLevels screenBrightness ${configuration.screenBrightness}")
                Timber.d("configureBrightnessLevels screenScreenSaverBrightness ${configuration.screenScreenSaverBrightness}")
            }
        } catch (e: SecurityException) {
            Timber.e(e.message)
        }
    }

    fun canWriteScreenSetting(): Boolean {
        Timber.d("canWriteScreenSetting")
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.System.canWrite(applicationContext)) {
            return true
        }
        return false
    }
}