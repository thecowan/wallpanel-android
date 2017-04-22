package de.rhuber.homedash;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

public class MotionActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 1;
    private final String TAG = BrowserActivity.class.getName();
    public static final String BROADCAST_MOTION_DETECTOR_MSG = "BROADCAST_MOTION_DETECTOR_MSG";
    private Handler updateHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_motion);
        requestCameraPermissions();
    }

    private void requestCameraPermissions(){
        if(PackageManager.PERMISSION_DENIED == ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Snackbar snackbar = Snackbar.make(getWindow().getDecorView().getRootView(),
                            "Camera permission granted.",
                            Snackbar.LENGTH_LONG);
                    snackbar.show();
                } else {
                    Snackbar snackbar = Snackbar.make(getWindow().getDecorView().getRootView(),
                            "Camera permission not granted. Motion detection won't work.",
                            Snackbar.LENGTH_LONG);
                    snackbar.show();
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void startUpdatePicture() {
        updateHandler = new Handler();
        updateHandler.postDelayed(updatePicture, 100);
    }

    private final Runnable updatePicture = new Runnable() {
        @Override
        public void run () {
            final ImageView preview = (ImageView) findViewById(R.id.imageView_preview);
            preview.setImageBitmap(WallPanelService.getInstance().getMotionPicture());
            updateHandler.postDelayed(this, 100);

            if (removeTextCountdown > 0) {
                removeTextCountdown--;
                if (removeTextCountdown == 0) {
                    setStatusText("");
                }
            }
        }
    };

    private void stopUpdatePicture() {
        if (updateHandler != null) {
            updateHandler.removeCallbacks(updatePicture);
            updateHandler = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        startUpdatePicture();

        LocalBroadcastManager.getInstance(this).
        registerReceiver(motionReceiver,new IntentFilter(BROADCAST_MOTION_DETECTOR_MSG));
    }

    @Override
    public void onPause() {
        super.onPause();

        stopUpdatePicture();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(motionReceiver);
    }

    private void setStatusText(String text) {
        final TextView status = (TextView) findViewById(R.id.textView_status);
        status.setText(text);
    }

    private int removeTextCountdown;
    private final BroadcastReceiver motionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        String message = intent.getStringExtra("message");
        Log.i(TAG, "Got a message: " + message);

        setStatusText(message);
            removeTextCountdown = 10;
        }
    };

}
