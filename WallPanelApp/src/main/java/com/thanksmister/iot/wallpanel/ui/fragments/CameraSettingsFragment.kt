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

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.preference.SwitchPreference
import androidx.core.app.ActivityCompat
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.navigation.Navigation
import com.thanksmister.iot.wallpanel.R
import com.thanksmister.iot.wallpanel.persistence.Configuration
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
    private var fpsPreference: EditTextPreference? = null
    private var cameraStreaming: Preference? = null
    private var rotatePreference: ListPreference? = null

    var cameraList = ArrayList<CameraUtils.Companion.CameraList>()

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
        setHasOptionsMenu(true)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // Set title bar
        if (activity is SettingsActivity) {
            val actionBar = (activity as SettingsActivity).supportActionBar
            with(actionBar) {
                this?.setDisplayHomeAsUpEnabled(true)
                this?.setDisplayShowHomeEnabled(true)
                this?.title = (getString(R.string.title_camera_settings))
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_help, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                view?.let { Navigation.findNavController(it).navigate(R.id.settings_action) }
                return true
            }
            R.id.action_help -> {
                showSupport()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_camera)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        fpsPreference = findPreference<EditTextPreference>(getString(R.string.key_setting_camera_fps)) as EditTextPreference
        cameraPreference = findPreference<SwitchPreference>(getString(R.string.key_setting_camera_enabled)) as SwitchPreference

        rotatePreference = findPreference<ListPreference>(Configuration.PREF_CAMERA_ROTATE) as ListPreference
        rotatePreference!!.setDefaultValue(configuration.cameraRotate)
        rotatePreference!!.value = configuration.cameraRotate.toString()
        if(configuration.cameraRotate == 0f) {
            rotatePreference!!.setValueIndex(0)
        } else if (configuration.cameraRotate == -90f) {
            rotatePreference!!.setValueIndex(1)
        } else if (configuration.cameraRotate == 90f) {
            rotatePreference!!.setValueIndex(2)
        } else if (configuration.cameraRotate == -180f) {
            rotatePreference!!.setValueIndex(3)
        }
        rotatePreference?.setOnPreferenceChangeListener { preference, newValue ->
            if (preference is ListPreference) {
                val valueFloat = rotatePreference!!.value
                val valueName = rotatePreference!!.entry.toString()
                rotatePreference!!.summary = getString(R.string.preference_camera_flip_summary, valueName)
                configuration.cameraRotate = valueFloat.toFloat()
            }
            true
        }

        cameraListPreference = findPreference<ListPreference>(getString(R.string.key_setting_camera_cameraid)) as ListPreference
        cameraListPreference?.setOnPreferenceChangeListener { preference, newValue ->
            if (preference is ListPreference) {
                val index = preference.findIndexOfValue(newValue.toString())
                preference.setSummary(if (index >= 0) preference.entries[index] else "")
                Timber.d("Camera index: " + index)
                if(index >= 0) {
                    val cameraListItem = cameraList[index]
                    configuration.cameraId = cameraListItem.cameraId
                    Timber.d("Camera Id: " + configuration.cameraId)
                }
            }
            true
        }
        cameraListPreference?.isEnabled = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (ActivityCompat.checkSelfPermission(requireActivity().applicationContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                createCameraList()
            }
        } else {
            createCameraList()
        }

        bindPreferenceSummaryToValue(cameraPreference!!)
        bindPreferenceSummaryToValue(fpsPreference!!)

        motionDetectionPreference = findPreference("button_key_motion_detection")
        faceDetectionPreference = findPreference("button_key_face_detection")
        qrCodePreference = findPreference("button_key_qr_code")
        cameraTestPreference = findPreference("button_key_camera_test")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && activity != null) {
            if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                configuration.cameraEnabled = false
                if(activity != null) {
                    dialogUtils.showAlertDialog(requireActivity(), getString(R.string.dialog_no_camera_permissions))
                }
                return
            }
        }

        cameraTestPreference?.onPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
            startCameraTest(preference.context)
            false
        }

        motionDetectionPreference?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            view.let { Navigation.findNavController(it).navigate(R.id.motion_action) }
            false
        }

        faceDetectionPreference?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            view.let { Navigation.findNavController(it).navigate(R.id.face_action) }
            false
        }

        qrCodePreference?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            view.let { Navigation.findNavController(it).navigate(R.id.qrcode_action) }
            false
        }

        cameraStreaming = findPreference("button_key_camera_streaming")

        cameraStreaming?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            view.let { Navigation.findNavController(it).navigate(R.id.action_camera_fragment_to_http_fragment) }
            false
        }
    }

    private fun createCameraList() {
        Timber.d("createCameraList")
        try {
            cameraList = CameraUtils.getCameraList(requireActivity())
            val cameraListEntries:ArrayList<CharSequence> = ArrayList()
            val cameraListValues:ArrayList<CharSequence> = ArrayList()
            for (item in cameraList) {
                cameraListEntries.add(item.description)
                cameraListValues.add(Integer.toString(item.cameraId))
            }
            cameraListPreference!!.entries = cameraListEntries.toTypedArray<CharSequence>()
            cameraListPreference!!.entryValues = cameraListValues.toTypedArray<CharSequence>()
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
                Toast.makeText(requireActivity(), getString(R.string.toast_camera_source_error), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startCameraTest(c: Context) {
        startActivity(Intent(c, LiveCameraActivity::class.java))
    }
}