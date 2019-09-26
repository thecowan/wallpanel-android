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
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import androidx.preference.SwitchPreference
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.navigation.Navigation
import com.thanksmister.iot.wallpanel.R
import com.thanksmister.iot.wallpanel.ui.activities.SettingsActivity
import dagger.android.support.AndroidSupportInjection

class SensorsSettingsFragment : BaseSettingsFragment() {

    private var sensorsPreference: SwitchPreference? = null
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
            (activity as SettingsActivity).supportActionBar!!.title = (getString(R.string.title_sensor_settings))
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
        addPreferencesFromResource(R.xml.pref_sensors)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        sensorsPreference = findPreference(getString(R.string.key_setting_sensors_enabled)) as SwitchPreference
        mqttPublishFrequency = findPreference(getString(R.string.key_setting_mqtt_sensorfrequency)) as EditTextPreference

        bindPreferenceSummaryToValue(sensorsPreference!!)
        bindPreferenceSummaryToValue(mqttPublishFrequency!!)

        val mSensorManager = activity!!.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        setSensorPreferenceSummary(findPreference(getString(R.string.key_settings_sensors_temperature)), mSensorManager.getSensorList(Sensor.TYPE_AMBIENT_TEMPERATURE));
        setSensorPreferenceSummary(findPreference(getString(R.string.key_settings_sensors_light)), mSensorManager.getSensorList(Sensor.TYPE_LIGHT));
        setSensorPreferenceSummary(findPreference(getString(R.string.key_settings_sensors_magneticField)), mSensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD));
        setSensorPreferenceSummary(findPreference(getString(R.string.key_settings_sensors_pressure)), mSensorManager.getSensorList(Sensor.TYPE_PRESSURE));
        setSensorPreferenceSummary(findPreference(getString(R.string.key_settings_sensors_humidity)), mSensorManager.getSensorList(Sensor.TYPE_RELATIVE_HUMIDITY));
    }

    private fun setSensorPreferenceSummary(preference: Preference, sensorList: List<Sensor>) {
        if (sensorList.isNotEmpty()) { // Could we have multiple sensors of same type?
            preference.summary = sensorList[0].name
            //preference.isEnabled = true
        }
    }
}