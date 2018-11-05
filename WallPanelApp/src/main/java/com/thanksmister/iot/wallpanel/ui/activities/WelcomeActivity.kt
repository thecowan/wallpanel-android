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

package com.thanksmister.iot.wallpanel.ui.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import com.thanksmister.iot.wallpanel.R

import com.thanksmister.iot.wallpanel.persistence.Configuration
import dagger.android.support.DaggerAppCompatActivity

import timber.log.Timber
import javax.inject.Inject

class WelcomeActivity : DaggerAppCompatActivity() {

    @Inject
    lateinit var configuration: Configuration

    public override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_welcome)

        if (supportActionBar != null) {
            supportActionBar!!.show()
            supportActionBar!!.title = getString(R.string.app_name)
        }

        findViewById<View>(R.id.welcomeSettingsButton).setOnClickListener {
            configuration.setFirstTime(false)
            startActivity(Intent(this@WelcomeActivity, SettingsActivity::class.java))
            finish()
        }

        if (!configuration.isFirstTime) {
            startBrowserActivity()
        }
    }

    private fun startBrowserActivity() {
        val browserType = configuration.androidBrowserType
        Timber.i("startBrowserActivity browserType $browserType")
        val targetClass: Class<*>
        when (browserType) {
            Configuration.PREF_BROWSER_NATIVE -> {
                Timber.i( "Explicitly using native browser")
                targetClass = BrowserActivityNative::class.java
            }
            Configuration.PREF_BROWSER_LEGACY  -> {
                Timber.i("Explicitly using legacy browser")
                targetClass = BrowserActivityLegacy::class.java
            }
            Configuration.PREF_BROWSER_AUTO -> {
                Timber.i("Auto-selecting dashboard browser")
                targetClass = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    BrowserActivityNative::class.java
                else
                    BrowserActivityLegacy::class.java
            }
            else -> {
                Timber.i("Auto-selecting dashboard browser")
                targetClass = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    BrowserActivityNative::class.java
                else
                    BrowserActivityLegacy::class.java
            }
        }
        val intent = Intent(this@WelcomeActivity, targetClass)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }
}