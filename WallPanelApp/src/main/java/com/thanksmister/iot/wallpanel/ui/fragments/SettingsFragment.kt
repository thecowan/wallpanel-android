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
import android.os.Bundle
import android.support.v14.preference.SwitchPreference
import android.support.v7.preference.CheckBoxPreference
import android.support.v7.preference.EditTextPreference
import android.support.v7.preference.ListPreference
import android.support.v7.preference.Preference
import android.view.*
import androidx.navigation.Navigation
import com.thanksmister.iot.wallpanel.R
import com.thanksmister.iot.wallpanel.ui.activities.SettingsActivity
import com.thanksmister.iot.wallpanel.utils.DateUtils
import com.thanksmister.iot.wallpanel.utils.DateUtils.SECONDS_VALUE
import dagger.android.support.AndroidSupportInjection
import timber.log.Timber

class SettingsFragment : BaseSettingsFragment() {


    private var openOnBootPreference: SwitchPreference? = null
    private var preventSleepPreference: SwitchPreference? = null
    private var browserActivityPreference: SwitchPreference? = null

    private var browserHeaderPreference: EditTextPreference? = null
    private var dashboardPreference: EditTextPreference? = null
    private var cameraPreference: Preference? = null
    private var mqttPreference: Preference? = null
    private var httpPreference: Preference? = null
    private var sensorsPreference: Preference? = null
    private var aboutPreference: Preference? = null
    private var listener: OnSettingsFragmentListener? = null
    private var browserRefreshPreference: SwitchPreference? = null
    private var clockSaverPreference: SwitchPreference? = null
    private var inactivityPreference: ListPreference? = null

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

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater!!.inflate(R.menu.menu_dashboard, menu)
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
        browserRefreshPreference = findPreference(getString(R.string.key_pref_browser_refresh)) as SwitchPreference
        clockSaverPreference = findPreference(getString(R.string.key_screensaver)) as SwitchPreference
        inactivityPreference = findPreference(getString(R.string.key_inactivity_time)) as ListPreference

        bindPreferenceSummaryToValue(dashboardPreference!!)
        bindPreferenceSummaryToValue(preventSleepPreference!!)
        bindPreferenceSummaryToValue(browserActivityPreference!!)
        bindPreferenceSummaryToValue(openOnBootPreference!!)
        bindPreferenceSummaryToValue(browserHeaderPreference!!)
        bindPreferenceSummaryToValue(clockSaverPreference!!)
        bindPreferenceSummaryToValue(inactivityPreference!!)

        cameraPreference = findPreference("button_key_camera")
        mqttPreference = findPreference("button_key_mqtt")
        httpPreference = findPreference("button_key_http")
        sensorsPreference = findPreference("button_key_sensors")
        aboutPreference = findPreference("button_key_about")

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
    }

    companion object {
    }
}
