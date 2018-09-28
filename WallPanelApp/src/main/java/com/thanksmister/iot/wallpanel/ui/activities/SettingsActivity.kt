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


import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.content.ContextCompat
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AlertDialog
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.thanksmister.iot.wallpanel.R
import com.thanksmister.iot.wallpanel.network.WallPanelService

import com.thanksmister.iot.wallpanel.persistence.Configuration
import com.thanksmister.iot.wallpanel.persistence.Configuration.Companion.PREF_BROWSER_AUTO
import com.thanksmister.iot.wallpanel.persistence.Configuration.Companion.PREF_BROWSER_LEGACY
import com.thanksmister.iot.wallpanel.persistence.Configuration.Companion.PREF_BROWSER_NATIVE
import com.thanksmister.iot.wallpanel.ui.fragments.CameraSettingsFragment
import com.thanksmister.iot.wallpanel.ui.fragments.SettingsFragment
import com.thanksmister.iot.wallpanel.utils.DialogUtils
import dagger.android.support.DaggerAppCompatActivity
import kotlinx.android.synthetic.main.activity_settings.*

import timber.log.Timber
import javax.inject.Inject

class SettingsActivity : DaggerAppCompatActivity(), SettingsFragment.OnSettingsFragmentListener {

    @Inject lateinit var configuration: Configuration
    @Inject lateinit var dialogUtils: DialogUtils

    public override fun onCreate(savedInstance: Bundle?) {

        super.onCreate(savedInstance)

        setContentView(R.layout.activity_settings)

        // Stop our service for performance reasons and to pick up changes
        val wallPanelService = Intent(this, WallPanelService::class.java)
        stopService(wallPanelService)

        lifecycle.addObserver(dialogUtils)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        Timber.d("onKeyDown")
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            this.finish()
            return true;
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == PERMISSIONS_REQUEST_WRITE_SETTINGS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.System.canWrite(applicationContext)) {
                    Toast.makeText(this, getString(R.string.toast_write_permissions_granted), Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, getString(R.string.toast_write_permissions_denied), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    public override fun onResume() {
        super.onResume()
        requestCameraPermissions()
    }

    private fun requestCameraPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !configuration.cameraPermissionsShown) {
            if (PackageManager.PERMISSION_DENIED == ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                    || PackageManager.PERMISSION_DENIED == ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                configuration.cameraPermissionsShown = true
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE),
                        PERMISSIONS_REQUEST_CAMERA)
            } else {
                checkWriteSettings()
            }
        } else {
            configuration.cameraPermissionsShown = true
            checkWriteSettings()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_CAMERA -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, R.string.toast_camera_permission_granted, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, R.string.toast_camera_permission_denied, Toast.LENGTH_LONG).show()
                }
                checkWriteSettings() // now check if we have write settings
            }
        }
    }

    private fun checkWriteSettings() {
        if (!configuration.writeScreenPermissionsShown && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(Settings.System.canWrite(applicationContext)) {
            } else if (!configuration.writeScreenPermissionsShown) {
                // launch the dialog to provide permissions
                configuration.writeScreenPermissionsShown = true
                AlertDialog.Builder(this@SettingsActivity)
                        .setMessage(getString(R.string.dialog_write_permissions_description))
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            launchWriteSettings()
                        }
                        .setNegativeButton(android.R.string.cancel){ _, _ ->
                            Toast.makeText(this, getString(R.string.toast_write_permissions_denied), Toast.LENGTH_LONG).show()
                        }.show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun launchWriteSettings() {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName"))
        startActivityForResult(intent, 200)
    }

    override fun onFinish() {
        this.finish()
    }

    override fun onBrowserButton() {
        val browserType = configuration.androidBrowserType
        val targetClass: Class<*>
        when (browserType) {
            Configuration.PREF_BROWSER_NATIVE -> {
                Timber.d("Explicitly using native browser")
                targetClass = BrowserActivityNative::class.java
            }
            Configuration.PREF_BROWSER_LEGACY -> {
                Timber.d("Explicitly using legacy browser")
                targetClass = BrowserActivityLegacy::class.java
            }
            Configuration.PREF_BROWSER_AUTO -> {
                Timber.d("Auto-selecting dashboard browser")
                targetClass = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    BrowserActivityNative::class.java
                else
                    BrowserActivityLegacy::class.java
            }
            else -> {
                Timber.d("Auto-selecting dashboard browser")
                targetClass = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    BrowserActivityNative::class.java
                else
                    BrowserActivityLegacy::class.java
            }
        }

        val intent = Intent(this@SettingsActivity, targetClass)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }

    companion object {
        const val PERMISSIONS_REQUEST_WRITE_SETTINGS = 200
        const val PERMISSIONS_REQUEST_CAMERA = 201
    }
}
