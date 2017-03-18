package de.rhuber.homedash;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by raimund on 22.02.2017.
 */

public class SensorJob extends JobService {
    final String TAG = this.getClass().getName();
    HomeDashService mService;
    boolean mBound = false;
    Context context;

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.i(TAG, "Job started");
        context = getApplicationContext();
        Intent intent = new Intent(context, HomeDashService.class);
        context.bindService(intent, mqttService2Connection, Context.BIND_AUTO_CREATE);

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.i(TAG, "Job stopped");
        return true;
    }

    private ServiceConnection mqttService2Connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            HomeDashService.MqttServiceBinder binder = (HomeDashService.MqttServiceBinder) service;
            mService = binder.getService();
            mBound = true;
            mService.updateSensorData();
            context.unbindService(mqttService2Connection);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }

    };
}