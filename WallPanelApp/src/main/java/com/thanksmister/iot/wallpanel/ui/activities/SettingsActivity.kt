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

package com.thanksmister.iot.wallpanel.ui.activities


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import android.view.KeyEvent
import android.widget.Toast
import com.thanksmister.iot.wallpanel.R
import com.thanksmister.iot.wallpanel.network.WallPanelService
import com.thanksmister.iot.wallpanel.persistence.Configuration
import com.thanksmister.iot.wallpanel.ui.fragments.SettingsFragment
import com.thanksmister.iot.wallpanel.utils.DialogUtils
import com.thanksmister.iot.wallpanel.utils.ScreenUtils
import dagger.android.support.DaggerAppCompatActivity
import timber.log.Timber
import javax.inject.Inject

class SettingsActivity : DaggerAppCompatActivity(), SettingsFragment.OnSettingsFragmentListener {

    @Inject
    lateinit var configuration: Configuration
    @Inject
    lateinit var dialogUtils: DialogUtils

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
            }
        } else {
            configuration.cameraPermissionsShown = true
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
            }
        }
    }

    override fun onFinish() {
        this.finish()
    }

    override fun onBrowserButton() {
        val intent = Intent(this@SettingsActivity, BrowserActivityNative::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }

    companion object {
        const val PERMISSIONS_REQUEST_WRITE_SETTINGS = 200
        const val PERMISSIONS_REQUEST_CAMERA = 201
    }
}