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

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.preference.SwitchPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.thanksmister.iot.wallpanel.R
import com.thanksmister.iot.wallpanel.persistence.Configuration
import com.thanksmister.iot.wallpanel.ui.activities.SettingsActivity
import com.thanksmister.iot.wallpanel.utils.DialogUtils
import javax.inject.Inject

open class BaseSettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject
    lateinit var configuration: Configuration
    @Inject
    lateinit var dialogUtils: DialogUtils

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // Set title bar
        if ((activity as SettingsActivity).supportActionBar != null) {
            (activity as SettingsActivity).supportActionBar!!.title = (getString(R.string.title_settings))
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
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

    private val bindPreferenceSummaryToValueListener = Preference.OnPreferenceChangeListener { preference, value ->
        val stringValue = value.toString()
        if (preference is SwitchPreference) {
            return@OnPreferenceChangeListener true
        } else if (preference is ListPreference) {
            val index = preference.findIndexOfValue(stringValue)
            preference.setSummary(
                    if (index >= 0)
                        preference.entries[index]
                    else null)
        } else {
            if (preference.key.equals(getString(R.string.key_setting_mqtt_password))) {
                // mask password in settings list
                preference.summary = stringValue.replace(Regex("."), "*");
            } else {
                preference.summary = stringValue
            }
        }
        true
    }

    fun showSupport() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AboutFragment.SUPPORT_URL)))
        } catch (e: ActivityNotFoundException) {
            dialogUtils.showAlertDialog(requireContext(), getString(R.string.error_no_web_browser))
        }
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
    fun bindPreferenceSummaryToValue(preference: Preference) {
        preference.onPreferenceChangeListener = bindPreferenceSummaryToValueListener
        if (preference is SwitchPreference) {
            bindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getBoolean(preference.getKey(), false))
        } else {
            bindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.context)
                            .getString(preference.key, "")!!)
        }
    }


    companion object {
        const val SUPPORT_URL: String = "https://thanksmister.com/wallpanel-android/"
    }
}// Required empty public constructor
