package de.rhuber.homedash;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
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

public class MotionActivity extends AppCompatActivity {

    final String TAG = BrowserActivity.class.getName();
    public static final String BROADCAST_MOTION_DETECTOR_MSG = "BROADCAST_MOTION_DETECTOR_MSG";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_motion);
    }

    private void updatePicture() {
        final ImageView preview = (ImageView) findViewById(R.id.imageView_preview);
        preview.setImageBitmap(HomeDashService.getInstance().getMotionPicture());
    }

    private void prependStatus(String text) {
        final TextView status = (TextView) findViewById(R.id.textView_status);
        status.setText(text + "\n" + status.getText());
    }

    @Override
    public void onResume() {
        super.onResume();

        updatePicture();

        LocalBroadcastManager.getInstance(this).
        registerReceiver(motionReceiver,new IntentFilter(BROADCAST_MOTION_DETECTOR_MSG));
    }

    @Override
    public void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(motionReceiver);
    }

    private BroadcastReceiver motionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Log.i(TAG, "Got a message: " + message);
            updatePicture();

            DateFormat df = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
            String date = df.format(Calendar.getInstance().getTime());
            prependStatus(date + " " + message);
        }
    };

}
