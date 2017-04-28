package de.rhuber.homedash;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

public class CameraTestActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 1;
    private final String TAG = BrowserActivity.class.getName();
    public static final String BROADCAST_CAMERA_TEST_MSG = "BROADCAST_CAMERA_TEST_MSG";
    private Handler updateHandler;

    WallPanelService wallPanelService;
    boolean wallPanelServiceBound = false;

    private int interval = 1000/15;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_cameratest);
        requestCameraPermissions();
    }

    private void requestCameraPermissions(){
        if(PackageManager.PERMISSION_DENIED == ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_CAMERA);
        }
    }

    // TODO move to settings
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
                            "Camera permission not granted.",
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
        updateHandler.postDelayed(updatePicture, interval);
    }

    private final Runnable updatePicture = new Runnable() {
        @Override
        public void run () {
            if (wallPanelServiceBound) {
                final ImageView preview = (ImageView) findViewById(R.id.imageView_preview);
                byte[] jpeg = wallPanelService.cameraReader.getJpeg();
                preview.setImageBitmap(BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length));

                if (removeTextCountdown > 0) {
                    removeTextCountdown--;
                    if (removeTextCountdown == 0) {
                        setStatusText("");
                    }
                }
            }
            updateHandler.postDelayed(this, interval);
        }
    };

    private void stopUpdatePicture() {
        if (updateHandler != null) {
            updateHandler.removeCallbacks(updatePicture);
            updateHandler = null;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(this, WallPanelService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (wallPanelServiceBound) {
            unbindService(mConnection);
            wallPanelServiceBound = false;
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            WallPanelService.WallPanelServiceBinder binder = (WallPanelService.WallPanelServiceBinder)service;
            wallPanelService = binder.getService();
            wallPanelServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            wallPanelServiceBound = false;
        }
    };

    @Override
    public void onResume() {
        super.onResume();

        startUpdatePicture();

        LocalBroadcastManager.getInstance(this).
        registerReceiver(testReceiver,new IntentFilter(BROADCAST_CAMERA_TEST_MSG));
    }

    @Override
    public void onPause() {
        super.onPause();

        stopUpdatePicture();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(testReceiver);
    }

    private void setStatusText(String text) {
        final TextView status = (TextView) findViewById(R.id.textView_status);
        status.setText(text);
    }

    private int removeTextCountdown;
    private final BroadcastReceiver testReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        String message = intent.getStringExtra("message");
        Log.i(TAG, "Got a message: " + message);

        setStatusText(message);
            removeTextCountdown = 10;
        }
    };

}
