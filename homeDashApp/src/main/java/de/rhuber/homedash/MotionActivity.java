package de.rhuber.homedash;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Timer;

public class MotionActivity extends AppCompatActivity {

    final String TAG = BrowserActivity.class.getName();
    public static final String BROADCAST_MOTION_DETECTOR_MSG = "BROADCAST_MOTION_DETECTOR_MSG";
    private Handler updateHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_motion);
    }

    private void startUpdatePicture() {
        updateHandler = new Handler();
        updateHandler.postDelayed(updatePicture, 100);
    }

    private Runnable updatePicture = new Runnable() {
        @Override
        public void run () {
            final ImageView preview = (ImageView) findViewById(R.id.imageView_preview);
            preview.setImageBitmap(HomeDashService.getInstance().getMotionPicture());
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

    private void prependStatus(String text) {
        final TextView status = (TextView) findViewById(R.id.textView_status);
        status.setText(text + "\n" + status.getText());
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
    private BroadcastReceiver motionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        String message = intent.getStringExtra("message");
        Log.i(TAG, "Got a message: " + message);

        final TextView status = (TextView) findViewById(R.id.textView_status);
        setStatusText(message);
            removeTextCountdown = 10;
        }
    };

}
