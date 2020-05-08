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
import javax.inject.Inject

class ScreenUtils @Inject
constructor(context: Context, private val configuration: Configuration): ContextWrapper(context) {

    fun resetScreenBrightness(screenSaver: Boolean = true) {
        Timber.d("resetScreenBrightness useScreenBrightness ${configuration.useScreenBrightness}")
        if(configuration.useScreenBrightness) {
            Timber.d("resetScreenBrightness")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && canWriteScreenSetting()) {
                setDeviceBrightnessControl(screenSaver)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !canWriteScreenSetting()) {
                restoreDeviceBrightnessControl()
            } else {
                setDeviceBrightnessControl(screenSaver)
            }
        } else {
            Timber.d("resetScreenBrightness ignored")
            restoreDeviceBrightnessControl()
        }
    }

    fun getCurrentScreenBrightness(): Int {
        return try {
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Exception) {
            0
        }
    }

    private fun setDeviceBrightnessControl(screenSaver: Boolean) {
        Timber.d("setDeviceBrightnessControl screenSaver $screenSaver")
        if(canWriteScreenSetting()) {
            setDeviceBrightnessMode(false)
            try {
                if (configuration.screenBrightness in 1..255 && !screenSaver) {
                    Timber.d("calculated brightness ${configuration.screenBrightness}")
                    Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, configuration.screenBrightness)
                } else if (configuration.screenScreenSaverBrightness in 1..255 && screenSaver) {
                    Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, configuration.screenScreenSaverBrightness)
                    Timber.d("calculated screensaver brightness ${configuration.screenScreenSaverBrightness}")
                }
            } catch (e: SecurityException) {
                Timber.e(e.message)
            }
        }
    }

    fun setScreenBrightnessLevels() {
        Timber.d("setScreenBrightnessLevels")
        try {
            val brightness = getCurrentScreenBrightness()
            updateScreenBrightness(brightness)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateScreenBrightness(brightness: Int) {
        Timber.d("setScreenBrightness $brightness")
        if(canWriteScreenSetting()) {
            try {
                if (brightness in 1..255) {
                    Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness)
                    configuration.screenBrightness = brightness
                    Timber.d("screenBrightness $brightness")
                    Timber.d("screenSaverDimValue ${configuration.screenSaverDimValue}")
                    if(configuration.screenSaverDimValue > 0) {
                        val dimAmount = brightness - (brightness * configuration.screenSaverDimValue/100)
                        Timber.d("dimAmount $dimAmount")
                        configuration.screenScreenSaverBrightness = dimAmount
                    } else {
                        configuration.screenScreenSaverBrightness = brightness
                    }
                    Timber.d("screenScreenSaverBrightness ${configuration.screenScreenSaverBrightness}")
                }
            } catch (e: SecurityException) {
                Timber.e(e.message)
            }
        }
    }

    // The user no longer has screen write permission or has chosen to not use this permission
    // we want reset device to automatic mode and reset the screen brightness to the last brightens settings
    // we also want to stop using screen brightness
    fun restoreDeviceBrightnessControl() {
        if(canWriteScreenSetting()) {
            Timber.d("restoreDeviceBrightnessControl")
            configuration.useScreenBrightness = false
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, getCurrentScreenBrightness())
            configuration.screenBrightness = getCurrentScreenBrightness()
            try {
                if(configuration.screenSaverDimValue > 0) {
                    val dimAmount = configuration.screenBrightness - (configuration.screenBrightness * configuration.screenSaverDimValue/100)
                    configuration.screenScreenSaverBrightness = dimAmount
                } else {
                    configuration.screenScreenSaverBrightness = configuration.screenBrightness
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            setDeviceBrightnessMode(true)
        }
    }

    private fun setDeviceBrightnessMode(automatic: Boolean = false) {
        var mode = -1
        try {
            mode = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE) //this will return integer (0 or 1)
        } catch (e: Settings.SettingNotFoundException) {
            Timber.e(e.message)
        }
        try {
            if(automatic) {
                if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL) {
                    //reset back to automatic mode
                    Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
                }
            } else {
                if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                    //Automatic mode, need to be in manual to change brightness
                    Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                }
            }
        } catch (e: SecurityException) {
            Timber.e(e.message)
        }
    }

    private fun canWriteScreenSetting(): Boolean {
        Timber.d("canWriteScreenSetting")
        var hasPermission = true
        // Check for permisions if >= Android 6
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hasPermission = Settings.System.canWrite(applicationContext)
        }
        return hasPermission
    }
}