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

package com.thanksmister.iot.wallpanel.ui.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v14.preference.SwitchPreference
import android.support.v7.preference.CheckBoxPreference
import android.support.v7.preference.EditTextPreference
import android.support.v7.preference.ListPreference
import android.support.v7.preference.Preference
import android.view.MenuItem
import android.view.View
import androidx.navigation.Navigation
import com.thanksmister.iot.wallpanel.R
import com.thanksmister.iot.wallpanel.ui.activities.SettingsActivity
import dagger.android.support.AndroidSupportInjection

class FaceDetectionSettingsFragment : BaseSettingsFragment() {

    private var motionDetectionPreference: SwitchPreference? = null
    private var mqttBrokerAddress: EditTextPreference? = null
    private var mqttBrokerPort: EditTextPreference? = null
    private var mqttClientId: EditTextPreference? = null
    private var mqttBaseTopic: EditTextPreference? = null
    private var mqttUsername: EditTextPreference? = null
    private var mqttPassword: EditTextPreference? = null
    private var mqttPublishFrequency: EditTextPreference? = null

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
        setHasOptionsMenu(true)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if((activity as SettingsActivity).supportActionBar != null) {
            (activity as SettingsActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            (activity as SettingsActivity).supportActionBar!!.setDisplayShowHomeEnabled(true)
            (activity as SettingsActivity).supportActionBar!!.title = (getString(R.string.title_motion_settings))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            view?.let { Navigation.findNavController(it).navigate(R.id.settings_action) }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_motion)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        motionDetectionPreference = findPreference(getString(R.string.key_setting_mqtt_enabled)) as SwitchPreference
        mqttBrokerAddress = findPreference(getString(R.string.key_setting_mqtt_servername)) as EditTextPreference
        mqttBrokerPort = findPreference(getString(R.string.key_setting_mqtt_serverport)) as EditTextPreference
        mqttClientId = findPreference(getString(R.string.key_setting_mqtt_clientid)) as EditTextPreference
        mqttBaseTopic = findPreference(getString(R.string.key_setting_mqtt_basetopic)) as EditTextPreference
        mqttUsername = findPreference(getString(R.string.key_setting_mqtt_username)) as EditTextPreference
        mqttPassword = findPreference(getString(R.string.key_setting_mqtt_password)) as EditTextPreference
        mqttPublishFrequency = findPreference(getString(R.string.key_setting_mqtt_sensorfrequency)) as EditTextPreference

        bindPreferenceSummaryToValue(motionDetectionPreference!!)
        bindPreferenceSummaryToValue(mqttBrokerAddress!!)
        bindPreferenceSummaryToValue(mqttBrokerPort!!)
        bindPreferenceSummaryToValue(mqttClientId!!)
        bindPreferenceSummaryToValue(mqttBaseTopic!!)
        bindPreferenceSummaryToValue(mqttUsername!!)
        bindPreferenceSummaryToValue(mqttPassword!!)
        bindPreferenceSummaryToValue(mqttPublishFrequency!!)
    }
}