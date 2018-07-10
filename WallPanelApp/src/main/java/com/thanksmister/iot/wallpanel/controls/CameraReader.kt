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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.opengl.GLES11Ext
import android.os.Handler
import android.support.v8.renderscript.RenderScript
import android.util.Log
import android.view.Surface
import android.view.WindowManager

import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.google.android.gms.vision.face.FaceDetector
import com.jjoe64.motiondetection.motiondetection.AggregateLumaMotionDetection
import com.jjoe64.motiondetection.motiondetection.ImageProcessing

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.ArrayList

import io.github.silvaren.easyrs.tools.Nv21Image
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class CameraReader(private val mContext: Context) {

    private val mSurfaceTexture: SurfaceTexture = SurfaceTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES)
    private var mCamera: Camera? = null

    private var currentFrame = ByteArray(0)
    private var currentWidth = 0
    private var currentHeight = 0
    private var currentOrientation = 0

    private val rs: RenderScript = RenderScript.create(mContext)
    private var motionDetector: AggregateLumaMotionDetection? = null
    private var faceDetector: FaceDetector? = null
    private var barcodeDetector: BarcodeDetector? = null
    private var cameraDetectorCallback: CameraCallback? = null
    private var minLuma = 1000

    private var detectorCheckHandler: Handler? = null
    private var checkQR = false
    private var checkFace = false
    private var mCheckInterval: Long = 1000

    private val previewCallback = Camera.PreviewCallback { data, cam ->
        if(cam != null && cam.parameters != null) {
            val lastFrame = currentFrame
            val s = cam.parameters.previewSize
            currentWidth = s.width
            currentHeight = s.height
            currentFrame = data
            cam.addCallbackBuffer(lastFrame)
        }
    }

    val jpeg: ByteArray
        get() {
            val outstr = ByteArrayOutputStream()
            bitmap!!.compress(Bitmap.CompressFormat.JPEG, 80, outstr)
            return outstr.toByteArray()
        }

    val bitmap: Bitmap?
        get() = getBitmap(currentFrame)

    fun getBitmapObservable(): Observable<Bitmap?> {
        return Observable.fromCallable { getBitmap(currentFrame) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    private val checkDetections = object : Runnable {
        override fun run() {
            if (currentFrame.size > 0) {
                checkMotionDetection(currentFrame)
                checkVisionLib(currentFrame)
            }
            detectorCheckHandler!!.postDelayed(this, mCheckInterval)
        }
    }

    init {
        getCameraList()
    }

    fun start(cameraId: Int, checkInterval: Long, cameraDetectorCallback: CameraCallback) {
        Timber.d("start Called")
        mCheckInterval = checkInterval
        this.cameraDetectorCallback = cameraDetectorCallback
        if (mCamera == null) {
            mCamera = getCameraInstance(cameraId)
            if (mCamera == null) {
                Timber.d("There is no camera so nothing is going to happen :(")
            } else {
                try {
                    mCamera!!.setPreviewTexture(mSurfaceTexture)
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                val params = mCamera!!.parameters
                val mPreviewFormat = mCamera!!.parameters.previewFormat
                val previewSize = params.previewSize
                currentWidth = previewSize.width
                currentHeight = previewSize.height
                val info = Camera.CameraInfo()
                Camera.getCameraInfo(cameraId, info)
                currentOrientation = info.orientation
                val BITS_PER_BYTE = 8
                val bytesPerPixel = ImageFormat.getBitsPerPixel(mPreviewFormat) / BITS_PER_BYTE

                val mPreviewBufferSize = currentWidth * currentHeight * bytesPerPixel * 3 / 2 + 1
                val mBuffer = ByteArray(mPreviewBufferSize)
                currentFrame = ByteArray(mPreviewBufferSize)
                mCamera!!.addCallbackBuffer(mBuffer)
                mCamera!!.setPreviewCallbackWithBuffer(previewCallback)
                mCamera!!.startPreview()
            }
        }

        if (detectorCheckHandler == null) {
            detectorCheckHandler = Handler()
            detectorCheckHandler!!.postDelayed(checkDetections, mCheckInterval)
        }
    }

    fun startMotionDetection(minLuma: Int, leniency: Int) {
        Timber.d("startMotionDetection Called")
        if (motionDetector == null) {
            motionDetector = AggregateLumaMotionDetection()
            this.minLuma = minLuma
            motionDetector!!.setLeniency(leniency)
        }
    }

    fun startFaceDetection() {
        Timber.d("startFaceDetection Called")
        if (faceDetector == null) {
            faceDetector = FaceDetector.Builder(mContext)
                    .setProminentFaceOnly(true)
                    .build()
        }
        checkFace = true
    }

    fun startQRCodeDetection() {
        Timber.d("startQRCodeDetection Called")
        if (barcodeDetector == null) {
            barcodeDetector = BarcodeDetector.Builder(mContext)
                    .setBarcodeFormats(Barcode.QR_CODE)
                    .build()
        }
        checkQR = true
    }

    fun stop() {
        Timber.d("stop Called")
        if (detectorCheckHandler != null) {
            detectorCheckHandler!!.removeCallbacks(checkDetections)
            detectorCheckHandler = null
        }

        checkFace = false
        if (faceDetector != null) {
            faceDetector!!.release()
            faceDetector = null
        }

        checkQR = false
        if (barcodeDetector != null) {
            barcodeDetector!!.release()
            barcodeDetector = null
        }

        if (motionDetector != null) {
            motionDetector = null
        }

        if (mCamera != null) {
            mCamera!!.setPreviewCallback(null)
            mCamera!!.stopPreview()
            mCamera!!.release()
            mCamera = null
        }
    }

    private fun getCameraInstance(cameraId: Int): Camera? {
        Timber.d("getCameraInstance called")
        var c: Camera? = null
        val numCameras = Camera.getNumberOfCameras()
        try {
            if (cameraId >= numCameras)
                c = Camera.open(0)
            else
                c = Camera.open(cameraId)
            c!!.setDisplayOrientation(180)
        } catch (e: Exception) {
            Timber.e(e.message)
        }

        return c
    }

    // TODO need to do this on background thread
    private fun getBitmap(data: ByteArray): Bitmap? {
        if (mCamera != null) {
            val result = Nv21Image.nv21ToBitmap(rs, data, currentWidth, currentHeight)
            val windowService = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val currentRotation = windowService.defaultDisplay.rotation
            var rotate = currentOrientation
            if (currentRotation == Surface.ROTATION_90)
                rotate += 90
            else if (currentRotation == Surface.ROTATION_180)
                rotate += 180
            else if (currentRotation == Surface.ROTATION_270) rotate += 270
            rotate %= 360

            val matrix = Matrix()
            matrix.postRotate(rotate.toFloat())
            return Bitmap.createBitmap(result, 0, 0, currentWidth, currentHeight, matrix, true)
        } else if (currentWidth > 0 && currentHeight > 0) {
            return cameraNotEnabledBitmap()
        } else {
            Timber.e("Doesn't look like the canvas is ready.")
        }
        return null
    }

    // TODO do on background thread
    private fun cameraNotEnabledBitmap(): Bitmap? {
        if (currentWidth == 0 && currentHeight == 0) {
            return null
        }
        val b = Bitmap.createBitmap(currentWidth, currentHeight, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val paint = Paint()
        paint.color = Color.BLACK
        paint.style = Paint.Style.FILL
        c.drawPaint(paint)

        paint.color = Color.WHITE
        paint.textSize = 20f
        val r = Rect()
        val text = "Camera Not Enabled "
        c.getClipBounds(r)
        val cHeight = r.height()
        val cWidth = r.width()
        paint.textAlign = Paint.Align.LEFT
        paint.getTextBounds(text, 0, text.length, r)
        val x = cWidth / 2f - r.width() / 2f - r.left.toFloat()
        val y = cHeight / 2f + r.height() / 2f - r.bottom
        c.drawText(text, x, y, paint)

        return b
    }

    private fun checkMotionDetection(currentFrame: ByteArray) {
        if (motionDetector != null) {
            val img = ImageProcessing.decodeYUV420SPtoLuma(currentFrame, currentWidth, currentHeight)

            // check if it is too dark
            var lumaSum = 0
            for (i in img) {
                lumaSum += i
            }
            if (lumaSum < minLuma) {
                if (cameraDetectorCallback != null) {
                    cameraDetectorCallback!!.onTooDark()
                }
            } else if (motionDetector!!.detect(img, currentWidth, currentHeight)) {
                // we have motion!
                if (cameraDetectorCallback != null) {
                    cameraDetectorCallback!!.onMotionDetected()
                }
            }
        }
    }

    private fun checkVisionLib(currentFrame: ByteArray) {
        if (checkFace || checkQR) {
            val frame = Frame.Builder()
                    .setBitmap(getBitmap(currentFrame)!!)
                    .build()

            if (checkFace) {
                if (faceDetector!!.isOperational) { //TODO rebuild the face detector when the screen rotates
                    if (faceDetector!!.detect(frame).size() > 0) {
                        if (cameraDetectorCallback != null) {
                            cameraDetectorCallback!!.onFaceDetected()
                        }
                    }
                }
            }

            if (checkQR) {
                if (barcodeDetector!!.isOperational) {
                    val barcodes = barcodeDetector!!.detect(frame)
                    if (barcodes.size() > 0) {
                        val data = barcodes.valueAt(0).displayValue
                        Timber.d("QR Code result: $data")
                        if (cameraDetectorCallback != null) {
                            cameraDetectorCallback!!.onQRCode(data)
                        }
                    }
                }
            }
        }
    }

    companion object {
        fun getCameraList(): ArrayList<String> {
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
            return cameraList
        }
    }
}
