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

package org.wallpanelproject.android.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager

import org.wallpanelproject.android.R
import org.wallpanelproject.android.persistence.Configuration

import timber.log.Timber

class WelcomeActivity : AppCompatActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_welcome)

        if (supportActionBar != null) {
            supportActionBar!!.show()
            supportActionBar!!.title = getString(R.string.app_name)
        }

        Timber.d("First time: " + Configuration(this@WelcomeActivity).isFirstTime)

        if (!Configuration(this@WelcomeActivity).isFirstTime) {
            Timber.d("Starting Browser on Startup")
            startBrowserActivity()
        }

        findViewById<View>(R.id.welcomeSettingsButton).setOnClickListener {
            startActivity(Intent(this@WelcomeActivity, SettingsActivity::class.java))
            finish()
        }
    }

    // TODO move to base activity
    private fun startBrowserActivity() {
        Timber.i("startBrowserActivity Called")
        val browserType = Configuration(this.applicationContext).androidBrowserType
        val targetClass: Class<*>
        when (browserType) {
            "Native" -> {
                Timber.i( "Explicitly using native browser")
                targetClass = BrowserActivityNative::class.java
            }
            "Legacy" -> {
                Timber.i("Explicitly using legacy browser")
                targetClass = BrowserActivityLegacy::class.java
            }
            "Auto" -> {
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
        startActivity(Intent(applicationContext, targetClass))
    }
}
