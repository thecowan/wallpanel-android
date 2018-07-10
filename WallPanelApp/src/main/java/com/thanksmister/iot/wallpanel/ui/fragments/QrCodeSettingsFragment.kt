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
import android.support.v7.preference.CheckBoxPreference
import android.support.v7.preference.EditTextPreference
import android.support.v7.preference.ListPreference
import android.support.v7.preference.Preference
import android.view.View
import androidx.navigation.Navigation
import com.thanksmister.iot.wallpanel.R
import com.thanksmister.iot.wallpanel.ui.activities.SettingsActivity
import dagger.android.support.AndroidSupportInjection

class QrCodeSettingsFragment : BaseSettingsFragment() {

    private var cameraListPreference: ListPreference? = null
    private var cameraTestPreference: Preference? = null
    private var qrCodePreference: Preference? = null
    private var motionDetectionPreference: Preference? = null
    private var processingIntervalPreference: EditTextPreference? = null
    private var cameraPreference: CheckBoxPreference? = null
    private var faceDetectionPreference: Preference? = null

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // Set title bar
        if((activity as SettingsActivity).supportActionBar != null) {
            (activity as SettingsActivity).supportActionBar!!.title = (getString(R.string.title_mqtt_settings))
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_camera)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        cameraPreference = findPreference(getString(R.string.key_setting_camera_enabled)) as CheckBoxPreference
        processingIntervalPreference = findPreference(getString(R.string.key_setting_camera_processinginterval)) as EditTextPreference
        cameraListPreference = findPreference(getString(R.string.key_setting_camera_cameraid)) as ListPreference
        cameraListPreference!!.setOnPreferenceChangeListener { preference, newValue ->
            if (preference is ListPreference) {
                val index = preference.findIndexOfValue(newValue.toString())
                preference.setSummary(
                        if (index >= 0)
                            preference.entries[index]
                        else
                            "")
            }
           true;
        }

        motionDetectionPreference = findPreference("button_key_motion_detection")
        faceDetectionPreference = findPreference("button_key_face_detection")
        qrCodePreference = findPreference("button_key_qr_code")
        cameraTestPreference = findPreference("button_key_camera_test")

        processingIntervalPreference!!.summary = configuration.cameraProcessingInterval.toString()

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (ActivityCompat.checkSelfPermission(activity!!.applicationContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                cameraPreference!!.isEnabled = false
                configuration.cameraEnabled = false
                // TODO ask for permissions again
                return
            }
        }*/

        cameraTestPreference!!.onPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
            //startCameraTest(preference.context)
            false
        }

        motionDetectionPreference!!.onPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
            view?.let { Navigation.findNavController(it).navigate(R.id.camera_action) }
            false
        }

        faceDetectionPreference!!.onPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
            // TODO navigate to preference fragment
            false
        }

        qrCodePreference!!.onPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
            // TODO navigate to preference fragment
            false
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            getString(R.string.key_setting_camera_processinginterval) -> {
                val value = processingIntervalPreference!!.text
                processingIntervalPreference!!.summary = value
            }
        }
    }
}