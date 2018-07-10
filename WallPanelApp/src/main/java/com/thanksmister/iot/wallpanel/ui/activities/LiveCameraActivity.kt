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

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import android.view.WindowManager
import com.thanksmister.iot.wallpanel.R
import com.thanksmister.iot.wallpanel.controls.CameraCallback
import com.thanksmister.iot.wallpanel.persistence.Configuration
import com.thanksmister.iot.wallpanel.ui.DetectionViewModel
import com.thanksmister.iot.wallpanel.ui.views.CameraSourcePreview
import dagger.android.support.DaggerAppCompatActivity
import kotlinx.android.synthetic.main.activity_cameratest.*
import timber.log.Timber
import javax.inject.Inject

class LiveCameraActivity : DaggerAppCompatActivity() {

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: DetectionViewModel
    @Inject lateinit var configuration: Configuration
    private var updateHandler: Handler? = null
    private var removeTextCountdown: Int = 0
    private val interval = 1000/15L
    private var preview: CameraSourcePreview? = null

    private val updatePicture = object : Runnable {
        override fun run() {
            if (removeTextCountdown > 0) {
                removeTextCountdown--
                if (removeTextCountdown == 0) {
                    setStatusText("")
                }
            }
            updateHandler!!.postDelayed(this, interval)
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_cameratest)

        if (supportActionBar != null) {
            supportActionBar!!.show()
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
            supportActionBar!!.title = "Live Camera Test"
        }

        window.setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
        preview = findViewById<CameraSourcePreview>(R.id.imageView_preview)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(DetectionViewModel::class.java)
        viewModel.startCamera(cameraCallback, preview!!)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            //viewModel.stopCamera()
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        //viewModel.stopCamera()
    }

    public override fun onStart() {
        super.onStart()
        startUpdatePicture()
    }

    public override fun onStop() {
        super.onStop()
        stopUpdatePicture()
    }

    public override fun onResume() {
        super.onResume()
    }

    public override fun onPause() {
        super.onPause()
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

    private val cameraCallback = object : CameraCallback {
        override fun onMotionDetected() {
            runOnUiThread {
                if(removeTextCountdown == 0) {
                    setStatusText("Motion Detected!")
                    removeTextCountdown = 10
                }
            }
        }

        override fun onTooDark() {
            runOnUiThread {
                if(removeTextCountdown == 0) {
                    setStatusText("Too dark for motion detection")
                    removeTextCountdown = 10
                }
            }
        }

        override fun onFaceDetected() {
            runOnUiThread {
                if(removeTextCountdown == 0) {
                    setStatusText("Face Detected!")
                    removeTextCountdown = 10
                }
            }
        }

        override fun onQRCode(data: String) {
            runOnUiThread {
                if(removeTextCountdown == 0) {
                    setStatusText("QR Code: $data")
                    removeTextCountdown = 10
                }
            }
        }
    }

    private fun setStatusText(text: String) {
        Timber.d("statusTextView: $text")
        statusTextView.text = text
    }

    companion object {

    }
}