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
import android.text.InputType
import android.view.*
import androidx.preference.SwitchPreference
import androidx.preference.EditTextPreference
import androidx.navigation.Navigation
import com.thanksmister.iot.wallpanel.R
import com.thanksmister.iot.wallpanel.ui.activities.SettingsActivity
import dagger.android.support.AndroidSupportInjection

class MqttSettingsFragment : BaseSettingsFragment() {

    private var mqttPreference: SwitchPreference? = null
    private var mqttBrokerAddress: EditTextPreference? = null
    private var mqttBrokerPort: EditTextPreference? = null
    private var mqttClientId: EditTextPreference? = null
    private var mqttBaseTopic: EditTextPreference? = null
    private var mqttUsername: EditTextPreference? = null
    private var mqttPassword: EditTextPreference? = null
    private var mqttDiscovery: SwitchPreference? = null
    private var mqttDiscoveryTopic: EditTextPreference? = null
    private var mqttDiscoveryDeviceName: EditTextPreference? = null

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
            (activity as SettingsActivity).supportActionBar!!.title = (getString(R.string.title_mqtt_settings))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_help, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            view?.let { Navigation.findNavController(it).navigate(R.id.settings_action) }
            return true
        } else if (id == R.id.action_help) {
            showSupport()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_mqtt)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        mqttPreference = findPreference<SwitchPreference>(getString(R.string.key_setting_mqtt_enabled)) as SwitchPreference
        mqttBrokerAddress = findPreference<EditTextPreference>(getString(R.string.key_setting_mqtt_servername)) as EditTextPreference
        mqttBrokerPort = findPreference<EditTextPreference>(getString(R.string.key_setting_mqtt_serverport)) as EditTextPreference
        mqttClientId = findPreference<EditTextPreference>(getString(R.string.key_setting_mqtt_clientid)) as EditTextPreference
        mqttBaseTopic = findPreference<EditTextPreference>(getString(R.string.key_setting_mqtt_basetopic)) as EditTextPreference
        mqttUsername = findPreference<EditTextPreference>(getString(R.string.key_setting_mqtt_username)) as EditTextPreference
        mqttPassword = findPreference<EditTextPreference>(getString(R.string.key_setting_mqtt_password)) as EditTextPreference
        mqttDiscovery = findPreference<SwitchPreference>(getString(R.string.key_setting_mqtt_home_assistant_discovery)) as SwitchPreference
        mqttDiscoveryTopic = findPreference<EditTextPreference>(getString(R.string.key_setting_mqtt_home_assistant_topic)) as EditTextPreference
        mqttDiscoveryDeviceName = findPreference<EditTextPreference>(getString(R.string.key_setting_mqtt_home_assistant_name)) as EditTextPreference

        mqttPassword?.setOnBindEditTextListener {editText ->
            // mask password in edit dialog
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        bindPreferenceSummaryToValue(mqttPreference!!)
        bindPreferenceSummaryToValue(mqttBrokerAddress!!)
        bindPreferenceSummaryToValue(mqttBrokerPort!!)
        bindPreferenceSummaryToValue(mqttClientId!!)
        bindPreferenceSummaryToValue(mqttBaseTopic!!)
        bindPreferenceSummaryToValue(mqttUsername!!)
        bindPreferenceSummaryToValue(mqttPassword!!)
        bindPreferenceSummaryToValue(mqttDiscovery!!)
        bindPreferenceSummaryToValue(mqttDiscoveryTopic!!)
        bindPreferenceSummaryToValue(mqttDiscoveryDeviceName!!)
    }
}