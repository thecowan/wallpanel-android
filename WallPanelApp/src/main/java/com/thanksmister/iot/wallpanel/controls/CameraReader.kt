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
import android.os.AsyncTask
import android.view.Surface
import android.view.WindowManager
import com.google.android.gms.vision.*
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor
import com.thanksmister.iot.wallpanel.persistence.Configuration
import com.thanksmister.iot.wallpanel.ui.views.CameraSourcePreview
import io.github.silvaren.easyrs.tools.Nv21Image
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.lang.ref.WeakReference
import javax.inject.Inject
import android.graphics.Bitmap
import android.support.v8.renderscript.RenderScript


class CameraReader @Inject
constructor(private val context: Context) {

    private val renderScript: RenderScript = RenderScript.create(context)
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
    private var bitmapComplete = true;
    private var bitmapCreateTask: BitmapTask? = null
    private var cameraOrientation: Int = 0

    fun getJpeg(): LiveData<ByteArray> {
        return byteArray
    }

    private fun setJpeg(value: ByteArray) {
        Timber.d("setJpeg $value")
        this.byteArray.value = value
    }

    fun stopCamera() {

        if (bitmapCreateTask != null) {
            bitmapCreateTask!!.cancel(true)
            bitmapCreateTask = null
        }

        if (cameraSource != null) {
            cameraSource!!.release()
            cameraSource = null
        }

        if (faceDetector != null) {
            faceDetector!!.release()
            faceDetector = null
        }

        if (barcodeDetector != null) {
            barcodeDetector!!.release()
            barcodeDetector = null
        }

        if (motionDetector != null) {
            motionDetector!!.release()
            motionDetector = null
        }

        if (multiDetector != null) {
            multiDetector!!.release()
            multiDetector = null
        }

        if (faceDetectorProcessor != null) {
            faceDetectorProcessor!!.release()
            faceDetectorProcessor = null
        }

        if (barCodeDetectorProcessor != null) {
            barCodeDetectorProcessor!!.release()
            barCodeDetectorProcessor = null
        }

        if (motionDetectorProcessor != null) {
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

            val info = Camera.CameraInfo()
            Camera.getCameraInfo(configuration.cameraId, info)
            cameraOrientation = info.orientation

            motionDetector = MotionDetector.Builder(configuration.cameraMotionMinLuma, configuration.cameraMotionLeniency).build()

            motionDetectorProcessor = MultiProcessor.Builder<Motion>(MultiProcessor.Factory<Motion> {
                object : Tracker<Motion>() {
                    override fun onUpdate(p0: Detector.Detections<Motion>?, motion: Motion?) {
                        super.onUpdate(p0, motion)
                        if (cameraCallback != null && configuration.cameraMotionEnabled) {
                            if (Motion.MOTION_TOO_DARK == motion?.type) {
                                cameraCallback!!.onTooDark()
                            } else if (Motion.MOTION_DETECTED == motion?.type) {
                                Timber.d("motionDetected")
                                cameraCallback!!.onMotionDetected()
                            }
                        }
                        if (motion?.byteArray != null && bitmapComplete) {
                            Timber.d("bitmapCreateTask")
                            bitmapCreateTask = BitmapTask(context, renderScript, object : OnCompleteListener {
                                override fun onComplete(byteArray: ByteArray?) {
                                    bitmapComplete = true
                                    setJpeg(byteArray!!)
                                }
                            })
                            bitmapComplete = false
                            bitmapCreateTask!!.execute(motion.byteArray, motion.width, motion.height, cameraOrientation)
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

            faceDetectorProcessor = LargestFaceFocusingProcessor(faceDetector, object : Tracker<Face>() {
                override fun onUpdate(detections: Detector.Detections<Face>, face: Face) {
                    super.onUpdate(detections, face)
                    if (detections.detectedItems.size() > 0) {
                        if (cameraCallback != null && configuration.cameraFaceEnabled) {
                            Timber.d("faceDetected")
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
                        if (cameraCallback != null && configuration.cameraQRCodeEnabled) {
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

            if (preview != null) {
                preview.start(cameraSource)
            } else {
                cameraSource!!.start()
            }
        }
    }

    interface OnCompleteListener {
        fun onComplete(byteArray: ByteArray?)
    }

    class BitmapTask(context: Context, private val renderScript: RenderScript, private val onCompleteListener: OnCompleteListener) : AsyncTask<Any, Void, ByteArray>() {

        private val contextRef: WeakReference<Context> = WeakReference(context)

        override fun doInBackground(vararg params: kotlin.Any): ByteArray? {
            if (isCancelled) {
                return null
            }
            val byteArray = params[0] as ByteArray
            val width = params[1] as Int
            val height = params[2] as Int
            var orientation = params[3] as Int

           /* val out = ByteArrayOutputStream();
            val yuvImage = YuvImage(byteArray, ImageFormat.NV21, width, height, null);
            yuvImage.compressToJpeg(Rect(0, 0, width, height), 80, out);
            var imageBytes = out.toByteArray();*/

            val windowService = contextRef.get()!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val currentRotation = windowService.defaultDisplay.rotation

            Timber.d("orientation: $orientation")
            Timber.d("currentRotation: $currentRotation")

            val nv21Bitmap = Nv21Image.nv21ToBitmap(renderScript, byteArray, width, height)
            var rotate = orientation
            when (currentRotation) {
                Surface.ROTATION_90 -> {
                    Timber.d("ROTATION_90")
                    orientation += 90
                }
                Surface.ROTATION_180 -> {
                    Timber.d("ROTATION_180")
                    orientation += 180
                }
                Surface.ROTATION_270 -> {
                    Timber.d("ROTATION_270")
                    orientation += 270
                }
            }

            rotate %= 360

            val matrix = Matrix()
            matrix.postRotate(rotate.toFloat())
            val bitmap =  Bitmap.createBitmap(nv21Bitmap, 0, 0, width, height, matrix, true)

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            val byteArrayOut = stream.toByteArray()
            bitmap.recycle()

            return byteArrayOut
        }

        override fun onPostExecute(result: ByteArray?) {
            super.onPostExecute(result)
            if (isCancelled) {
                return
            }
            Timber.d("onPostExecute")
            onCompleteListener.onComplete(result)
        }
    }

    companion object {

    }
}