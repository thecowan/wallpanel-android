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
import android.content.Context.WIFI_SERVICE
import android.net.wifi.WifiManager
import android.os.Bundle
import androidx.preference.SwitchPreference
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import android.text.format.Formatter
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.navigation.Navigation
import com.thanksmister.iot.wallpanel.R
import com.thanksmister.iot.wallpanel.ui.activities.SettingsActivity
import dagger.android.support.AndroidSupportInjection


class HttpSettingsFragment : BaseSettingsFragment() {

    private var httpRestPreference: SwitchPreference? = null
    private var httpMjpegPreference: SwitchPreference? = null
    private var httpMjpegStreamsPreference: EditTextPreference? = null
    private var httpPortPreference: EditTextPreference? = null


    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
        setHasOptionsMenu(true)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (activity is SettingsActivity) {
            val actionBar = (activity as SettingsActivity).supportActionBar
            with(actionBar) {
                this?.setDisplayHomeAsUpEnabled(true)
                this?.setDisplayShowHomeEnabled(true)
                this?.title = (getString(R.string.title_http_settings))
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
        addPreferencesFromResource(R.xml.pref_http)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        httpRestPreference = findPreference<SwitchPreference>(getString(R.string.key_setting_http_restenabled)) as SwitchPreference
        httpMjpegPreference = findPreference<SwitchPreference>(getString(R.string.key_setting_http_mjpegenabled)) as SwitchPreference
        httpMjpegStreamsPreference = findPreference<EditTextPreference>(getString(R.string.key_setting_http_mjpegmaxstreams)) as EditTextPreference
        httpPortPreference = findPreference<EditTextPreference>(getString(R.string.key_setting_http_port)) as EditTextPreference

        bindPreferenceSummaryToValue(httpRestPreference!!)
        bindPreferenceSummaryToValue(httpMjpegPreference!!)
        bindPreferenceSummaryToValue(httpMjpegStreamsPreference!!)
        bindPreferenceSummaryToValue(httpPortPreference!!)

        val wm = requireActivity().applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        val ip = Formatter.formatIpAddress(wm.connectionInfo.ipAddress)

        val description = findPreference<Preference>(getString(R.string.key_setting_directions)) as Preference
        description.summary = getString(R.string.pref_mjpeg_streaming_description, ip )
    }
}