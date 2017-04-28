package de.rhuber.homedash;

public interface CameraDetectorCallback {
    void onMotionDetected();
    void onTooDark();
    void onFaceDetected();
}
