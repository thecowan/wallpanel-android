package org.wallpanelproject.android;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.SENSOR_SERVICE;

class SensorReader  {
    private final String TAG = this.getClass().getName();
    private final String VALUE = "value";
    private final String UNIT = "unit";
    private final String ID = "id";

    private WallPanelService wallPanelService;

    private final SensorManager mSensorManager;
    private final List<Sensor> mSensorList = new ArrayList<Sensor>();

    private final Context context;

    private final Handler sensorHandler = new Handler();
    private Integer updateFrequencyMilliSeconds = 0;

    private int motionDetectedCountdown = 0;
    private int faceDetectedCountdown = 0;

    public SensorReader(WallPanelService wallPanelService, Context context) {
        Log.d(TAG, "Creating SensorReader");
        this.wallPanelService = wallPanelService;
        this.context = context;
        mSensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
        for (Sensor s : mSensorManager.getSensorList(Sensor.TYPE_ALL)) {
            if (getSensorName(s.getType()) != null)
                mSensorList.add(s);
        }
    }

    public void startReadings(int freqSeconds) {
        Log.d(TAG, "startReadings Called");
        if (freqSeconds >= 0) {
            updateFrequencyMilliSeconds = 1000 * freqSeconds;
            sensorHandler.postDelayed(sensorHandlerRunnable, updateFrequencyMilliSeconds);
        }
    }

    private final Runnable sensorHandlerRunnable = new Runnable() {
        @Override
        public void run() {
            if (updateFrequencyMilliSeconds > 0) {
                Log.i(TAG, "Updating Sensors");
                getSensorReadings();
                getBatteryReading();
                updateMotionDetected();
                updateFaceDetected();
                sensorHandler.postDelayed(this, updateFrequencyMilliSeconds);
            }
        }
    };

    public void stopReadings() {
        Log.d(TAG, "stopSensorJob Called");
        sensorHandler.removeCallbacksAndMessages(sensorHandlerRunnable);
        updateFrequencyMilliSeconds = 0;
    }

    private void publishSensorData(String sensorName, JSONObject sensorData) {
        Log.d(TAG, "publishSensorData Called");
        wallPanelService.publishMessage(
                sensorData,
                "sensor/" + sensorName);
    }

    private String getSensorName(int sensorType) {
        switch (sensorType) {
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                return "temperature";
            case Sensor.TYPE_LIGHT: // TODO change in API to light
                return "light";
            case Sensor.TYPE_MAGNETIC_FIELD:
                return "magneticField";
            case Sensor.TYPE_PRESSURE:
                return "pressure";
            case Sensor.TYPE_RELATIVE_HUMIDITY:
                return "humidity";
        }
        return null;
    }

    private void getSensorReadings() {
        Log.d(TAG, "getSensorReadings called");
        for (Sensor sensor : mSensorList) {
            mSensorManager.registerListener(new SensorEventListener(){
                @Override
                public void onSensorChanged(SensorEvent event) {
                    JSONObject data = new JSONObject();
                    try {
                        data.put(VALUE, event.values[0]);
                        data.put(UNIT, "??");
                        data.put(ID, event.sensor.getName());
                    } catch (JSONException ex) { ex.printStackTrace(); }

                    publishSensorData(getSensorName(event.sensor.getType()), data);
                    mSensorManager.unregisterListener(this);
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {}
            }, sensor, 1000);
        }
    }

    private void getBatteryReading(){
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, intentFilter);

        int batteryStatusIntExtra = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1) : -1;
        boolean isCharging = batteryStatusIntExtra == BatteryManager.BATTERY_STATUS_CHARGING ||
                batteryStatusIntExtra == BatteryManager.BATTERY_STATUS_FULL;

        int chargePlug = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) : -1;
        boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
        boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

        int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
        int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;

        JSONObject data = new JSONObject();
        try {
            data.put(VALUE, level);
            String BATTERYSENSOR_UNIT = "%";
            data.put(UNIT, BATTERYSENSOR_UNIT);
            data.put("charging", isCharging);
            data.put("acPlugged", acCharge);
            data.put("usbPlugged", usbCharge);
        } catch (JSONException ex) { ex.printStackTrace(); }

        publishSensorData("battery", data);
    }

    public void doMotionDetected() {
        Log.d(TAG, "doMotionDetected called");
        if (motionDetectedCountdown <= 0) {
            JSONObject data = new JSONObject();
            try {
                data.put(VALUE, true);
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
            publishSensorData("motion", data);
        }
        motionDetectedCountdown = 2;
    }

    private void updateMotionDetected() {
        if (motionDetectedCountdown > 0) {
            motionDetectedCountdown--;
            if (motionDetectedCountdown == 0) {
                Log.i(TAG, "Clearing motion detected status");
                JSONObject data = new JSONObject();
                try { data.put(VALUE, false); } catch (JSONException ex) { ex.printStackTrace(); }
                publishSensorData("motion", data);
            }
        }
    }

    public void doFaceDetected() {
        Log.d(TAG, "doFaceDetected called");
        if (faceDetectedCountdown <= 0) {
            JSONObject data = new JSONObject();
            try {
                data.put(VALUE, true);
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
            publishSensorData("face", data);
        }
        faceDetectedCountdown = 2;
    }

    private void updateFaceDetected() {
        if (faceDetectedCountdown > 0) {
            faceDetectedCountdown--;
            if (faceDetectedCountdown == 0) {
                Log.i(TAG, "Clearing face detected status");
                JSONObject data = new JSONObject();
                try { data.put(VALUE, false); } catch (JSONException ex) { ex.printStackTrace(); }
                publishSensorData("face", data);
            }
        }
    }
}
