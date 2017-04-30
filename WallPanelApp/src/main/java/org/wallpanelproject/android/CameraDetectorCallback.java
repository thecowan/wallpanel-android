package org.wallpanelproject.android;

public interface CameraDetectorCallback {
    void onMotionDetected();
    void onTooDark();
    void onFaceDetected();
}
