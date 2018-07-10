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

import android.annotation.SuppressLint
import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.graphics.*
import android.hardware.Camera
import android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT
import android.os.AsyncTask
import android.os.Handler
import android.util.Log
import android.util.SparseArray
import com.google.android.gms.common.images.Size
import com.google.android.gms.vision.*
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor
import com.jjoe64.motiondetection.motiondetection.AggregateLumaMotionDetection
import com.jjoe64.motiondetection.motiondetection.ImageProcessing
import com.thanksmister.iot.wallpanel.controls.CameraCallback
import com.thanksmister.iot.wallpanel.controls.Motion
import com.thanksmister.iot.wallpanel.controls.MotionDetector
import com.thanksmister.iot.wallpanel.persistence.Configuration
import com.thanksmister.iot.wallpanel.ui.views.CameraSourcePreview
import timber.log.Timber
import java.util.ArrayList
import javax.inject.Inject

/**
 * Created by Michael Ritchie on 6/28/18.
 */
class CameraViewModel @Inject
constructor(application: Application, private val configuration: Configuration) : AndroidViewModel(application) {

    //private val mSurfaceTexture: SurfaceTexture = SurfaceTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES)
    //private val renderScript: RenderScript = RenderScript.create(getApplication())
    private val bitmap = MutableLiveData<Bitmap>()
    private val byteArray = MutableLiveData<ByteArray>()
    private val cameras = MutableLiveData<ArrayList<String>>()
    private var camera: Camera? = null
    private var currentWidth = 0
    private var currentHeight = 0
    //private var currentOrientation = 0
    private var cameraCallback: CameraCallback? = null
    private var checkMotionInterval: Long = 0L
    private var motionDetectionTask: MotionTask? = null
    private var cameraStopped = false
    private var motionCheckComplete = true
    private var detectorCheckHandler: Handler? = null
    private var motionDetector: AggregateLumaMotionDetection? = null
    private var faceDetector: FaceDetector? = null
    private var barcodeDetector: BarcodeDetector? = null
    private var motionDetectorCustom: MotionDetector? = null
    private var multiDetector: MultiDetector? = null
    private var cameraSource: CameraSource? = null
    private var faceDetectorProcessor: LargestFaceFocusingProcessor? = null
    private var barCodeDetectorProcessor: MultiProcessor<Barcode>? = null

    private fun getByteArray(): ByteArray? {
        return byteArray.value
    }

    private fun setByteArray(byteArray: ByteArray) {
        this.byteArray.value = byteArray
    }

    fun getBitmap(): LiveData<Bitmap> {
        return bitmap
    }

    fun setBitmap(bitmap: Bitmap?) {
        this.bitmap.value = bitmap
    }

    fun getCameras(): LiveData<ArrayList<String>> {
        return cameras
    }

    private fun setCameras(cameras: ArrayList<String>) {
        this.cameras.value = cameras
    }

    @Suppress("DEPRECATION")
    private val previewCallback = Camera.PreviewCallback { data, cam ->
        try {
            if (cam != null && !cameraStopped) {
                val s = cam.parameters.previewSize
                currentWidth = s.width
                currentHeight = s.height
                setByteArray(data)
                cam.addCallbackBuffer(getByteArray())
            }
        } catch (e: Exception) {
            Timber.w(e.message)
        }
    }

    init {
        Timber.d("init")
        getCameraList()
    }

    public override fun onCleared() {
        //prevents memory leaks by disposing pending observable objects

        stopCamera()

        if(motionDetectionTask != null) {
            motionDetectionTask!!.cancel(true)
            motionDetectionTask = null
        }

        if(cameraSource != null) {
            cameraSource!!.release()
            cameraSource = null
        }

        if(detectorCheckHandler != null) {
            detectorCheckHandler!!.removeCallbacks(processDetectedImageRunnable)
            detectorCheckHandler = null
        }

        if(faceDetector != null) {
            faceDetector!!.release()
            faceDetector = null
        }

        if(barcodeDetector != null) {
            barcodeDetector!!.release()
            barcodeDetector = null
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
    }

    private fun stopCamera() {
        Timber.d("stopCamera")
        cameraStopped = true;
        if (camera != null) {
            camera!!.setPreviewCallback(null)
            camera!!.stopPreview()
            camera!!.release()
            camera = null
        }
    }

    @Suppress("DEPRECATION")
    private fun getCameraList() {
        //TODO maybe we should check camera permissions?
        val cameraList: ArrayList<String> = ArrayList()
        for (i in 0 until Camera.getNumberOfCameras()) {
            var description: String
            try {
                val c = Camera.open(i)
                val p = c.parameters
                val previewSize = p.previewSize
                val width = previewSize.width
                val height = previewSize.height
                val info = Camera.CameraInfo()
                Camera.getCameraInfo(i, info)
                description = java.text.MessageFormat.format(
                        "{0}: {1} Camera {3}x{4} {2}º",
                        i,
                        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) "Front" else "Back",
                        info.orientation,
                        width,
                        height)
                c.release()
            } catch (e: Exception) {
                Log.e("CameraReader", "Had a problem reading camera $i")
                e.printStackTrace()
                description = java.text.MessageFormat.format("{0}: Error", i)
            }
            cameraList.add(description)
        }
        setCameras(cameraList)
    }

    /*@Suppress("DEPRECATION")
    fun startCamera(cameraCallback: CameraCallback) {
        this.cameraCallback = cameraCallback
        if(configuration.cameraEnabled) {
            if (camera == null) {
                camera = getCameraInstance(configuration.cameraId)
            }
            if (camera != null) {
                cameraStopped = false;
                Timber.d("We have camera!!")
                camera!!.setPreviewTexture(mSurfaceTexture)
                currentWidth = camera!!.parameters.previewSize.width
                currentHeight = camera!!.parameters.previewSize.height

                val info = Camera.CameraInfo()
                Camera.getCameraInfo(configuration.cameraId, info)
                currentOrientation = info.orientation

                val mPreviewFormat = camera!!.parameters.previewFormat
                val bytesPerPixel = ImageFormat.getBitsPerPixel(mPreviewFormat) / BITS_PER_BYTE
                val mPreviewBufferSize = currentWidth * currentHeight * bytesPerPixel * 3 / 2 + 1
                val mBuffer = ByteArray(mPreviewBufferSize)

                camera!!.addCallbackBuffer(mBuffer)
                camera!!.setPreviewCallback(previewCallback)
                camera!!.startPreview()
            }
        }
    }*/

    fun startCamera(callback: CameraCallback, preview: CameraSourcePreview?) {
        Timber.d("startCamera")
        this.cameraCallback = callback
        if (multiDetector == null && configuration.cameraEnabled) {

            if(configuration.cameraMotionEnabled) {
                motionDetectorCustom = MotionDetector.Builder(getApplication()).build()
                val motionDetectorProcessor = MultiProcessor.Builder<Motion>(MultiProcessor.Factory<Motion> {
                    object : Tracker<Motion>() {
                        override fun onUpdate(p0: Detector.Detections<Motion>?, p1: Motion?) {
                            super.onUpdate(p0, p1)
                            Timber.d("Motion Detected : " + p1?.type)
                        }
                    }
                }).build()

                motionDetectorCustom!!.setProcessor(motionDetectorProcessor)
            }

            if(configuration.cameraFaceEnabled) {
                faceDetector = FaceDetector.Builder(getApplication())
                        .setProminentFaceOnly(true)
                        .setTrackingEnabled(false)
                        .setMode(FaceDetector.FAST_MODE)
                        .setClassificationType(FaceDetector.NO_CLASSIFICATIONS)
                        .setLandmarkType(FaceDetector.NO_LANDMARKS)
                        .build()

                faceDetectorProcessor = LargestFaceFocusingProcessor(multiDetector, object: Tracker<Face> () {
                    override fun onUpdate(detections: Detector.Detections<Face>, face: Face) {
                        super.onUpdate(detections, face)
                        if(detections.detectedItems.size() > 0) {
                            Timber.d("Face Detected")
                            if(cameraCallback != null && configuration.cameraFaceEnabled) {
                                cameraCallback!!.onFaceDetected()
                            }
                        }
                    }
                })

                faceDetector!!.setProcessor(faceDetectorProcessor)
            }

            if(configuration.cameraQRCodeEnabled) {
                barcodeDetector = BarcodeDetector.Builder(getApplication())
                        .setBarcodeFormats(Barcode.QR_CODE)
                        .build()

                barCodeDetectorProcessor = MultiProcessor.Builder<Barcode>(MultiProcessor.Factory<Barcode> {
                    object : Tracker<Barcode>() {
                        override fun onUpdate(p0: Detector.Detections<Barcode>?, p1: Barcode?) {
                            super.onUpdate(p0, p1)
                            Timber.d("Barcode: " + p1?.displayValue)
                            if(cameraCallback != null && configuration.cameraQRCodeEnabled) {
                                cameraCallback!!.onQRCode(p1?.displayValue)
                            }
                        }
                    }
                }).build()

                barcodeDetector!!.setProcessor(barCodeDetectorProcessor);
            }

            if(faceDetector != null && barcodeDetector != null && motionDetectorCustom != null) {
                multiDetector = MultiDetector.Builder()
                        .add(barcodeDetector)
                        .add(faceDetector)
                        .add(motionDetectorCustom)
                        .build();
            } else if(barcodeDetector != null && motionDetectorCustom != null) {
                multiDetector = MultiDetector.Builder()
                        .add(barcodeDetector)
                        .add(motionDetectorCustom)
                        .build();
            } else if(faceDetector != null && motionDetectorCustom != null) {
                multiDetector = MultiDetector.Builder()
                        .add(motionDetectorCustom)
                        .add(faceDetector)
                        .build();
            } else if(faceDetector != null && barcodeDetector != null) {
                multiDetector = MultiDetector.Builder()
                        .add(barcodeDetector)
                        .add(faceDetector)
                        .build();
            } else if(faceDetector != null) {
                multiDetector = MultiDetector.Builder()
                        .add(faceDetector)
                        .build();
            } else if (barcodeDetector != null) {
                multiDetector = MultiDetector.Builder()
                        .add(barcodeDetector)
                        .build();
            } else if (motionDetectorCustom != null) {
                multiDetector = MultiDetector.Builder()
                        .add(motionDetectorCustom)
                        .build();
            }

            cameraSource = CameraSource.Builder(getApplication(), multiDetector)
                    .setRequestedFps(15.0f)
                    .setRequestedPreviewSize(640, 480)
                    .setFacing(CAMERA_FACING_FRONT)
                    .build()

            if(preview != null) {
                preview.start(cameraSource)
            }
        }
    }

    // TODO pass in the selected camera preview size (width/height)
    @SuppressLint("MissingPermission")
    fun startFaceDetection(preview: CameraSourcePreview, callback: CameraCallback) {
        Timber.d("startFaceDetection")
        this.cameraCallback = callback
        if (configuration.cameraFaceEnabled) {
            if (faceDetector == null) {
                faceDetector = FaceDetector.Builder(getApplication())
                        .setProminentFaceOnly(true)
                        .setTrackingEnabled(false)
                        .setMode(FaceDetector.FAST_MODE)
                        .setClassificationType(FaceDetector.NO_CLASSIFICATIONS)
                        .setLandmarkType(FaceDetector.NO_LANDMARKS)
                        .build()

                faceDetectorProcessor = LargestFaceFocusingProcessor(faceDetector, object: Tracker<Face> () {
                    override fun onUpdate(detections: Detector.Detections<Face>, face: Face) {
                        super.onUpdate(detections, face)
                        if(detections.detectedItems.size() > 0) {
                            Timber.d("Face Detected")
                            if(cameraCallback != null) {
                                cameraCallback!!.onFaceDetected()
                            }
                        }
                    }
                })
                faceDetector!!.setProcessor(faceDetectorProcessor)

                cameraSource = CameraSource.Builder(getApplication(), faceDetector)
                        .setRequestedFps(15.0f)
                        .setRequestedPreviewSize(640, 480)
                        .setFacing(CAMERA_FACING_FRONT)
                        .build()

                preview.start(cameraSource)
            }
        }
    }

    fun startMotionDetection() {
        Timber.d("startMotionDetection")
        checkMotionInterval = configuration.cameraProcessingInterval
        if (motionDetector == null) {
            motionDetector = AggregateLumaMotionDetection()
            motionDetector!!.setLeniency(configuration.cameraMotionLeniency)
        }

        if (detectorCheckHandler == null) {
            detectorCheckHandler = Handler()
            detectorCheckHandler!!.postDelayed(processDetectedImageRunnable, checkMotionInterval)
        }
    }

    private val processDetectedImageRunnable = object : Runnable {
        override fun run() {
            Timber.d("processDetectedImageRunnable")
            if(cameraSource != null) {
                cameraSource?.takePicture({}, {
                    setByteArray(it)
                    if(motionCheckComplete && getByteArray() != null) {
                        val previewSize = cameraSource!!.previewSize
                        Timber.d("Preview Width ${previewSize.width}")
                        Timber.d("Preview Height ${previewSize.height}")
                        createCheckMotionTask(previewSize)
                    }
                })
                detectorCheckHandler!!.postDelayed(this, checkMotionInterval)
            }
        }
    }

    private fun createCheckMotionTask(previewSize: Size) {
        if(motionCheckComplete) {
            Timber.d("createCheckMotionTask")
            motionDetectionTask = MotionTask(motionDetector!!, object : OnMotionListener {
                override fun onNoMotion() {
                    motionCheckComplete = true
                }
                override fun onMotionDetected() {
                    Timber.d("Motion Detected!!")
                    if (cameraCallback != null) {
                        cameraCallback!!.onMotionDetected()
                    }
                    motionCheckComplete = true
                }
                override fun onTooDark() {
                    Timber.d("Too Dark!!")
                    if (cameraCallback != null) {
                        cameraCallback!!.onTooDark()
                    }
                    motionCheckComplete = true
                }
            })
            motionCheckComplete = false
            motionDetectionTask!!.execute(getByteArray(), previewSize.width, previewSize.height, configuration.cameraMotionMinLuma)
        }
    }

    /*@Deprecated("We moved this to async")
    private fun checkMotionDetection(currentFrame: ByteArray) {
        Timber.d("checkMotionDetection")
        if (motionDetector != null && configuration.cameraEnabled && configuration.cameraMotionEnabled) {
            val img = ImageProcessing.decodeYUV420SPtoLuma(currentFrame, currentWidth, currentHeight)
            var lumaSum = 0
            for (i in img) {
                lumaSum += i
            }
            if (lumaSum < configuration.cameraMotionMinLuma) {
                if (cameraCallback != null) {
                    Timber.d("checkMotionDetection too dark!!")
                    cameraCallback!!.onTooDark()
                }
            } else if (motionDetector!!.detect(img, currentWidth, currentHeight)) {
                if (cameraCallback != null) {
                    Timber.d("checkMotionDetection motion detected!!")
                    cameraCallback!!.onMotionDetected()
                }
            }
        }
    }*/

    /*@Suppress("DEPRECATION")
    private fun getCameraInstance(cameraId: Int): Camera? {
        var camera: Camera? = null
        val numCameras = Camera.getNumberOfCameras()
        try {
            camera = if (cameraId >= numCameras) {
                Camera.open(0)
            } else {
                Camera.open(cameraId)
            }
            camera!!.setDisplayOrientation(180)
        } catch (e: Exception) {
            Timber.e(e.message)
        }
        return camera
    }*/

    interface OnMotionListener {
        fun onMotionDetected()
        fun onTooDark()
        fun onNoMotion()
    }

    /*class BitmapTask(context: Context, private val renderScript: RenderScript,
                     private val onCompleteListener: OnCompleteListener) : AsyncTask<kotlin.Any, Void, Bitmap>() {

        private val contextRef: WeakReference<Context> = WeakReference(context)

        override fun doInBackground(vararg params: kotlin.Any): Bitmap? {
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
            val result = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size);
            return result

            //TODO rotating is expensive, can we do anything about this?
            //val result = Nv21Image.nv21ToBitmap(renderScript, byteArray, width, height)
           *//* val windowService = contextRef.get()!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
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
            return Bitmap.createBitmap(result, 0, 0, width, height, matrix, true)*//*
        }

        override fun onPostExecute(result: Bitmap?) {
            super.onPostExecute(result)
            if (isCancelled) {
                return
            }
            onCompleteListener.onComplete(result)
        }
    }*/

    class MotionTask(private val motionDetection: AggregateLumaMotionDetection,
                     private val onMotionListener: OnMotionListener) : AsyncTask<kotlin.Any, Void, String>() {

        private val MOTION_TOO_DARK = "motion_too_dark"
        private val MOTION_DETECTED = "motion_detected"
        private val MOTION_NOT_DETECTED = "motion_not_detected"

        override fun doInBackground(vararg params: kotlin.Any): String {
            if (isCancelled) {
                return MOTION_NOT_DETECTED
            }
            val byteArray = params[0] as ByteArray
            val width = params[1] as Int
            val height = params[2] as Int
            val minLuma = params[3] as Int

            val img = ImageProcessing.decodeYUV420SPtoLuma(byteArray, width/10, height/10)
            var lumaSum = 0
            for (i in img) {
                lumaSum += i
            }
            if (lumaSum < minLuma) {
                return MOTION_TOO_DARK
            } else if (motionDetection.detect(img, width/10, height/10)) {
                return MOTION_DETECTED
            }
            return MOTION_NOT_DETECTED
        }

        override fun onPostExecute(result: String) {
            super.onPostExecute(result)
            if (isCancelled) {
                return
            }
            if (MOTION_TOO_DARK == result) {
                onMotionListener.onTooDark()
            } else if (MOTION_DETECTED == result) {
                onMotionListener.onMotionDetected()
            } else {
                onMotionListener.onNoMotion()
            }
        }
    }

    /*class QrCodeReaderTask(private val readQrCode: Boolean, private val barcodeDetector: BarcodeDetector?,
                     private val listener: OnQrCodeListener) : AsyncTask<kotlin.Any, Void, String>() {

        private val QR_CODE_READ = "qrcode_read"
        private val NO_QRCODE_READ = "qrcode_not_read"

        override fun doInBackground(vararg params: kotlin.Any): String {
            if (isCancelled) {
                return NO_QRCODE_READ
            }

            val bitmap = params[0] as Bitmap
            val frame = Frame.Builder()
                    .setBitmap(bitmap)
                    .build()

            if (readQrCode && barcodeDetector != null && barcodeDetector.isOperational) {
                val barcodeList = barcodeDetector.detect(frame)
                if (barcodeList.size() > 0) {
                    val data = barcodeList.valueAt(0).displayValue
                    Timber.d("QR Code result: $data")
                    return data
                }
            }

            return NO_QRCODE_READ
        }

        override fun onPostExecute(result: String) {
            super.onPostExecute(result)
            if (isCancelled) {
                return
            }
            if (QR_CODE_READ == result) {
                listener.onQrCodeRead(result)
            } else {
                listener.onNoQrCode()
            }
        }
    }
    */

    companion object {
        const val BITS_PER_BYTE = 8
    }
}