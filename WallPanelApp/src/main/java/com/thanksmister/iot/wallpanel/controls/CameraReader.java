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

package org.wallpanelproject.android.controls;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.os.Handler;
import android.support.v8.renderscript.RenderScript;
import android.util.Log;
import android.util.SparseArray;
import android.view.Surface;
import android.view.WindowManager;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.android.gms.vision.face.FaceDetector;
import com.jjoe64.motiondetection.motiondetection.AggregateLumaMotionDetection;
import com.jjoe64.motiondetection.motiondetection.ImageProcessing;

import org.wallpanelproject.android.network.WallPanelService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import io.github.silvaren.easyrs.tools.Nv21Image;
import timber.log.Timber;

public class CameraReader {

    private final SurfaceTexture mSurfaceTexture;
    private Camera mCamera;
    private static ArrayList<String> cameraList;

    private byte[] currentFrame = new byte[0];
    private int currentWidth = 0;
    private int currentHeight = 0;
    private int currentOrientation = 0;

    private final RenderScript rs;
    private AggregateLumaMotionDetection motionDetector;
    private FaceDetector faceDetector;
    private BarcodeDetector barcodeDetector;
    private CameraDetectorCallback cameraDetectorCallback;
    private int minLuma = 1000;

    private Handler detectorCheckHandler;
    private boolean checkQR = false;
    private boolean checkFace = false;
    private long mCheckInterval = 1000;

    private final Context mContext;

    public CameraReader(Context context) {
        mContext = context;
        mSurfaceTexture = new SurfaceTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        rs = RenderScript.create(context);
        getCameras();
    }

    protected void finalize() {
        try { super.finalize(); } catch (Throwable t) { t.printStackTrace(); }
        stop();
    }

    public void start(int cameraId, long checkInterval, CameraDetectorCallback cameraDetectorCallback) {
        Timber.d("start Called");
        mCheckInterval = checkInterval;
        this.cameraDetectorCallback = cameraDetectorCallback;
        if (mCamera == null) {
            mCamera = getCameraInstance(cameraId);
            if (mCamera == null) {
                Timber.d("There is no camera so nothing is going to happen :(");
            } else {
                try {
                    mCamera.setPreviewTexture(mSurfaceTexture);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                final Camera.Parameters params = mCamera.getParameters();
                int mPreviewFormat = mCamera.getParameters().getPreviewFormat();
                final Camera.Size previewSize = params.getPreviewSize();
                currentWidth = previewSize.width;
                currentHeight = previewSize.height;
                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(cameraId, info);
                currentOrientation = info.orientation;
                final int BITS_PER_BYTE = 8;
                final int bytesPerPixel = ImageFormat.getBitsPerPixel(mPreviewFormat) / BITS_PER_BYTE;

                final int mPreviewBufferSize = currentWidth * currentHeight * bytesPerPixel * 3 / 2 + 1;
                byte[] mBuffer = new byte[mPreviewBufferSize];
                currentFrame = new byte[mPreviewBufferSize];
                mCamera.addCallbackBuffer(mBuffer);
                mCamera.setPreviewCallbackWithBuffer(previewCallback);

                mCamera.startPreview();
            }
        }

        if (detectorCheckHandler == null) {
            detectorCheckHandler = new Handler();
            detectorCheckHandler.postDelayed(checkDetections, mCheckInterval);
        }
    }

    public void startMotionDetection(int minLuma, int leniency) {
        Timber.d("startMotionDetection Called");
        if (motionDetector == null) {
            motionDetector = new AggregateLumaMotionDetection();
            this.minLuma = minLuma;
            motionDetector.setLeniency(leniency);
        }
    }

    public void startFaceDetection() {
        Timber.d("startFaceDetection Called");
        if (faceDetector == null) {
            faceDetector = new FaceDetector.Builder(mContext)
                    .setProminentFaceOnly(true)
                    .build();
        }
        checkFace = true;
    }

    public void startQRCodeDetection() {
        Timber.d("startQRCodeDetection Called");
        if (barcodeDetector == null) {
            barcodeDetector = new BarcodeDetector.Builder(mContext)
                            .setBarcodeFormats(Barcode.QR_CODE)
                            .build();
        }
        checkQR = true;
    }

    public void stop() {
        Timber.d("stop Called");
        if (detectorCheckHandler != null) {
            detectorCheckHandler.removeCallbacks(checkDetections);
            detectorCheckHandler = null;
        }

        checkFace = false;
        if (faceDetector != null) {
            faceDetector.release();
            faceDetector = null;
        }

        checkQR = false;
        if (barcodeDetector != null) {
            barcodeDetector.release();
            barcodeDetector = null;
        }

        if (motionDetector != null) {
            motionDetector = null;
        }

        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private final Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera cam) {
            byte[] lastFrame = currentFrame;
            Camera.Size s = cam.getParameters().getPreviewSize();
            currentWidth = s.width;
            currentHeight = s.height;
            currentFrame = data;
            cam.addCallbackBuffer(lastFrame);
        }
    };

    private Camera getCameraInstance(int cameraId) {
        Timber.d("getCameraInstance called");
        Camera c = null;
        int numCameras = Camera.getNumberOfCameras();
        try {
            if (cameraId >= numCameras)
                c = Camera.open(0);
            else
                c = Camera.open(cameraId);
            c.setDisplayOrientation(180);
        }
        catch (Exception e) {
            Timber.e(e.getMessage());
        }
        return c;
    }

    public static ArrayList<String> getCameras() {
        if (cameraList == null) {
            cameraList = new ArrayList<>();
            for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
                String description;
                try {
                    final Camera c = Camera.open(i);
                    Camera.Parameters p = c.getParameters();
                    final Camera.Size previewSize = p.getPreviewSize();
                    int width = previewSize.width;
                    int height = previewSize.height;
                    Camera.CameraInfo info = new Camera.CameraInfo();
                    Camera.getCameraInfo(i, info);
                    description = java.text.MessageFormat.format(
                            "{0}: {1} Camera {3}x{4} {2}º",
                            i,
                            (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) ? "Front" : "Back",
                            info.orientation,
                            width,
                            height);
                    c.release();
                } catch (Exception e) {
                    Log.e("CameraReader", "Had a problem reading camera " + i);
                    e.printStackTrace();
                    description = java.text.MessageFormat.format("{0}: Error", i);
                }
                cameraList.add(description);
            }
        }
        return cameraList;
    }

    public byte[] getJpeg() {
        ByteArrayOutputStream outstr = new ByteArrayOutputStream();
        getBitmap().compress(Bitmap.CompressFormat.JPEG, 80, outstr);
        return outstr.toByteArray();
    }

    private Bitmap getBitmap(byte[] data) {
        if (mCamera != null) {
            Bitmap result = Nv21Image.nv21ToBitmap(rs, data, currentWidth, currentHeight);
            WindowManager windowService = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            final int currentRotation = windowService.getDefaultDisplay().getRotation();
            int rotate = currentOrientation;
            if (currentRotation == Surface.ROTATION_90) rotate += 90;
            else if (currentRotation == Surface.ROTATION_180) rotate += 180;
            else if (currentRotation == Surface.ROTATION_270) rotate += 270;
            rotate = rotate % 360;

            Matrix matrix = new Matrix();
            matrix.postRotate(rotate);
            return Bitmap.createBitmap(result, 0, 0, currentWidth, currentHeight, matrix, true);
        } else if(currentWidth > 0 && currentHeight > 0) {
            return getCameraNotEnabledBitmap();
        } else {
            Timber.e("Doesn't look like the canvas is ready.");
        }
        return null;
    }

    public Bitmap getBitmap() {
        return getBitmap(currentFrame);
    }

    private Bitmap getCameraNotEnabledBitmap() {
        if(currentWidth == 0 && currentHeight == 0) {
            return null;
        }
        Bitmap b = Bitmap.createBitmap(currentWidth, currentHeight, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        c.drawPaint(paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(20);
        Rect r = new Rect();
        String text = "Camera Not Enabled ";
        c.getClipBounds(r);
        int cHeight = r.height();
        int cWidth = r.width();
        paint.setTextAlign(Paint.Align.LEFT);
        paint.getTextBounds(text, 0, text.length(), r);
        float x = cWidth / 2f - r.width() / 2f - r.left;
        float y = cHeight / 2f + r.height() / 2f - r.bottom;
        c.drawText(text, x, y, paint);

        return b;
    }

    private final Runnable checkDetections = new Runnable() {
        @Override
        public void run () {
            if (currentFrame.length > 0) {
                checkMotionDetection(currentFrame);
                checkVisionLib(currentFrame);
            }
            detectorCheckHandler.postDelayed(this, mCheckInterval);
        }
    };

    private void checkMotionDetection(byte[] currentFrame) {
        if (motionDetector != null) {
            int[] img = ImageProcessing.decodeYUV420SPtoLuma(currentFrame, currentWidth, currentHeight);

            // check if it is too dark
            int lumaSum = 0;
            for (int i : img) {
                lumaSum += i;
            }
            if (lumaSum < minLuma) {
                if (cameraDetectorCallback != null) {
                    cameraDetectorCallback.onTooDark();
                }
            } else if (motionDetector.detect(img, currentWidth, currentHeight)) {
                // we have motion!
                if (cameraDetectorCallback != null) {
                    cameraDetectorCallback.onMotionDetected();
                }
            }
        }
    }

    private void checkVisionLib(byte[] currentFrame) {
        if (checkFace || checkQR) {
            Frame frame = new Frame.Builder()
                    .setBitmap(getBitmap(currentFrame))
                    .build();

            if (checkFace) {
                if (faceDetector.isOperational()) { //TODO rebuild the face detector when the screen rotates
                  if (faceDetector.detect(frame).size() > 0) {
                      if (cameraDetectorCallback != null) {
                          cameraDetectorCallback.onFaceDetected();
                      }
                  }
                }
            }

            if (checkQR) {
                if (barcodeDetector.isOperational()) {
                    SparseArray<Barcode> barcodes = barcodeDetector.detect(frame);
                    if (barcodes.size() > 0) {
                        String data = barcodes.valueAt(0).displayValue;
                        Timber.d("QR Code result: " + data);
                        if (cameraDetectorCallback != null) {
                            cameraDetectorCallback.onQRCode(data);
                        }
                    }
                }
            }
        }
    }

}
