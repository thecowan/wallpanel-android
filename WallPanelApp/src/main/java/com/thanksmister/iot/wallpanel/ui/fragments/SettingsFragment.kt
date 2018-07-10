
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

package com.thanksmister.iot.wallpanel.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v14.preference.SwitchPreference
import android.support.v4.content.ContextCompat
import android.support.v7.preference.*
import android.view.View
import com.thanksmister.iot.wallpanel.controls.CameraReader
import com.thanksmister.iot.wallpanel.R
import com.thanksmister.iot.wallpanel.persistence.Configuration
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject
    lateinit var configuration: Configuration

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_general)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        //na-da
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_app_preventsleep)))
        bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_app_launchurl)))
        bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_app_showactivity)))

        bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_android_startonboot)))
        bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_android_browsertype)))

        bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_http_restenabled)))
        bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_http_port)))
        bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_http_mjpegenabled)))
        bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_http_mjpegmaxstreams)))

        bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_mqtt_enabled)))
        bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_mqtt_servername)))
        bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_mqtt_serverport)))
        bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_mqtt_basetopic)))
        bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_mqtt_clientid)))
        bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_mqtt_username)))

        val prefSensorFrequencyRange = findPreference(getString(R.string.key_setting_mqtt_sensorfrequency))
        val preferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            val summary = getString(R.string.text_seconds, newValue)
            preference.summary = summary
            true
        }

        prefSensorFrequencyRange.onPreferenceChangeListener = preferenceChangeListener
        preferenceChangeListener.onPreferenceChange(prefSensorFrequencyRange,
                PreferenceManager.getDefaultSharedPreferences(prefSensorFrequencyRange.context).getString(prefSensorFrequencyRange.key, ""))

        val cameraTestPreference = findPreference(getString(R.string.key_camera_test))
        cameraTestPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
            startCameraTestActivity(preference.context)
            false
        }

        bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_camera_enabled)))
        bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_camera_motionenabled)))
        bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_camera_motionwake)))
        bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_camera_motionbright)))
        bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_camera_motionontime)))
        bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_camera_processinginterval)))
        bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_camera_motionleniency)))
        bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_camera_motionminluma)))
        bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_camera_faceenabled)))
        bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_camera_facewake)))
        bindPreferenceSummaryToValue(findPreference(getString(R.string.key_setting_camera_qrcodeenabled)))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isAdded && activity != null) {
            if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(activity!!, android.Manifest.permission.CAMERA)) {
                createCameraList()
                cameraTestPreference.isEnabled = true
            } else {
                cameraTestPreference.isEnabled = false
            }
        } else {
            createCameraList()
        }
    }

    private fun createCameraList() {
        val cameras = findPreference(getString(R.string.key_setting_camera_cameraid)) as ListPreference
        val cameraList = CameraReader.getCameras()
        cameras.entries = cameraList.toTypedArray<CharSequence>()
        val vals = arrayOfNulls<CharSequence>(cameraList.size)
        for (i in cameraList.indices) {
            vals[i] = Integer.toString(i)
        }
        cameras.entryValues = vals
        bindPreferenceSummaryToValue(cameras)
    }

    private fun startCameraTestActivity(c: Context) {
        startActivity(Intent(c, CameraTestActivity::class.java))
    }

    companion object {

        private val sBindPreferenceSummaryToValueListener = Preference.OnPreferenceChangeListener { preference, value ->
            val stringValue = value.toString()

            if (preference is SwitchPreference) {
                return@OnPreferenceChangeListener true
            }

            if (preference is ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                val index = preference.findIndexOfValue(stringValue)

                // Set the summary to reflect the new value.
                preference.setSummary(
                        if (index >= 0)
                            preference.entries[index]
                        else
                            null)

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.

                preference.summary = stringValue
            }
            true
        }

        /**
         * Binds a preference's summary to its value. More specifically, when the
         * preference's value is changed, its summary (line of text below the
         * preference title) is updated to reflect the value. The summary is also
         * immediately updated upon calling this method. The exact display format is
         * dependent on the type of preference.
         *
         * @see .sBindPreferenceSummaryToValueListener
         */
        private fun bindPreferenceSummaryToValue(preference: Preference) {

            // Set the listener to watch for value changes.
            preference.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener

            if (preference is SwitchPreference) {
                sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                        PreferenceManager
                                .getDefaultSharedPreferences(preference.getContext())
                                .getBoolean(preference.getKey(), false))
            } else {
                sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                        PreferenceManager
                                .getDefaultSharedPreferences(preference.context)
                                .getString(preference.key, "")!!)
            }

        }
    }
}// Required empty public constructor
