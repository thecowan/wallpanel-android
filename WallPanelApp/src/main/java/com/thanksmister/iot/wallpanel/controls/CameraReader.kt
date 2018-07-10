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

package com.thanksmister.iot.wallpanel.controls

import android.annotation.SuppressLint
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.graphics.*
import android.hardware.Camera
import android.opengl.GLES11Ext
import android.os.AsyncTask
import com.google.android.gms.vision.*
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor
import com.thanksmister.iot.wallpanel.persistence.Configuration
import com.thanksmister.iot.wallpanel.ui.views.CameraSourcePreview
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject

class CameraReader @Inject
constructor(private val context: Context) {

    private var cameraCallback: CameraCallback? = null
    private var faceDetector: FaceDetector? = null
    private var barcodeDetector: BarcodeDetector? = null
    private var motionDetector: MotionDetector? = null
    private var multiDetector: MultiDetector? = null
    private var cameraSource: CameraSource? = null
    private var faceDetectorProcessor: LargestFaceFocusingProcessor? = null
    private var barCodeDetectorProcessor: MultiProcessor<Barcode>? = null
    private var motionDetectorProcessor: MultiProcessor<Motion>? = null
    private val byteArray = MutableLiveData<ByteArray>()
    private var bitmapComplete = false;
    private var bitmapCreateTask: BitmapTask? = null

    fun getJpeg(): LiveData<ByteArray> {
        return byteArray
    }

    private fun setJpeg(value: ByteArray) {
        this.byteArray.value = value
    }

    fun stopCamera() {

        if(bitmapCreateTask != null) {
            bitmapCreateTask!!.cancel(true)
            bitmapCreateTask = null
        }

        if(cameraSource != null) {
            cameraSource!!.release()
            cameraSource = null
        }

        if(faceDetector != null) {
            faceDetector!!.release()
            faceDetector = null
        }

        if(barcodeDetector != null) {
            barcodeDetector!!.release()
            barcodeDetector = null
        }

        if(motionDetector != null) {
            motionDetector!!.release()
            motionDetector = null
        }

        if(multiDetector != null) {
            multiDetector!!.release()
            multiDetector = null
        }

        if(faceDetectorProcessor != null) {
            faceDetectorProcessor!!.release()
            faceDetectorProcessor = null
        }

        if(barCodeDetectorProcessor != null) {
            barCodeDetectorProcessor!!.release()
            barCodeDetectorProcessor = null
        }

        if(motionDetectorProcessor != null) {
            motionDetectorProcessor!!.release()
            motionDetectorProcessor = null
        }
    }

    fun startCamera(callback: CameraCallback, configuration: Configuration) {
        startCamera(callback, configuration, null)
    }

    @SuppressLint("MissingPermission")
    fun startCamera(callback: CameraCallback, configuration: Configuration, preview: CameraSourcePreview?) {

        Timber.d("startCamera")

        this.cameraCallback = callback

        if (configuration.cameraEnabled) {

            motionDetector = MotionDetector.Builder(configuration.cameraMotionMinLuma, configuration.cameraMotionLeniency).build()

            motionDetectorProcessor = MultiProcessor.Builder<Motion>(MultiProcessor.Factory<Motion> {
                object : Tracker<Motion>() {
                    override fun onUpdate(p0: Detector.Detections<Motion>?, motion: Motion?) {
                        super.onUpdate(p0, motion)
                        if (cameraCallback != null && configuration.cameraMotionEnabled) {
                            Timber.d("Motion Detected : " + motion?.type)
                            if(Motion.MOTION_TOO_DARK == motion?.type) {
                                cameraCallback!!.onTooDark()
                            } else if (Motion.MOTION_DETECTED == motion?.type) {
                                cameraCallback!!.onMotionDetected()
                            }
                        }
                        if (motion?.byteArray != null) {
                            bitmapCreateTask = BitmapTask(object : OnCompleteListener {
                                override fun onComplete(byteArray: ByteArray?) {
                                    bitmapComplete = true
                                    setJpeg(byteArray!!)
                                }
                            })
                            if(bitmapComplete) {
                                bitmapComplete = false
                                bitmapCreateTask!!.execute(motion.byteArray, motion.width, motion.height)
                            }
                        }
                    }
                }
            }).build()

            motionDetector!!.setProcessor(motionDetectorProcessor)

            faceDetector = FaceDetector.Builder(context)
                    .setProminentFaceOnly(true)
                    .setTrackingEnabled(false)
                    .setMode(FaceDetector.FAST_MODE)
                    .setClassificationType(FaceDetector.NO_CLASSIFICATIONS)
                    .setLandmarkType(FaceDetector.NO_LANDMARKS)
                    .build()

            faceDetectorProcessor = LargestFaceFocusingProcessor(faceDetector, object: Tracker<Face>() {
                override fun onUpdate(detections: Detector.Detections<Face>, face: Face) {
                    super.onUpdate(detections, face)
                    if(detections.detectedItems.size() > 0) {
                        if(cameraCallback != null && configuration.cameraFaceEnabled) {
                            Timber.d("Face Detected")
                            cameraCallback!!.onFaceDetected()
                        }
                    }
                }
            })

            faceDetector!!.setProcessor(faceDetectorProcessor)

            barcodeDetector = BarcodeDetector.Builder(context)
                    .setBarcodeFormats(Barcode.QR_CODE)
                    .build()

            barCodeDetectorProcessor = MultiProcessor.Builder<Barcode>(MultiProcessor.Factory<Barcode> {
                object : Tracker<Barcode>() {
                    override fun onUpdate(p0: Detector.Detections<Barcode>?, p1: Barcode?) {
                        super.onUpdate(p0, p1)
                        if(cameraCallback != null && configuration.cameraQRCodeEnabled) {
                            Timber.d("Barcode: " + p1?.displayValue)
                            cameraCallback!!.onQRCode(p1?.displayValue)
                        }
                    }
                }
            }).build()

            barcodeDetector!!.setProcessor(barCodeDetectorProcessor);

            multiDetector = MultiDetector.Builder()
                    .add(barcodeDetector)
                    .add(faceDetector)
                    .add(motionDetector)
                    .build();

            cameraSource = CameraSource.Builder(context, multiDetector)
                    .setRequestedFps(15.0f)
                    .setRequestedPreviewSize(640, 480)
                    .setFacing(configuration.cameraId)
                    .build()

            if(preview != null) {
                preview.start(cameraSource)
            } else {
                cameraSource!!.start()
            }
        }
    }

    interface OnCompleteListener {
        fun onComplete(byteArray: ByteArray?)
    }

    class BitmapTask(private val onCompleteListener: OnCompleteListener) : AsyncTask<Any, Void, ByteArray>() {

        override fun doInBackground(vararg params: kotlin.Any): ByteArray? {
            if (isCancelled) {
                return null
            }
            val byteArray = params[0] as ByteArray
            val width = params[1] as Int
            val height = params[2] as Int
            val orientation = params[3] as Int

            val out = ByteArrayOutputStream();
            val yuvImage =  YuvImage(byteArray, ImageFormat.NV21, width, height, null);
            yuvImage.compressToJpeg(Rect(0, 0, width, height), 30, out);
            val imageBytes = out.toByteArray();
            /*val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size);
            val outstr = ByteArrayOutputStream()
            bitmap!!.compress(Bitmap.CompressFormat.JPEG, 80, outstr)
            return outstr.toByteArray()*/
            return imageBytes

            /*val result = Nv21Image.nv21ToBitmap(renderScript, byteArray, width, height)
            val windowService = contextRef.get()!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val currentRotation = windowService.defaultDisplay.rotation
            var rotate = orientation
            if (currentRotation == Surface.ROTATION_90)
                rotate += 90
            else if (currentRotation == Surface.ROTATION_180)
                rotate += 180
            else if (currentRotation == Surface.ROTATION_270) rotate += 270
            rotate %= 360
            val matrix = Matrix()
            matrix.postRotate(rotate.toFloat())
            return Bitmap.createBitmap(result, 0, 0, width, height, matrix, true)*/
        }

        override fun onPostExecute(result: ByteArray?) {
            super.onPostExecute(result)
            if (isCancelled) {
                return
            }
            onCompleteListener.onComplete(result)
        }
    }

    companion object {

    }
}