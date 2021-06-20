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

package com.thanksmister.iot.wallpanel.ui.fragments

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.Navigation
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import com.thanksmister.iot.wallpanel.R
import com.thanksmister.iot.wallpanel.persistence.Configuration.Companion.PREF_SCREENSAVER_DIM_VALUE
import com.thanksmister.iot.wallpanel.persistence.Configuration.Companion.PREF_SCREEN_BRIGHTNESS
import com.thanksmister.iot.wallpanel.ui.activities.SettingsActivity.Companion.PERMISSIONS_REQUEST_WRITE_SETTINGS
import com.thanksmister.iot.wallpanel.ui.views.SettingsCodeView
import com.thanksmister.iot.wallpanel.utils.DateUtils
import com.thanksmister.iot.wallpanel.utils.DateUtils.SECONDS_VALUE
import com.thanksmister.iot.wallpanel.utils.ScreenUtils
import dagger.android.support.AndroidSupportInjection
import timber.log.Timber
import javax.inject.Inject


class SettingsFragment : BaseSettingsFragment() {

    @Inject
    lateinit var screenUtils: ScreenUtils

    private var defaultCode: String = ""
    private var tempCode: String = ""
    private var confirmCode = false

    private var openOnBootPreference: SwitchPreference? = null
    private var hardwareAcceleration: SwitchPreference? = null
    private var preventSleepPreference: SwitchPreference? = null
    private var browserActivityPreference: SwitchPreference? = null
    private var ignoreSSLErrorsPreference: SwitchPreference? = null
    private var browserHeaderPreference: EditTextPreference? = null
    private var dashboardPreference: EditTextPreference? = null
    private var cameraPreference: Preference? = null
    private var mqttPreference: Preference? = null
    private var httpPreference: Preference? = null
    private var sensorsPreference: Preference? = null
    private var aboutPreference: Preference? = null
    private var brightnessPreference: Preference? = null
    private var browserRefreshPreference: SwitchPreference? = null

    private var clockSaverPreference: SwitchPreference? = null
    private var inactivityPreference: ListPreference? = null
    private var screenBrightness: SwitchPreference? = null
    private var dimPreference: ListPreference? = null
    private var rotationPreference: EditTextPreference? = null

    private val fullScreenPreference: SwitchPreference by lazy {
        findPreference<SwitchPreference>(PREF_SETTINGS_FULL_SCREEN) as SwitchPreference
    }

    private val settingsTransparentPreference: SwitchPreference by lazy {
        findPreference<SwitchPreference>(PREF_SETTINGS_BUTTON_TRANSPARENT) as SwitchPreference
    }

    private val browserRefreshOnDisconnect: SwitchPreference by lazy {
        findPreference<SwitchPreference>(PREF_SETTINGS_REFRESH_ON_DISCONNECT) as SwitchPreference
    }

    private val settingsDisablePreference: SwitchPreference by lazy {
        findPreference<SwitchPreference>(PREF_SETTINGS_BUTTON_DISABLE) as SwitchPreference
    }

    private val settingsLocationPreference: ListPreference by lazy {
        findPreference<ListPreference>(PREF_SETTINGS_BUTTON_LOCATION) as ListPreference
    }

    private val useDarkThemeSettings: SwitchPreference by lazy {
        findPreference<SwitchPreference>(PREF_SETTINGS_THEME) as SwitchPreference
    }

    private val codePreference: Preference by lazy {
        findPreference<Preference>("button_alarm_code") as Preference
    }

    private val resetHomeApp: Preference by lazy {
        findPreference<Preference>("button_reset_home_app") as Preference
    }

    private val userAgentPreference: EditTextPreference by lazy {
        findPreference<EditTextPreference>(PREF_SETTINGS_USER_AGENT) as EditTextPreference
    }

    private val dimScreensaver: SwitchPreference by lazy {
        findPreference<SwitchPreference>(PREF_SETTINGS_SCREENSAVER_DIM) as SwitchPreference
    }

    private val wallpaperSaverPreference: SwitchPreference by lazy {
        findPreference<SwitchPreference>("settings_wallpaper_screensaver") as SwitchPreference
    }

    private val webScreenSaver: SwitchPreference by lazy {
        findPreference<SwitchPreference>(PREF_SETTINGS_WEB_SCREENSAVER) as SwitchPreference
    }

    private val webScreenSaverUrl: EditTextPreference by lazy {
        findPreference<EditTextPreference>(PREF_SETTINGS_WEB_SCREENSAVER_URL) as EditTextPreference
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == PERMISSIONS_REQUEST_WRITE_SETTINGS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.System.canWrite(requireActivity().applicationContext)) {
                    screenBrightness?.isChecked = true
                    configuration.useScreenBrightness = true
                    Toast.makeText(requireContext(), getString(R.string.toast_screen_brightness_captured), Toast.LENGTH_SHORT).show()
                    screenUtils.setScreenBrightnessLevels()
                } else {
                    Toast.makeText(requireActivity(), getString(R.string.toast_write_permissions_denied), Toast.LENGTH_LONG).show()
                    configuration.useScreenBrightness = false
                    screenBrightness?.isChecked = false
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_dashboard, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_help) {
            showSupport()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_general)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        dashboardPreference = findPreference<EditTextPreference>(PREF_SETTINGS_DASHBOARD_URL) as EditTextPreference
        browserHeaderPreference = findPreference<EditTextPreference>(PREF_SETTINGS_USER_AGENT) as EditTextPreference
        preventSleepPreference = findPreference<SwitchPreference>(getString(R.string.key_setting_app_preventsleep)) as SwitchPreference
        browserActivityPreference = findPreference<SwitchPreference>(getString(R.string.key_setting_app_showactivity)) as SwitchPreference
        openOnBootPreference = findPreference<SwitchPreference>(getString(R.string.key_setting_android_startonboot)) as SwitchPreference
        hardwareAcceleration = findPreference<SwitchPreference>(getString(R.string.key_hadware_accelerated_enabled)) as SwitchPreference
        browserRefreshPreference = findPreference<SwitchPreference>(getString(R.string.key_pref_browser_refresh)) as SwitchPreference
        clockSaverPreference = findPreference<SwitchPreference>(getString(R.string.key_screensaver)) as SwitchPreference
        inactivityPreference = findPreference<ListPreference>(PREF_SCREEN_INACTIVITY_TIME) as ListPreference
        dimPreference = findPreference<ListPreference>(PREF_SCREENSAVER_DIM_VALUE) as ListPreference
        screenBrightness = findPreference<SwitchPreference>(PREF_SCREEN_BRIGHTNESS) as SwitchPreference
        ignoreSSLErrorsPreference = findPreference<SwitchPreference>(getString(R.string.key_setting_ignore_ssl_errors)) as SwitchPreference

        browserRefreshOnDisconnect.isChecked = configuration.browserRefreshDisconnect
        fullScreenPreference.isChecked = configuration.fullScreen
        settingsTransparentPreference.isChecked = configuration.settingsTransparent
        settingsDisablePreference.isChecked = configuration.settingsDisabled
        useDarkThemeSettings.isChecked = configuration.useDarkTheme
        userAgentPreference.text = configuration.browserUserAgent
        dashboardPreference?.text = configuration.appLaunchUrl

        if(configuration.appLaunchUrl.isNotEmpty()) {
            dashboardPreference?.summary = configuration.appLaunchUrl
        }

        val code = configuration.settingsCode.toString()
        if (code.isNotEmpty()) {
            codePreference.summary = toStars(code)
        }

        // TODO deprecate this
        bindPreferenceSummaryToValue(preventSleepPreference!!)
        bindPreferenceSummaryToValue(browserActivityPreference!!)
        bindPreferenceSummaryToValue(openOnBootPreference!!)
        bindPreferenceSummaryToValue(hardwareAcceleration!!)
        bindPreferenceSummaryToValue(browserHeaderPreference!!)
        bindPreferenceSummaryToValue(clockSaverPreference!!)
        bindPreferenceSummaryToValue(ignoreSSLErrorsPreference!!)

        inactivityPreference?.setDefaultValue(configuration.inactivityTime)
        inactivityPreference?.value = configuration.inactivityTime.toString()

        if (configuration.inactivityTime < SECONDS_VALUE) {
            inactivityPreference?.summary = getString(R.string.preference_summary_inactivity_seconds,
                    DateUtils.convertInactivityTime(configuration.inactivityTime))
        } else {
            inactivityPreference?.summary = getString(R.string.preference_summary_inactivity_minutes,
                    DateUtils.convertInactivityTime(configuration.inactivityTime))
        }

        settingsLocationPreference.setDefaultValue(configuration.settingsLocation)
        settingsLocationPreference.value = configuration.settingsLocation.toString()
        settingsLocationPreference.summary = settingsLocationPreference.entry

        dimPreference?.setDefaultValue(configuration.screenSaverDimValue)
        dimPreference?.value = configuration.screenSaverDimValue.toString()
        dimPreference?.summary = getString(R.string.preference_summary_dim_screensaver, configuration.screenSaverDimValue.toString())

        cameraPreference = findPreference("button_key_camera")
        mqttPreference = findPreference("button_key_mqtt")
        httpPreference = findPreference("button_key_http")
        sensorsPreference = findPreference("button_key_sensors")
        aboutPreference = findPreference("button_key_about")
        brightnessPreference = findPreference("button_key_brightness")

        rotationPreference = findPreference("pref_settings_image_rotation")
        rotationPreference?.text = configuration.imageRotation.toString()
        rotationPreference?.summary = getString(R.string.preference_summary_image_rotation, configuration.imageRotation.toString())
        rotationPreference?.setDefaultValue(configuration.imageRotation.toString())

        // Setup additional screensaver options
        webScreenSaver.isChecked = configuration.webScreenSaver
        dimScreensaver.isChecked = configuration.hasDimScreenSaver
        wallpaperSaverPreference.isChecked = configuration.hasScreenSaverWallpaper

        if( wallpaperSaverPreference.isChecked) {
            setWallPaperScreensaver(true)
        } else if (webScreenSaver.isChecked) {
            setWebScreensaver(true)
        }

        val webScreensaverUrlValue = configuration.webScreenSaverUrl
        if(webScreensaverUrlValue.isNotEmpty()) {
            webScreenSaverUrl.text = webScreensaverUrlValue
        }

        try {
            cameraPreference?.onPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
                view.let { Navigation.findNavController(it).navigate(R.id.camera_action) }
                false
            }
            mqttPreference?.onPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
                view.let { Navigation.findNavController(it).navigate(R.id.mqtt_action) }
                false
            }
            httpPreference?.onPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
                view.let { Navigation.findNavController(it).navigate(R.id.http_action) }
                false
            }
            sensorsPreference?.onPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
                view.let { Navigation.findNavController(it).navigate(R.id.sensors_action) }
                false
            }
            codePreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                showCodeDialog()
                true
            }
            resetHomeApp.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                resetHomeApp()
                true
            }
            aboutPreference?.onPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
                view.let { Navigation.findNavController(it).navigate(R.id.about_action) }
                false
            }

            brightnessPreference?.onPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
                screenUtils.setScreenBrightnessLevels()
                Toast.makeText(requireContext(), getString(R.string.toast_screen_brightness_captured), Toast.LENGTH_SHORT).show()
                false
            }
        }  catch (e: IllegalArgumentException) {
            Timber.d(e.message)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            PREF_SCREEN_BRIGHTNESS -> {
                val useBright = screenBrightness?.isChecked
                if (useBright != null) {
                    configuration.useScreenBrightness = useBright
                    if (useBright) {
                        checkWriteSettings()
                    } else {
                        screenUtils.restoreDeviceBrightnessControl()
                    }
                }
            }
            PREF_SCREEN_INACTIVITY_TIME -> {
                val inactivity = inactivityPreference?.value?.toLongOrNull()
                if (inactivity != null) {
                    configuration.inactivityTime = inactivity
                    if (inactivity < SECONDS_VALUE) {
                        inactivityPreference?.summary = getString(R.string.preference_summary_inactivity_seconds, DateUtils.convertInactivityTime(inactivity))
                    } else {
                        inactivityPreference?.summary = getString(R.string.preference_summary_inactivity_minutes, DateUtils.convertInactivityTime(inactivity))
                    }
                }
            }
            PREF_SCREENSAVER_DIM_VALUE -> {
                val dim = dimPreference?.value?.toIntOrNull()
                if (dim != null) {
                    configuration.screenSaverDimValue = dim
                    screenUtils.setScreenBrightnessLevels()
                    dimPreference?.summary = getString(R.string.preference_summary_dim_screensaver, dim.toString())
                } else {
                    Toast.makeText(requireContext(), getString(R.string.toast_error_face_size), Toast.LENGTH_SHORT).show()
                }
            }
            PREF_SETTINGS_FULL_SCREEN -> {
                configuration.fullScreen = fullScreenPreference.isChecked
            }
            PREF_SETTINGS_BUTTON_TRANSPARENT -> {
                configuration.settingsTransparent = settingsTransparentPreference.isChecked
            }
            PREF_SETTINGS_BUTTON_DISABLE -> {
                configuration.settingsDisabled = settingsDisablePreference.isChecked
            }
            PREF_SETTINGS_THEME -> {
                configuration.useDarkTheme = useDarkThemeSettings.isChecked
                if (configuration.useDarkTheme) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    requireActivity().recreate()
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    requireActivity().recreate()
                }
            }
            PREF_SETTINGS_BUTTON_LOCATION -> {
                val value = settingsLocationPreference.value?.toIntOrNull()
                if (value != null) {
                    configuration.settingsLocation = value
                    settingsLocationPreference.summary = settingsLocationPreference.entry
                }
            }
            PREF_SETTINGS_DASHBOARD_URL -> {
                val value = dashboardPreference?.text.orEmpty()
                if (value.isNotEmpty()) {
                    configuration.appLaunchUrl = value
                    dashboardPreference?.summary = value
                }
            }
            PREF_SETTINGS_USER_AGENT -> {
                val value = userAgentPreference.text.orEmpty()
                if (value.isNotEmpty()) {
                    configuration.browserUserAgent = value
                    userAgentPreference.summary = value
                }
            }
            PREF_SETTINGS_WEB_SCREENSAVER_URL -> {
                val value = webScreenSaverUrl.text.orEmpty()
                if (value.isNotEmpty()) {
                    configuration.webScreenSaverUrl = value
                    webScreenSaverUrl.summary = value
                }
            }
            "settings_wallpaper_screensaver" -> {
                val value = wallpaperSaverPreference.isChecked
                configuration.hasScreenSaverWallpaper = value
                setWallPaperScreensaver(value)
            }
            PREF_SETTINGS_REFRESH_ON_DISCONNECT -> {
                val value = browserRefreshOnDisconnect.isChecked
                configuration.browserRefreshDisconnect = value
            }
            PREF_SETTINGS_WEB_SCREENSAVER -> {
                val value = webScreenSaver.isChecked
                configuration.webScreenSaver = value
                setWebScreensaver(value)
            }
            PREF_SETTINGS_SCREENSAVER_DIM -> {
                val value = dimScreensaver.isChecked
                configuration.hasDimScreenSaver = value
                if(value) {
                    setWallPaperScreensaver(false)
                    setWebScreensaver(false)
                }
            }

            "pref_settings_image_rotation" -> {
                rotationPreference?.text?.let {
                    val rotation = it.toIntOrNull()
                    if (rotation != null) {
                        configuration.imageRotation = rotation
                        rotationPreference?.summary = getString(R.string.preference_summary_image_rotation, rotation.toString())
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.toast_error_bad_decimal), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun setWebScreensaver(value: Boolean) {
        configuration.webScreenSaver = value
        webScreenSaver.isChecked = value
        if(value) {
            configuration.hasScreenSaverWallpaper = false
            wallpaperSaverPreference.isChecked = false
            setDimScreensaver(false)
        }
    }

    private fun setWallPaperScreensaver(value: Boolean) {
        configuration.hasScreenSaverWallpaper = value
        wallpaperSaverPreference.isChecked = value
        if(value) {
            configuration.webScreenSaver = false
            webScreenSaver.isChecked = false
            setDimScreensaver(false)
        }
    }

    private fun setDimScreensaver(value: Boolean) {
        configuration.hasDimScreenSaver = value
        dimScreensaver.isChecked = value
    }


    private fun checkWriteSettings() {
        if (!configuration.writeScreenPermissionsShown && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(requireActivity().applicationContext)) {
                screenUtils.setScreenBrightnessLevels()
                Toast.makeText(requireContext(), getString(R.string.toast_screen_brightness_captured), Toast.LENGTH_SHORT).show()
            } else if (!configuration.writeScreenPermissionsShown) {
                // launch the dialog to provide permissions
                configuration.writeScreenPermissionsShown = true
                AlertDialog.Builder(requireActivity())
                        .setMessage(getString(R.string.dialog_write_permissions_description))
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            launchWriteSettings()
                        }
                        .setNegativeButton(android.R.string.cancel) { _, _ ->
                            Toast.makeText(requireActivity(), getString(R.string.toast_write_permissions_denied), Toast.LENGTH_LONG).show()
                        }.show()
            }
        } else if (configuration.useScreenBrightness) {
            // rewrite the screen brightness levels until we have a slider in place
            screenUtils.setScreenBrightnessLevels()
            Toast.makeText(requireContext(), getString(R.string.toast_screen_brightness_captured), Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun launchWriteSettings() {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${requireActivity().applicationContext.packageName}"))
        startActivityForResult(intent, 200)
    }

    private fun showCodeDialog() {
        defaultCode = configuration.settingsCode
        if (activity != null && isAdded) {
            dialogUtils.showCodeDialog(requireActivity(), confirmCode, object : SettingsCodeView.ViewListener {
                override fun onComplete(code: String) {
                    if (code == defaultCode) {
                        confirmCode = false
                        dialogUtils.clearDialogs()
                        Toast.makeText(activity, R.string.toast_code_match, Toast.LENGTH_LONG).show()
                    } else if (!confirmCode) {
                        tempCode = code
                        confirmCode = true
                        dialogUtils.clearDialogs()
                        if (activity != null && isAdded) {
                            showCodeDialog()
                        }
                    } else if (code == tempCode) {
                        configuration.isFirstTime = false;
                        configuration.settingsCode = tempCode
                        tempCode = ""
                        confirmCode = false
                        dialogUtils.clearDialogs()
                        Toast.makeText(activity, R.string.toast_code_changed, Toast.LENGTH_LONG).show()
                    } else {
                        tempCode = ""
                        confirmCode = false
                        dialogUtils.clearDialogs()
                        Toast.makeText(activity, R.string.toast_code_not_match, Toast.LENGTH_LONG).show()
                    }
                }
                override fun onError() {}
                override fun onCancel() {
                    confirmCode = false
                    dialogUtils.clearDialogs()
                    Toast.makeText(activity, R.string.toast_code_unchanged, Toast.LENGTH_SHORT).show()
                }
            }, DialogInterface.OnCancelListener {
                confirmCode = false
                Toast.makeText(activity, R.string.toast_code_unchanged, Toast.LENGTH_SHORT).show()
            })
        }
    }

    private fun toStars(textToStars: String?): String {
        var text = textToStars
        val sb = StringBuilder()
        for (i in text.orEmpty().indices) {
            sb.append('*')
        }
        text = sb.toString()
        return text
    }

    private fun resetHomeApp() {
        val pm: PackageManager = requireActivity().packageManager
        pm.clearPackagePreferredActivities(requireActivity().packageName)
        requireActivity().recreate()
    }

    companion object {
        const val PREF_SCREEN_INACTIVITY_TIME = "pref_screensaver_inactivity_time"
        const val PREF_SETTINGS_FULL_SCREEN = "pref_settings_fullscreen"
        const val PREF_SETTINGS_BUTTON_TRANSPARENT = "pref_settings_button_transparent"
        const val PREF_SETTINGS_REFRESH_ON_DISCONNECT = "pref_settings_refresh_on_disconnect"
        const val PREF_SETTINGS_BUTTON_DISABLE = "pref_settings_button_disable"
        const val PREF_SETTINGS_BUTTON_LOCATION = "pref_settings_button_location"
        const val PREF_SETTINGS_THEME = "pref_settings_theme"
        const val PREF_SETTINGS_DASHBOARD_URL = "pref_settings_dashboard_url"
        const val PREF_SETTINGS_USER_AGENT = "pref_settings_user_agent"
        const val PREF_SETTINGS_SCREENSAVER_DIM = "settings_screensaver_dim"
        const val PREF_SETTINGS_WEB_SCREENSAVER = "settings_screensaver_web"
        const val PREF_SETTINGS_WEB_SCREENSAVER_URL = "settings_screensaver_web_url"
    }
}
