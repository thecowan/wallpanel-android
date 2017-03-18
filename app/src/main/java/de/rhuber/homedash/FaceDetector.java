package de.rhuber.homedash;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.util.Log;

import java.io.IOException;
import java.util.List;

public class FaceDetector {
    private final String TAG = FaceDetector.class.getName();
    FaceDetectionCallback faceDetectionCallback;
    Camera camera;
    SurfaceTexture surfaceTexture = new SurfaceTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);

    public interface FaceDetectionCallback{
        public void facesDetected(int faceCount);
    }

    public void startDetection(final FaceDetectionCallback faceDetectionCallback){
        if(faceDetectionCallback == null){
            Log.e(TAG, "FaceDetectionCallback callback missing");
            return;
        }
        this.faceDetectionCallback = faceDetectionCallback;
        camera = getCameraInstance();
        if(camera == null){
            return;
        }
        try {
            camera.getParameters().setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
            camera.setPreviewTexture(surfaceTexture);
            camera.setFaceDetectionListener(new Camera.FaceDetectionListener() {
                @Override
                public void onFaceDetection(Camera.Face[] faces, Camera camera) {
                    if(faces.length > 0) {
                        faceDetectionCallback.facesDetected(faces.length);
                        Log.i(TAG, "Faces  detected " + faces.length);
                    }
                }
            });
            camera.startPreview();
        } catch (IOException e) {
            Log.e(TAG,"Failed to open camera",e);
        }
    }

    public void stopDetection(){
        Log.i(TAG, "Detetion stopped");
        camera.release();
    }

    private Camera getCameraInstance(){
        Camera camera = null;
        try {
            if (Camera.getNumberOfCameras() > 1 ) {
                camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
            } else {
                camera = Camera.open();
            }
        }
        catch (Exception e){
            Log.e(TAG, "Couldn't open Camera");
        }
        return camera;
    }
}
