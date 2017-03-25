package de.rhuber.homedash;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.util.Log;
import android.support.v4.util.ArrayMap;

import java.util.Map;

import static android.content.Context.SENSOR_SERVICE;

class SensorReader  {
    private final String TAG = this.getClass().getName();
    private final SensorManager mSensorManager;
    private final Sensor mLight;
    private final SensorEventListener lightListener;
    private final SensorEventListener pressureListener;
    private final Sensor mPressure;

    private final Context context;
    private SensorDataListener lightCallback;

    private SensorDataListener pressureCallback;

    private final String LIGHTSENSOR_UNIT = "lx";
    private final String PRESSURESENSOR_UNIT = "??";
    private final String SENSOR = "sensor";
    private final String VALUE = "value";
    private final String UNIT = "unit";
    @SuppressWarnings("FieldCanBeLocal")
    private final String BATTERYSENSOR_UNIT = "%";


    public SensorReader(Context context) {
        this.context = context;
        mSensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
        mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);

        lightListener = new SensorEventListener(){
            @Override
            public void onSensorChanged(SensorEvent event) {
                //if(event.sensor == mLight){
                    if(lightCallback!= null){
                        ArrayMap<String, String> map = new ArrayMap<>(3);
                        map.put(SENSOR, (event.sensor.getName()));
                        map.put(VALUE, Float.toString(event.values[0]));
                        map.put(UNIT, LIGHTSENSOR_UNIT);
                        lightCallback.sensorData(map);
                    }
                    Log.i(TAG, Float.toString(event.values[0]));
                //}
                mSensorManager.unregisterListener(this);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        pressureListener = new SensorEventListener(){
            @Override
            public void onSensorChanged(SensorEvent event) {
                if(pressureCallback!= null){
                    ArrayMap<String, String> map = new ArrayMap<>(3);
                    map.put(SENSOR, (event.sensor.getName()));
                    map.put(VALUE, Float.toString(event.values[0]) );
                    map.put(UNIT, PRESSURESENSOR_UNIT);
                    pressureCallback.sensorData(map);
                }
                Log.i(TAG, Float.toString(event.values[0]));
                mSensorManager.unregisterListener(this);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
    }

    public interface SensorDataListener {
        void sensorData(Map<String,String> sensorData);
    }

    public void getLightReading(SensorDataListener listener){
        lightCallback = listener;
        mSensorManager.registerListener(lightListener,mLight,1000);
    }

    public  void getPressureReading(SensorDataListener listener){
        pressureCallback = listener;
        mSensorManager.registerListener(pressureListener,mPressure,1000);
    }

    public  void getBatteryReading(SensorDataListener listener){

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

        float batteryPct = level / (float)scale;

        Log.i(TAG, "AC connected: "+acCharge);
        Log.i(TAG, "USB connected: "+usbCharge);
        Log.i(TAG, "Battery charging: "+ isCharging);
        Log.i(TAG, "Battery Level: "+ batteryPct);

        ArrayMap<String, String> map = new ArrayMap<>(3);
        map.put(SENSOR, "Battery");
        map.put(VALUE, Integer.toString(level));
        map.put(UNIT, BATTERYSENSOR_UNIT);
        map.put("charging", Boolean.toString(isCharging));
        map.put("acPlugged", Boolean.toString(acCharge));
        map.put("usbPlugged", Boolean.toString(usbCharge));
        listener.sensorData(map);
    }
}
