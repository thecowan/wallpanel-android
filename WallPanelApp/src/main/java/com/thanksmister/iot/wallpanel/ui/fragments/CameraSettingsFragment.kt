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

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v14.preference.SwitchPreference
import android.support.v4.app.ActivityCompat
import android.support.v7.preference.EditTextPreference
import android.support.v7.preference.ListPreference
import android.support.v7.preference.Preference
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.navigation.Navigation
import com.thanksmister.iot.wallpanel.R
import com.thanksmister.iot.wallpanel.ui.activities.LiveCameraActivity
import com.thanksmister.iot.wallpanel.ui.activities.SettingsActivity
import com.thanksmister.iot.wallpanel.utils.CameraUtils
import dagger.android.support.AndroidSupportInjection
import timber.log.Timber

class CameraSettingsFragment : BaseSettingsFragment() {

    private var cameraListPreference: ListPreference? = null
    private var cameraTestPreference: Preference? = null
    private var qrCodePreference: Preference? = null
    private var motionDetectionPreference: Preference? = null
    private var cameraPreference: SwitchPreference? = null
    private var faceDetectionPreference: Preference? = null
    private var motionBrightPreference: SwitchPreference? = null
    private var motionDimPreference: EditTextPreference? = null
    private var fpsPreference: EditTextPreference? = null

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
        setHasOptionsMenu(true)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // Set title bar
        if((activity as SettingsActivity).supportActionBar != null) {
            (activity as SettingsActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            (activity as SettingsActivity).supportActionBar!!.setDisplayShowHomeEnabled(true)
            (activity as SettingsActivity).supportActionBar!!.title = (getString(R.string.title_camera_settings))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater!!.inflate(R.menu.menu_help, menu)
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
        addPreferencesFromResource(R.xml.pref_camera)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        motionBrightPreference = findPreference(getString(R.string.key_setting_camera_motionbright)) as SwitchPreference
        motionDimPreference = findPreference(getString(R.string.key_setting_camera_motionontime)) as EditTextPreference
        fpsPreference = findPreference(getString(R.string.key_setting_camera_fps)) as EditTextPreference
        cameraPreference = findPreference(getString(R.string.key_setting_camera_enabled)) as SwitchPreference

        cameraListPreference = findPreference(getString(R.string.key_setting_camera_cameraid)) as ListPreference
        cameraListPreference!!.setOnPreferenceChangeListener { preference, newValue ->
            if (preference is ListPreference) {
                val index = preference.findIndexOfValue(newValue.toString())
                preference.setSummary(
                        if (index >= 0)
                            preference.entries[index]
                        else
                            "")
                if(index >= 0) {
                    //configuration.cameraId = index
                    Timber.d("Camera Id: " + configuration.cameraId)
                }
            }
            true;
        }
        cameraListPreference!!.isEnabled = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (ActivityCompat.checkSelfPermission(activity!!.applicationContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                createCameraList()
            }
        } else {
            createCameraList()
        }

        bindPreferenceSummaryToValue(cameraPreference!!)
        bindPreferenceSummaryToValue(cameraListPreference!!);
        bindPreferenceSummaryToValue(motionBrightPreference!!)
        bindPreferenceSummaryToValue(motionDimPreference!!)
        bindPreferenceSummaryToValue(fpsPreference!!)

        motionDetectionPreference = findPreference("button_key_motion_detection")
        faceDetectionPreference = findPreference("button_key_face_detection")
        qrCodePreference = findPreference("button_key_qr_code")
        cameraTestPreference = findPreference("button_key_camera_test")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (ActivityCompat.checkSelfPermission(activity!!.applicationContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                configuration.cameraEnabled = false
                dialogUtils.showAlertDialog(activity!!.applicationContext, getString(R.string.dialog_no_camera_permissions))
                return
            }
        }

        cameraTestPreference!!.onPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
            startCameraTest(preference.context)
            false
        }

        motionDetectionPreference!!.onPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
            view.let { Navigation.findNavController(it).navigate(R.id.motion_action) }
            false
        }

        faceDetectionPreference!!.onPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
            view.let { Navigation.findNavController(it).navigate(R.id.face_action) }
            false
        }

        qrCodePreference!!.onPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
            view.let { Navigation.findNavController(it).navigate(R.id.qrcode_action) }
            false
        }
    }

    private fun createCameraList() {
        Timber.d("createCameraList")
        try {
            val cameraList = CameraUtils.getCameraList(activity!!)
            cameraListPreference!!.entries = cameraList.toTypedArray<CharSequence>()
            val vals = arrayOfNulls<CharSequence>(cameraList.size)
            for (i in cameraList.indices) {
                vals[i] = Integer.toString(i)
            }
            cameraListPreference?.entryValues = vals
            val index = cameraListPreference!!.findIndexOfValue(configuration.cameraId.toString())
            cameraListPreference!!.summary = if (index >= 0)
                cameraListPreference!!.entries[index]
            else
                ""
            cameraListPreference!!.isEnabled = true
        } catch (e: Exception) {
            Timber.e(e.message)
            cameraListPreference!!.isEnabled = false
            if(activity != null) {
                Toast.makeText(activity!!, getString(R.string.toast_camera_source_error), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startCameraTest(c: Context) {
        startActivity(Intent(c, LiveCameraActivity::class.java))
    }
}