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

package com.thanksmister.iot.wallpanel.ui


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.thanksmister.iot.wallpanel.R

import com.thanksmister.iot.wallpanel.persistence.Configuration
import dagger.android.support.DaggerAppCompatActivity

import timber.log.Timber
import javax.inject.Inject

class SettingsActivity : DaggerAppCompatActivity() {

    @Inject
    lateinit var configuration: Configuration

    private var systemSettingsPermissionAsked: Boolean = false

    public override fun onCreate(savedInstance: Bundle?) {

        super.onCreate(savedInstance)

        if (supportActionBar != null) {
            supportActionBar!!.title = "Settings"
        }

        if (savedInstance == null) {
            supportFragmentManager.beginTransaction().add(android.R.id.content, SettingsFragment()).commit()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        Timber.d("onActivityResult requestCode: $requestCode")
        Timber.d("onActivityResult resultCode: $resultCode")
        if (requestCode == PERMISSIONS_REQUEST_WRITE_SETTINGS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.System.canWrite(applicationContext)) {
                    Toast.makeText(this, "Write settings permission granted...", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Write settings permission denied...", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    public override fun onResume() {
        super.onResume()
        Timber.d("onResume")
        requestCameraPermissions()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_welcome, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_settings) {
            startBrowserActivity()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun requestCameraPermissions() {
        Timber.d("requestCameraPermissions")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Timber.d("requestCameraPermissions asking")
            if (PackageManager.PERMISSION_DENIED == ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)) {
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.CAMERA),
                        PERMISSIONS_REQUEST_CAMERA)
            } else {
                checkWriteSettings()
            }
        } else {
            checkWriteSettings()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_CAMERA -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, R.string.toast_camera_permission_granted, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, R.string.toast_camera_permission_denied, Toast.LENGTH_LONG).show()
                }

                checkWriteSettings() // now check if we have write settings
            }
        }
    }

    private fun checkWriteSettings() {
        Timber.d("checkWriteSettings")
        if(systemSettingsPermissionAsked) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(Settings.System.canWrite(applicationContext)) {
                // na-da
            } else {
                // launch the dialog to provide permissions
                AlertDialog.Builder(this@SettingsActivity)
                        .setTitle("Permissions Required")
                        .setMessage("Do you want to grant permission to modify the system settings for screen brightness?")
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName"))
                            startActivityForResult(intent, 200)
                            systemSettingsPermissionAsked = true
                        }
                        .setNegativeButton(android.R.string.cancel){ _, _ ->
                            Toast.makeText(this, "Write settings permission denied...", Toast.LENGTH_LONG).show()
                            systemSettingsPermissionAsked = true
                        }.show()
            }
        }
    }

    private fun startBrowserActivity() {
        Timber.d("startBrowserActivity")
        val browserType = configuration.androidBrowserType
        val targetClass: Class<*>
        when (browserType) {
            "Native" -> {
                Timber.d("Explicitly using native browser")
                targetClass = BrowserActivityNative::class.java
            }
            "Legacy" -> {
                Timber.d("Explicitly using legacy browser")
                targetClass = BrowserActivityLegacy::class.java
            }
            "Auto" -> {
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
        startActivity(Intent(applicationContext, targetClass))
    }

    companion object {
        const val PERMISSIONS_REQUEST_WRITE_SETTINGS = 200
        const val PERMISSIONS_REQUEST_CAMERA = 201
    }

}
