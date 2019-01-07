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

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import com.thanksmister.iot.wallpanel.R
import com.thanksmister.iot.wallpanel.ui.activities.SettingsActivity

import kotlinx.android.synthetic.main.fragment_about.*
import timber.log.Timber

class AboutFragment : Fragment() {

    private var versionNumber: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // Set title bar
        if((activity as SettingsActivity).supportActionBar != null) {
            (activity as SettingsActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            (activity as SettingsActivity).supportActionBar!!.setDisplayShowHomeEnabled(true)
            (activity as SettingsActivity).supportActionBar!!.title = (getString(R.string.pref_about_title))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            view?.let { Navigation.findNavController(it).navigate(R.id.settings_action) }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            val packageInfo = activity!!.packageManager.getPackageInfo(activity!!.packageName, 0)
            versionNumber = " v" + packageInfo.versionName
            versionName.text = versionNumber
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.e(e.message)
        }

        sendFeedbackButton.setOnClickListener { feedback() }
        rateApplicationButton.setOnClickListener { rate() }
        githubButton.setOnClickListener { showGitHub() }
        supportButton.setOnClickListener { showSupport() }
    }

    private fun rate() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + GOOGLE_PLAY_RATING)))
        } catch (ex: android.content.ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + GOOGLE_PLAY_RATING)))
        }
    }

    private fun showSupport() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SUPPORT_URL)))
    }

    private fun showGitHub() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL)))
    }

    private fun feedback() {
        val Email = Intent(Intent.ACTION_SENDTO)
        Email.type = "text/email"
        Email.data = Uri.parse("mailto:" + EMAIL_ADDRESS)
        Email.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.mail_to_subject_text) + " " + versionNumber)
        startActivity(Intent.createChooser(Email, getString(R.string.mail_subject_text)))
    }

    companion object {
        const val SUPPORT_URL:String = "https://thanksmister.com/wallpanel-android/"
        const val GOOGLE_PLAY_RATING = "com.thanksmister.iot.wallpanel"
        const val GITHUB_URL = "https://github.com/thanksmister/wallpanel-android"
        const val EMAIL_ADDRESS = "mister@thanksmister.com"

        fun newInstance(): AboutFragment {
            return AboutFragment()
        }
    }
}