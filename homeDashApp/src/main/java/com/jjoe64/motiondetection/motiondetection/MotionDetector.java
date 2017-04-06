package com.jjoe64.motiondetection.motiondetection;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class MotionDetector {
    class MotionDetectorThread extends Thread {
        private final AtomicBoolean isRunning = new AtomicBoolean(true);

        public void stopDetection() {
            isRunning.set(false);
        }

        @Override
        public void run() {
            while (isRunning.get()) {
                long now = System.currentTimeMillis();
                if (now-lastCheck > checkInterval) {
                    lastCheck = now;

                    //刚启动摄像头图像不稳，可能误判，因此忽略前面几帧
                    if(detectCount < 5){
                        detectCount++;
                        mCamera.addCallbackBuffer(mBuffer);
                        continue;
                    }


                    if(!safeToTakePicture){
                        continue;
                    }

                    if (nextData.get() != null) {
                        int[] img = ImageProcessing.decodeYUV420SPtoLuma(nextData.get(), nextWidth.get(), nextHeight.get());

                        // check if it is too dark
                        int lumaSum = 0;
                        for (int i : img) {
                            lumaSum += i;
                        }
                        if (lumaSum < minLuma) {
                            if (motionDetectorCallback != null) {
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        motionDetectorCallback.onTooDark();
                                    }
                                });
                            }
                        } else if (detector.detect(img, nextWidth.get(), nextHeight.get())) {
                            // check
                            if (motionDetectorCallback != null) {
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        motionDetectorCallback.onMotionDetected();
                                    }
                                });
                            }
                        }

                        if(inPreview)
                            mCamera.addCallbackBuffer(mBuffer);
                    }
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private final AggregateLumaMotionDetection detector;
    private long checkInterval = 200;
    private long lastCheck = 0;
    private MotionDetectorCallback motionDetectorCallback;
    private Handler mHandler = new Handler();

    private AtomicReference<byte[]> nextData = new AtomicReference<>();
    private AtomicInteger nextWidth = new AtomicInteger();
    private AtomicInteger nextHeight = new AtomicInteger();
    private int minLuma = 1000;
    private MotionDetectorThread worker;

    private Camera mCamera;
    private boolean inPreview;

    private SurfaceTexture mSurfaceTexture;
    private int mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private byte[] mBuffer;
    private boolean safeToTakePicture = true;
    private long detectCount;

    public MotionDetector(int cameraId) {
        mCameraId = cameraId;
        detector = new AggregateLumaMotionDetection();

        mSurfaceTexture = new SurfaceTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
    }

    public void setMotionDetectorCallback(MotionDetectorCallback motionDetectorCallback) {
        this.motionDetectorCallback = motionDetectorCallback;
    }

    private void consume(byte[] data, int width, int height) {
        nextData.set(data);
        nextWidth.set(width);
        nextHeight.set(height);
    }

    public void setCheckInterval(long checkInterval) {
        this.checkInterval = checkInterval;
    }

    public void setMinLuma(int minLuma) {
        this.minLuma = minLuma;
    }

    public void setLeniency(int l) {
        detector.setLeniency(l);
    }

    public void onResume() {
        if(mCamera == null) {
            mCamera = getCameraInstance();//TODO we need to fail better if camera doesn't work

            if (mCamera != null) {
                worker = new MotionDetectorThread();
                worker.start();

                try {
                    mCamera.setPreviewTexture(mSurfaceTexture);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setPreviewSize(1280, 720);
                //parameters.setPreviewFpsRange(5, 15);
                int size2 = 1920 * 1080 * 3;
                //size2  = size2 * ImageFormat.getBitsPerPixel(parameters.getPreviewFormat()) / 8;
                mBuffer = new byte[size2]; // class variable
                mCamera.addCallbackBuffer(mBuffer);
                mCamera.setPreviewCallbackWithBuffer(previewCallback);

                mCamera.setParameters(parameters);
                mCamera.startPreview();
                inPreview = true;
            } else {
                Log.e("MotionDetector", "There is no camera so nothing is going to happen :(");
            }
        }
        detectCount = 0;
    }

    public static ArrayList<String> getCameras() {
        ArrayList<String> result = new ArrayList<>();
        for (int i=0; i<Camera.getNumberOfCameras(); i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            result.add(java.text.MessageFormat.format(
                    "id:{0} facing:{1} orientation:{2}",
                    i, info.facing, info.orientation));

        }
        return result;
    }

    private Camera getCameraInstance(){
        Camera c = null;
        int numCameras = Camera.getNumberOfCameras();
        if (numCameras > 0){
            try {
                if (mCameraId >= numCameras)
                    c = Camera.open(0);
                else
                    c = Camera.open(mCameraId);
            }
            catch (Exception e){
                // Camera is not available (in use or does not exist)
                Log.e("MotionDetector", e.getMessage());
            }

        } else {
            Log.e("MotionDetector", "There is no camera hardware reported!");
        }
        return c; // returns null if camera is unavailable
    }

    private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {

        /**
         * {@inheritDoc}
         */
        @Override
        public void onPreviewFrame(byte[] data, Camera cam) {
            if (data == null) return;
            Camera.Size size = cam.getParameters().getPreviewSize();
            if (size == null) return;
            Log.d("MotionDetectorSSSSSSSSS", "Using width=" + size.width + " height=" + size.height);
            consume(data, size.width, size.height);
        }
    };

    public void onPause() {
        releaseCamera();
        if (worker != null) worker.stopDetection();
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.setPreviewCallback(null);
            if (inPreview) mCamera.stopPreview();
            inPreview = false;
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    public Bitmap getLastBitmap() {
        safeToTakePicture = false;
        int[] rgb = ImageProcessing.decodeYUV420SPtoRGB(nextData.get(), nextWidth.get(), nextHeight.get());
        Bitmap bitmap = Bitmap.createBitmap(rgb, nextWidth.get(), nextHeight.get(), Bitmap.Config.ARGB_8888);
        safeToTakePicture = true;
        return bitmap;
    }

    protected void finalize()
    {
        onPause();
    }
}
