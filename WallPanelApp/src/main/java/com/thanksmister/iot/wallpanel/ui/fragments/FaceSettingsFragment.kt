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
import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.SwitchPreference
import androidx.preference.EditTextPreference
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.navigation.Navigation
import com.thanksmister.iot.wallpanel.R
import com.thanksmister.iot.wallpanel.ui.activities.SettingsActivity
import dagger.android.support.AndroidSupportInjection

class FaceSettingsFragment : BaseSettingsFragment() {

    private var faceDetectionPreference: SwitchPreference? = null
    private var faceWakePreference: SwitchPreference? = null
    private var faceSizePreference: EditTextPreference? = null
    private var faceRotationPreference: SwitchPreference? = null

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
            (activity as SettingsActivity).supportActionBar!!.title = (getString(R.string.title_facedetection_settings))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_help, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            view?.let { Navigation.findNavController(it).navigate(R.id.camera_action) }
            return true
        } else if (id == R.id.action_help) {
            showSupport()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_face)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        faceDetectionPreference = findPreference<SwitchPreference>(getString(R.string.key_setting_camera_faceenabled)) as SwitchPreference
        faceWakePreference = findPreference<SwitchPreference>(getString(R.string.key_setting_camera_facewake)) as SwitchPreference
        faceSizePreference = findPreference<EditTextPreference>(getString(R.string.key_setting_camera_face_size)) as EditTextPreference
        faceRotationPreference = findPreference<SwitchPreference>(getString(R.string.key_setting_camera_facerotation)) as SwitchPreference

        bindPreferenceSummaryToValue(faceDetectionPreference!!)
        bindPreferenceSummaryToValue(faceWakePreference!!)
        bindPreferenceSummaryToValue(faceRotationPreference!!)

        val faceSize = configuration.cameraFaceSize
        faceSizePreference?.summary = getString(R.string.preference_summary_camera_facesize, faceSize.toString())
        faceSizePreference?.setDefaultValue(faceSize)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            getString(R.string.key_setting_camera_face_size)-> {
                faceSizePreference?.text?.let {
                    val faceSize = it.toIntOrNull()
                    if(faceSize != null && faceSize >= 0 && faceSize <= 100) {
                        faceSizePreference?.summary = getString(R.string.preference_summary_camera_facesize, faceSize.toString())
                        configuration.cameraFaceSize = faceSize
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.tost_error_face_size), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}