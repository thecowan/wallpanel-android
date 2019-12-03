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
import android.content.Intent
import android.content.SharedPreferences
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
import androidx.navigation.Navigation
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import com.thanksmister.iot.wallpanel.R
import com.thanksmister.iot.wallpanel.persistence.Configuration.Companion.PREF_SCREENSAVER_DIM_VALUE
import com.thanksmister.iot.wallpanel.persistence.Configuration.Companion.PREF_SCREEN_BRIGHTNESS
import com.thanksmister.iot.wallpanel.persistence.Configuration.Companion.PREF_SCREEN_INACTIVITY_TIME
import com.thanksmister.iot.wallpanel.ui.activities.SettingsActivity
import com.thanksmister.iot.wallpanel.ui.activities.SettingsActivity.Companion.PERMISSIONS_REQUEST_WRITE_SETTINGS
import com.thanksmister.iot.wallpanel.utils.DateUtils
import com.thanksmister.iot.wallpanel.utils.DateUtils.SECONDS_VALUE
import com.thanksmister.iot.wallpanel.utils.ScreenUtils
import dagger.android.support.AndroidSupportInjection
import timber.log.Timber
import javax.inject.Inject

class SettingsFragment : BaseSettingsFragment() {

    @Inject
    lateinit var screenUtils: ScreenUtils

    private var openOnBootPreference: SwitchPreference? = null
    private var hadwareAcceleration: SwitchPreference? = null
    private var preventSleepPreference: SwitchPreference? = null
    private var browserActivityPreference: SwitchPreference? = null

    private var browserHeaderPreference: EditTextPreference? = null
    private var dashboardPreference: EditTextPreference? = null
    private var cameraPreference: Preference? = null
    private var mqttPreference: Preference? = null
    private var httpPreference: Preference? = null
    private var sensorsPreference: Preference? = null
    private var aboutPreference: Preference? = null
    private var brightnessPreference: Preference? = null
    private var listener: OnSettingsFragmentListener? = null
    private var browserRefreshPreference: SwitchPreference? = null
    private var clockSaverPreference: SwitchPreference? = null
    private var walllpaperSaverPreference: SwitchPreference? = null
    private var inactivityPreference: ListPreference? = null
    private var screenBrightness: SwitchPreference? = null
    private var dimPreference: ListPreference? = null

    interface OnSettingsFragmentListener {
        fun onFinish()
        fun onBrowserButton()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        if (context is OnSettingsFragmentListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnSettingsFragmentListener")
        }
        super.onAttach(context)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if ((activity as SettingsActivity).supportActionBar != null) {
            (activity as SettingsActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(false)
            (activity as SettingsActivity).supportActionBar!!.setDisplayShowHomeEnabled(false)
            (activity as SettingsActivity).supportActionBar!!.title = (getString(R.string.title_settings))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == PERMISSIONS_REQUEST_WRITE_SETTINGS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.System.canWrite(requireActivity().applicationContext)) {
                    //Toast.makeText(requireActivity(), getString(R.string.toast_write_permissions_granted), Toast.LENGTH_LONG).show()
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
        if (id == R.id.action_settings) {
            listener!!.onBrowserButton()
            return true
        } else if (id == R.id.action_help) {
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

        dashboardPreference = findPreference(getString(R.string.key_setting_app_launchurl)) as EditTextPreference
        browserHeaderPreference = findPreference(getString(R.string.key_setting_browser_user_agent)) as EditTextPreference
        preventSleepPreference = findPreference(getString(R.string.key_setting_app_preventsleep)) as SwitchPreference
        browserActivityPreference = findPreference(getString(R.string.key_setting_app_showactivity)) as SwitchPreference
        openOnBootPreference = findPreference(getString(R.string.key_setting_android_startonboot)) as SwitchPreference
        hadwareAcceleration = findPreference(getString(R.string.key_hadware_accelerated_enabled)) as SwitchPreference
        browserRefreshPreference = findPreference(getString(R.string.key_pref_browser_refresh)) as SwitchPreference
        clockSaverPreference = findPreference(getString(R.string.key_screensaver)) as SwitchPreference
        walllpaperSaverPreference = findPreference(getString(R.string.key_screensaver_wallpaper)) as SwitchPreference
        inactivityPreference = findPreference(PREF_SCREEN_INACTIVITY_TIME) as ListPreference
        dimPreference = findPreference(PREF_SCREENSAVER_DIM_VALUE) as ListPreference
        screenBrightness = findPreference(PREF_SCREEN_BRIGHTNESS) as SwitchPreference

        bindPreferenceSummaryToValue(dashboardPreference!!)
        bindPreferenceSummaryToValue(preventSleepPreference!!)
        bindPreferenceSummaryToValue(browserActivityPreference!!)
        bindPreferenceSummaryToValue(openOnBootPreference!!)
        bindPreferenceSummaryToValue(hadwareAcceleration!!)
        bindPreferenceSummaryToValue(browserHeaderPreference!!)
        bindPreferenceSummaryToValue(clockSaverPreference!!)
        bindPreferenceSummaryToValue(walllpaperSaverPreference!!)

        inactivityPreference?.setDefaultValue(configuration.inactivityTime)
        inactivityPreference?.value = configuration.inactivityTime.toString()

        if (configuration.inactivityTime < SECONDS_VALUE) {
            inactivityPreference?.summary = getString(R.string.preference_summary_inactivity_seconds,
                    DateUtils.convertInactivityTime(configuration.inactivityTime))
        } else {
            inactivityPreference?.summary = getString(R.string.preference_summary_inactivity_minutes,
                    DateUtils.convertInactivityTime(configuration.inactivityTime))
        }

        dimPreference?.setDefaultValue(configuration.screenSaverDimValue)
        dimPreference?.value = configuration.screenSaverDimValue.toString()
        dimPreference?.summary = getString(R.string.preference_summary_dim_screensaver, configuration.screenSaverDimValue.toString())

        cameraPreference = findPreference("button_key_camera")
        mqttPreference = findPreference("button_key_mqtt")
        httpPreference = findPreference("button_key_http")
        sensorsPreference = findPreference("button_key_sensors")
        aboutPreference = findPreference("button_key_about")
        brightnessPreference = findPreference("button_key_brightness")


        try {
            cameraPreference!!.onPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
                view.let { Navigation.findNavController(it).navigate(R.id.camera_action) }
                false
            }
            mqttPreference!!.onPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
                view.let { Navigation.findNavController(it).navigate(R.id.mqtt_action) }
                false
            }
            httpPreference!!.onPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
                view.let { Navigation.findNavController(it).navigate(R.id.http_action) }
                false
            }
            sensorsPreference!!.onPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
                view.let { Navigation.findNavController(it).navigate(R.id.sensors_action) }
                false
            }

            aboutPreference!!.onPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
                view.let { Navigation.findNavController(it).navigate(R.id.about_action) }
                false
            }

            brightnessPreference!!.onPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
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
                val useBright = screenBrightness!!.isChecked
                configuration.useScreenBrightness = useBright
                if(useBright) {
                    checkWriteSettings()
                } else {
                    screenUtils.restoreDeviceBrightnessControl()
                }
            }
            PREF_SCREEN_INACTIVITY_TIME -> {
                val inactivity = inactivityPreference?.value!!.toLong()
                configuration.inactivityTime = inactivity
                if (inactivity < SECONDS_VALUE) {
                    inactivityPreference?.summary = getString(R.string.preference_summary_inactivity_seconds, DateUtils.convertInactivityTime(inactivity))
                } else {
                    inactivityPreference?.summary = getString(R.string.preference_summary_inactivity_minutes, DateUtils.convertInactivityTime(inactivity))
                }
            }
            PREF_SCREENSAVER_DIM_VALUE -> {
                val dim = dimPreference?.value!!.toInt()
                configuration.screenSaverDimValue = dim
                screenUtils.setScreenBrightnessLevels()
                dimPreference?.summary = getString(R.string.preference_summary_dim_screensaver, dim.toString())
            }
        }
    }

    private fun checkWriteSettings() {
        Timber.d("checkWriteSettings")
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
}
