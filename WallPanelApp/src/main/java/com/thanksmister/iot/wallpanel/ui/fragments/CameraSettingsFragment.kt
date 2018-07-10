/*
 * <!--
 *   ~ Copyright (c) 2017. ThanksMister LLC
 *   ~
 *   ~ Licensed under the Apache License, Version 2.0 (the "License");
 *   ~ you may not use this file except in compliance with the License. 
 *   ~ You may obtain a copy of the License at
 *   ~
 *   ~ http://www.apache.org/licenses/LICENSE-2.0
 *   ~
 *   ~ Unless required by applicable law or agreed to in writing, software distributed 
 *   ~ under the License is distributed on an "AS IS" BASIS, 
 *   ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 *   ~ See the License for the specific language governing permissions and 
 *   ~ limitations under the License.
 *   -->
 */

package com.thanksmister.iot.mqtt.alarmpanel.ui.fragments

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.preference.CheckBoxPreference
import android.support.v7.preference.EditTextPreference
import android.support.v7.preference.ListPreference
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.text.TextUtils
import android.view.View
import android.widget.Toast

import com.thanksmister.iot.mqtt.alarmpanel.BaseActivity
import com.thanksmister.iot.mqtt.alarmpanel.R
import com.thanksmister.iot.mqtt.alarmpanel.ui.Configuration
import com.thanksmister.iot.mqtt.alarmpanel.ui.modules.CameraModule
import com.thanksmister.iot.mqtt.alarmpanel.utils.DialogUtils
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class CameraSettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject lateinit var configuration: Configuration
    @Inject lateinit var dialogUtils: DialogUtils

    private var tolPreference: EditTextPreference? = null
    private var fromPreference: EditTextPreference? = null
    private var domainPreference: EditTextPreference? = null
    private var keyPreference: EditTextPreference? = null
    private var activePreference: CheckBoxPreference? = null
    private var descriptionPreference: Preference? = null
    private var rotatePreference: ListPreference? = null
    private var telegramTokenPreference: EditTextPreference? = null
    private var telegramChatIdPreference: EditTextPreference? = null
    private var notesPreference: Preference? = null

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_camera)
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        telegramChatIdPreference = findPreference(Configuration.PREF_TELEGRAM_CHAT_ID) as EditTextPreference
        telegramTokenPreference = findPreference(Configuration.PREF_TELEGRAM_TOKEN) as EditTextPreference
        tolPreference = findPreference(Configuration.PREF_MAIL_TO) as EditTextPreference
        fromPreference = findPreference(Configuration.PREF_MAIL_FROM) as EditTextPreference
        domainPreference = findPreference(Configuration.PREF_MAIL_URL) as EditTextPreference
        keyPreference = findPreference(Configuration.PREF_MAIL_API_KEY) as EditTextPreference
        activePreference = findPreference(Configuration.PREF_MODULE_CAMERA) as CheckBoxPreference
        rotatePreference = findPreference(Configuration.PREF_CAMERA_ROTATE) as ListPreference
        descriptionPreference = findPreference("pref_mail_description")
        notesPreference = findPreference("pref_description")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (ActivityCompat.checkSelfPermission(activity as BaseActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                telegramChatIdPreference!!.isEnabled = false
                telegramTokenPreference!!.isEnabled = false
                tolPreference!!.isEnabled = false
                fromPreference!!.isEnabled = false
                domainPreference!!.isEnabled = false
                activePreference!!.isEnabled = false
                keyPreference!!.isEnabled = false
                rotatePreference!!.isEnabled = false
                descriptionPreference!!.isEnabled = false
                notesPreference!!.isEnabled = false
                configuration.setHasCamera(false)

                dialogUtils.showAlertDialog(activity as BaseActivity, getString(R.string.dialog_no_camera_permissions))
                return
            }
        }

        activePreference!!.isChecked = configuration.hasCamera()

        if (!TextUtils.isEmpty(configuration.getMailTo())) {
            tolPreference!!.text = configuration.getMailTo()
            tolPreference!!.summary = configuration.getMailTo()
        }

        if (!TextUtils.isEmpty(configuration.getMailFrom())) {
            fromPreference!!.text = configuration.getMailFrom()
            fromPreference!!.summary = configuration.getMailFrom()
        }

        if (!TextUtils.isEmpty(configuration.getMailGunUrl())) {
            domainPreference!!.text = configuration.getMailGunUrl()
            domainPreference!!.summary = configuration.getMailGunUrl()
        }

        if (!TextUtils.isEmpty(configuration.getMailGunApiKey())) {
            keyPreference!!.text = configuration.getMailGunApiKey()
            keyPreference!!.summary = configuration.getMailGunApiKey()
        }

        if (!TextUtils.isEmpty(configuration.telegramChatId)) {
            telegramChatIdPreference!!.text = configuration.telegramChatId
            telegramChatIdPreference!!.summary = configuration.telegramChatId
        }

        if (!TextUtils.isEmpty(configuration.telegramToken)) {
            telegramTokenPreference!!.text = configuration.telegramToken
            telegramTokenPreference!!.summary = configuration.telegramToken
        }

        rotatePreference!!.setDefaultValue(configuration.getCameraRotate())
        rotatePreference!!.value = configuration.getCameraRotate().toString()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        val value: String
        when (key) {
            Configuration.PREF_MAIL_TO -> {
                value = tolPreference!!.text
                configuration.setMailTo(value)
                tolPreference!!.summary = value
            }
            Configuration.PREF_MAIL_FROM -> {
                value = fromPreference!!.text
                configuration.setMailFrom(value)
                fromPreference!!.summary = value
            }
            Configuration.PREF_MAIL_URL -> {
                value = domainPreference!!.text
                configuration.setMailGunUrl(value)
                domainPreference!!.summary = value
            }
            Configuration.PREF_MAIL_API_KEY -> {
                value = keyPreference!!.text
                configuration.setMailGunApiKey(value)
                keyPreference!!.summary = value
            }
            Configuration.PREF_TELEGRAM_CHAT_ID -> {
                value = telegramChatIdPreference!!.text
                configuration.telegramChatId = value
                telegramChatIdPreference!!.summary = value
            }
            Configuration.PREF_TELEGRAM_TOKEN -> {
                value = telegramTokenPreference!!.text
                configuration.telegramToken = value
                telegramTokenPreference!!.summary = value
            }
            Configuration.PREF_MODULE_CAMERA -> {
                val checked = activePreference!!.isChecked
                configuration.setHasCamera(checked)
            }
            Configuration.PREF_CAMERA_ROTATE -> {
                val valueFloat = rotatePreference!!.value
                val valueName = rotatePreference!!.entry.toString()
                rotatePreference!!.summary = getString(R.string.preference_camera_flip_summary, valueName)
                configuration.setCameraRotate(valueFloat)
            }
        }
    }
}