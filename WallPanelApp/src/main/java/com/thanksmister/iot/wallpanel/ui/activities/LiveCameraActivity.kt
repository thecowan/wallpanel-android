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
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView

import com.thanksmister.iot.wallpanel.R
import com.thanksmister.iot.wallpanel.controls.CameraDetectorCallback
import com.thanksmister.iot.wallpanel.controls.CameraReader
import com.thanksmister.iot.wallpanel.persistence.Configuration

import javax.inject.Inject

import dagger.android.support.DaggerAppCompatActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class LiveCameraActivity : DaggerAppCompatActivity() {

    @Inject lateinit var configuration: Configuration
    private var updateHandler: Handler? = null
    private var removeTextCountdown: Int = 0
    private var cameraReader: CameraReader? = null
    private val interval = 1000/15

    private val updatePicture = object : Runnable {
        override fun run() {
            if (removeTextCountdown > 0) {
                removeTextCountdown--
                if (removeTextCountdown == 0) {
                    setStatusText("")
                }
            }
            updateHandler!!.postDelayed(this, interval.toLong())
        }
    }

    private val cameraDetectorCallback = object : CameraDetectorCallback {
        override fun onMotionDetected() {
            Timber.i("Motion detected")
            setStatusText("Motion Detected!")
            removeTextCountdown = 10
        }

        override fun onTooDark() {
            Timber.i("Too dark")
            val intent = Intent(BROADCAST_CAMERA_TEST_MSG)
            intent.putExtra("message", "Too dark for motion detection")
            setStatusText("Too dark for motion detection")
            removeTextCountdown = 10
        }

        override fun onFaceDetected() {
            Timber.i("Face detected")
            setStatusText("Face Detected!")
            removeTextCountdown = 10
        }

        override fun onQRCode(data: String) {
            Timber.i("QR Code Received")
            setStatusText("QR Code: $data")
            removeTextCountdown = 10
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_cameratest)

        if (supportActionBar != null) {
            supportActionBar!!.show()
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
            supportActionBar!!.title = "Camera Test"
        }

        window.setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)

        cameraReader = CameraReader(this.applicationContext)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    public override fun onStart() {
        super.onStart()
        startCamera()
        startUpdatePicture()
    }

    public override fun onStop() {
        super.onStop()
        stopUpdatePicture()
        stopCamera()
    }

    public override fun onResume() {
        super.onResume()
    }

    public override fun onPause() {
        super.onPause()
    }

    private fun startCamera() {
        if (configuration.cameraEnabled) {
            cameraReader!!.start(configuration.cameraId, configuration.cameraProcessingInterval, this.cameraDetectorCallback)
            if (configuration.cameraMotionEnabled) {
                Timber.d( "Camera Motion detection is enabled")
                cameraReader!!.startMotionDetection(configuration.cameraMotionMinLuma,
                        configuration.cameraMotionLeniency)
            }
            if (configuration.cameraFaceEnabled) {
                Timber.d( "Camera Face detection is enabled")
                cameraReader!!.startFaceDetection()
            }
            if (configuration.cameraQRCodeEnabled) {
                Timber.d( "Camera QR Code detection is enabled")
                cameraReader!!.startQRCodeDetection()
            }
        }
    }

    private fun stopCamera() {
        if (cameraReader != null) {
            cameraReader!!.stop()
        }
    }

    private fun startUpdatePicture() {
        updateHandler = Handler()
        updateHandler!!.postDelayed(updatePicture, interval.toLong())
    }

    private fun stopUpdatePicture() {
        if (updateHandler != null) {
            updateHandler!!.removeCallbacks(updatePicture)
            updateHandler = null
        }
    }

    private fun setStatusText(text: String) {
        val status = findViewById<TextView>(R.id.textView_status) as TextView
        status.text = text
    }

    companion object {
        val BROADCAST_CAMERA_TEST_MSG = "BROADCAST_CAMERA_TEST_MSG"
    }
}