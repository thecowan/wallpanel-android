package org.wallpanelproject.android;

interface CameraDetectorCallback {
    void onMotionDetected();
    void onTooDark();
    void onFaceDetected();
    void onQRCode(String data);
}
