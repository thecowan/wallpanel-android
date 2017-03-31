package de.rhuber.homedash;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import android.util.Log;

import com.jjoe64.motiondetection.MotionDetector;
import com.jjoe64.motiondetection.MotionDetectorCallback;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.Map;


public class HomeDashService extends Service {
    private static final int ONGOING_NOTIFICATION_ID = 1;
    private static final String MQTT_COMMAND_WAKEUP = "wakeup";
    private static final String MQTT_COMMAND_CLEAR_BROWSER_CACHE = "clearBrowserCache";
    private static final String MQTT_COMMAND_JS_EXEC = "jsExec";
    private static final String MQTT_COMMAND_URL = "url";
    private static final String MQTT_COMMAND_SAVE = "save";
    private static final String MQTT_COMMAND_RELOAD = "reload";

    private final String TAG = HomeDashService.class.getName();
    private final IBinder mBinder = new MqttServiceBinder();
    private MqttAndroidClient mqttAndroidClient;
    private Handler sensorHandler;
    private Runnable sensorHandlerRunnable;
    private SensorReader sensorReader;
    private String topicPrefix;
    private SharedPreferences sharedPreferences;
    SharedPreferences.OnSharedPreferenceChangeListener prefsChangedListener;
    private final String MOTION_SENSOR_MOTION_DETECTED_JSON ="{\"sensor\":\"cameraMotionDetector\",\"unit\":\"Boolean\",\"value\":\"true\"}";
    private MotionDetector motionDetector;
    private MotionDetectorCallback motionDetectorCallback;

    private PowerManager.WakeLock fullWakeLock;
    private PowerManager.WakeLock partialWakeLock;
    private WifiManager.WifiLock wifiLock;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d(TAG, "onTaskRemoved Called");
        stopSensorJob();
        stopMotionDetection();
        stopMqttConnection();
        stopSelf();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate Called");
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        // prepare the lock types we may use
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        //noinspection deprecation
        fullWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                PowerManager.ON_AFTER_RELEASE |
                PowerManager.ACQUIRE_CAUSES_WAKEUP, "fullWakeLock");
        partialWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "partialWakeLock");
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "wifiLock");

        // We always grab partialWakeLock
        Log.i(TAG, "Acquiring Partial Wake Lock");
        partialWakeLock.acquire();

        prefsChangedListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                if (s.equals(getString(R.string.key_setting_enable_mqtt))) {
                    Log.i(TAG, "MQTT Setting Changed");
                    boolean newValue = sharedPreferences.getBoolean(s, false);
                    if (newValue) {
                        final String topic = sharedPreferences.getString(getString(R.string.key_setting_mqtt_topic), "");
                        final String url = sharedPreferences.getString(getString(R.string.key_setting_mqtt_host), "");
                        final String clientId = "homeDash-" + Build.DEVICE;
                        final String username = sharedPreferences.getString(getString(R.string.key_setting_mqtt_username), "");
                        final String password = sharedPreferences.getString(getString(R.string.key_setting_mqtt_password), "");
                        startMqttConnection(url, clientId, topic, username, password);
                    } else {
                        stopMqttConnection();
                    }
                } else if (s.startsWith("setting_motion_detection")) {
                    Log.i(TAG, "A Motion Detection Setting Changed");
                    configureMotionDetection();
                } else if (s.equals(getString(R.string.key_setting_prevent_sleep))) {
                    Log.i(TAG, "Prevent Sleep Setting Changed");
                    boolean preventSleep = sharedPreferences.getBoolean(s, false);
                    if (preventSleep)
                    {
                        Log.i(TAG, "Acquiring WakeLock to prevent sleep");
                        if (!fullWakeLock.isHeld()) fullWakeLock.acquire();
                    }
                    else
                    {
                        Log.i(TAG, "Will not prevent sleep");
                        if (fullWakeLock.isHeld()) fullWakeLock.release();
                    }
                } else if (s.equals(getString(R.string.key_setting_keep_wifi_on))) {
                    Log.i(TAG, "Keep WiFi On Setting Changed");
                    boolean keepWiFiOn = sharedPreferences.getBoolean(s, false);
                    if (keepWiFiOn) {
                        Log.i(TAG, "Acquiring WakeLock to keep WiFi active");
                        if (!wifiLock.isHeld()) wifiLock.acquire();
                    }
                    else {
                        Log.i(TAG, "Will not stop WiFi turning off");
                        if (wifiLock.isHeld()) wifiLock.release();
                    }
                }
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(prefsChangedListener);
        prefsChangedListener.onSharedPreferenceChanged(sharedPreferences, getString(R.string.key_setting_prevent_sleep));
        prefsChangedListener.onSharedPreferenceChanged(sharedPreferences, getString(R.string.key_setting_keep_wifi_on));
        prefsChangedListener.onSharedPreferenceChanged(sharedPreferences, getString(R.string.key_setting_enable_mqtt));

        configureMotionDetection();

        startForeground();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy Called");
        stopSensorJob();
        stopMotionDetection();
        //stopMqttConnection();

        Log.i(TAG, "Releasing Screen/WiFi Locks");
        if (partialWakeLock.isHeld()) partialWakeLock.release();
        if (fullWakeLock.isHeld()) fullWakeLock.release();
        if (wifiLock.isHeld()) wifiLock.release();
    }

    public void startMqttConnection(String serverUri, String clientId, final String topic,
                                    final String username, final String password) {
        Log.d(TAG, "startMqttConnection Called");
        if (mqttAndroidClient == null) {
            topicPrefix = topic;
            if (!topicPrefix.endsWith("/")) {
                topicPrefix += "/";
            }

            mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), serverUri, clientId);
            mqttAndroidClient.setCallback(new MqttCallbackExtended() {

                @Override
                public void connectionLost(Throwable cause) {
                    Log.i(TAG, "MQTT connectionLost ", cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    Log.i(TAG, "MQTT messageArrived " + message);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.i(TAG, "MQTT deliveryComplete " + token);
                }

                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    Log.i(TAG, "MQTT connectComplete " + serverURI);
                }
            });
            MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
            if (username.length() > 0) {
                mqttConnectOptions.setUserName(username);
                mqttConnectOptions.setPassword(password.toCharArray());
            }
            mqttConnectOptions.setAutomaticReconnect(true);
            mqttConnectOptions.setCleanSession(false);
            try {
                mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                        disconnectedBufferOptions.setBufferEnabled(true);
                        disconnectedBufferOptions.setBufferSize(100);
                        disconnectedBufferOptions.setPersistBuffer(false);
                        disconnectedBufferOptions.setDeleteOldestMessages(false);
                        mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                        subscribeToTopic(topicPrefix + "command");
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.i(TAG, "MQTT connection failure: ", exception);
                    }
                });
            } catch (MqttException ex) {
                ex.printStackTrace();
            }
            startSensorJob();
        }
    }

    private void updateSensorData(){
        Log.d(TAG, "updateSensorData Called");
        if(sensorReader==null){
            sensorReader = new SensorReader(getApplicationContext());
        }

        if(sharedPreferences.getBoolean(getString(R.string.key_setting_sensor_light_enable),false)){
            sensorReader.getLightReading(new SensorReader.SensorDataListener() {
                @Override
                public void sensorData(Map<String, String> sensorData) {
                    publishSensorMessage(sensorData, "brightness");
                }
            });
        }

        if(sharedPreferences.getBoolean(getString(R.string.key_setting_sensor_battery_enable),false)) {
            sensorReader.getBatteryReading(new SensorReader.SensorDataListener() {
                @Override
                public void sensorData(Map<String, String> sensorData) {
                    publishSensorMessage(sensorData, "battery");
                }
            });
        }

        if(sharedPreferences.getBoolean(getString(R.string.key_setting_sensor_pressure_enable),false)) {
            sensorReader.getPressureReading(new SensorReader.SensorDataListener() {
                @Override
                public void sensorData(Map<String, String> sensorData) {
                    publishSensorMessage(sensorData, "pressure");
                }
            });
        }

    }

    private void publishSensorMessage(Map<String, String> map, String topicPostfix){
        Log.d(TAG, "publishSensorMessage Called");
        String message = new JSONObject(map).toString();
        publishMessage(message.getBytes(),topicPostfix);
    }

    public void stopMqttConnection(){
        Log.d(TAG, "stopMqttConnection Called");
        try {
            if(mqttAndroidClient != null && mqttAndroidClient.isConnected()) {
                mqttAndroidClient.disconnect();
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
        mqttAndroidClient = null;
        stopSensorJob();
    }

    private void publishMessage(byte[] message, String topicPostfix){
        Log.d(TAG, "publishMessage Called");
        if (mqttAndroidClient != null && mqttAndroidClient.isConnected()) {
            try {
                String test = new String(message, Charset.forName("UTF-8"));
                Log.i(TAG,test);
                mqttAndroidClient.publish(topicPrefix+"sensor/"+topicPostfix, message, 0, false);

            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    private boolean subscribeToTopic(final String subscriptionTopic) {
        Log.d(TAG, "subscribeToTopic Called");
        if (mqttAndroidClient.isConnected()) {
            try {
                mqttAndroidClient.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.i(TAG, "subscribed to: " + subscriptionTopic);
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.i(TAG, "Failed to subscribe to: " + subscriptionTopic);
                    }
                });

                mqttAndroidClient.subscribe(subscriptionTopic, 0, new IMqttMessageListener() {

                    @Override
                    public void messageArrived(String topic, MqttMessage message) throws Exception {
                        String payload = new String(message.getPayload(), Charset.forName("UTF-8"));
                        JSONObject jsonObject = new JSONObject(payload);

                        String url =  jsonObject.has(MQTT_COMMAND_URL) ? jsonObject.getString(MQTT_COMMAND_URL) : null;
                        if(url != null){
                            Log.i(TAG, "Browsing to new URL: "+url);
                            Intent intent = new Intent(BrowserActivity.BROADCAST_ACTION_LOAD_URL);
                            intent.putExtra(BrowserActivity.BROADCAST_ACTION_LOAD_URL, url);
                            LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getApplicationContext());
                            bm.sendBroadcast(intent);

                            boolean save = jsonObject.has(MQTT_COMMAND_SAVE) && jsonObject.getBoolean(MQTT_COMMAND_SAVE);
                            if (save) {
                                Log.i(TAG, "Saving new URL as default");
                                Editor ed = sharedPreferences.edit();
                                ed.putString(getString(R.string.key_setting_startup_url), url);
                                ed.apply();
                            }
                        }
                        if(jsonObject.has(MQTT_COMMAND_WAKEUP)){
                            switchScreenOn();
                        }
                        if(jsonObject.has(MQTT_COMMAND_RELOAD)) {
                            reloadPage();
                        }
                        if(jsonObject.has(MQTT_COMMAND_CLEAR_BROWSER_CACHE)){
                            clearBrowserCache();
                        }
                        String js =  jsonObject.has(MQTT_COMMAND_JS_EXEC) ? jsonObject.getString(MQTT_COMMAND_JS_EXEC) : null;
                        if(js != null){
                            Intent intent = new Intent(BrowserActivity.BROADCAST_ACTION_JS_EXEC);
                            intent.putExtra(BrowserActivity.BROADCAST_ACTION_JS_EXEC, js);
                            LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getApplicationContext());
                            bm.sendBroadcast(intent);
                        }
                        Log.i(TAG, "messageArrived: " + jsonObject);

                    }
                });
                return true;

            } catch (MqttException ex) {
                System.err.println("Exception whilst subscribing");
                ex.printStackTrace();
            }
        }
        return false;
    }

    private void startSensorJob(){
        Log.d(TAG, "startSensorJob Called");
        if (sensorHandler == null) {
            Integer updateFrequencySeconds = Integer.parseInt(sharedPreferences.getString(getString(R.string.key_setting_sensor_update_frequency),"60"));
            if(updateFrequencySeconds!= 0){
                sensorHandler = new Handler();
                final Integer updateFrequencyMilliSeconds = 1000 * updateFrequencySeconds;

                sensorHandlerRunnable = new Runnable() {
                    public void run() {
                        Log.i(TAG, "Updating Sensors");
                        updateSensorData();
                        sensorHandler.postDelayed(this, updateFrequencyMilliSeconds);
                    }
                };

                sensorHandler.postDelayed(sensorHandlerRunnable, updateFrequencyMilliSeconds);
            }
        }
    }

    private void stopSensorJob(){
        Log.d(TAG, "stopSensorJob Called");
        if(sensorHandler != null) {
            sensorHandler.removeCallbacksAndMessages(sensorHandlerRunnable);
            sensorHandler = null;
        }
    }

    public class MqttServiceBinder extends Binder {
        HomeDashService getService() {
            Log.d(TAG, "mqttServiceBinder.getService Called");
            return HomeDashService.this;
        }
    }

    private void startForeground(){
        Log.d(TAG, "startForeground Called");
        Intent notificationIntent = new Intent(this, HomeDashService.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT_WATCH) {
            notification = new Notification.Builder(this)
                    .setContentTitle(getText(R.string.homedash_service_notification_title))
                    .setContentText(getText(R.string.homedash_service_notification_message))
                    .setSmallIcon(R.drawable.ic_home_white_24dp)
                    .setLargeIcon(BitmapFactory.decodeResource(getApplication().getResources(),R.mipmap.ic_launcher))
                    .setContentIntent(pendingIntent)
                    .setLocalOnly(true)
                    .build();
        } else {
            notification = new Notification.Builder(this)
                    .setContentTitle(getText(R.string.homedash_service_notification_title))
                    .setContentText(getText(R.string.homedash_service_notification_message))
                    .setSmallIcon(R.drawable.ic_home_white_24dp)
                    .setLargeIcon(BitmapFactory.decodeResource(getApplication().getResources(),R.mipmap.ic_launcher))
                    .setContentIntent(pendingIntent)
                    .build();
        }

        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }


    public void configureMotionDetection(){
        Log.d(TAG, "updateMotionDetection Called");
        final boolean enabled = sharedPreferences.getBoolean(getString(R.string.key_setting_motion_detection_enable),false);
        if (enabled) {
            if (motionDetector == null) {
                motionDetector = new MotionDetector(this, null);
                if (motionDetectorCallback == null) {
                    motionDetectorCallback = new MotionDetectorCallback() {
                        @Override
                        public void onMotionDetected() {
                            switchScreenOn();
                            publishMessage(MOTION_SENSOR_MOTION_DETECTED_JSON.getBytes(Charset.forName("UTF-8")), "motion");
                            Log.i(TAG, "Motion detected");

                            Intent intent = new Intent(SettingsActivity.BROADCAST_MOTION_DETECTOR_MSG);
                            intent.putExtra("message","Motion Detected!");
                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                        }

                        @Override
                        public void onTooDark() {
                            Log.i(TAG, "Too dark for motion detection");

                            Intent intent = new Intent(SettingsActivity.BROADCAST_MOTION_DETECTOR_MSG);
                            intent.putExtra("message","Too dark for motion detection");
                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                        }
                    };
                }
                motionDetector.setMotionDetectorCallback(motionDetectorCallback);
                motionDetector.onResume();
            }
            motionDetector.setCheckInterval(Long.valueOf(sharedPreferences.getString(getString(R.string.key_setting_motion_detection_interval), "500")));
            motionDetector.setLeniency(Integer.valueOf(sharedPreferences.getString(getString(R.string.key_setting_motion_detection_leniency), "20")));
            motionDetector.setMinLuma(Integer.valueOf(sharedPreferences.getString(getString(R.string.key_setting_motion_detection_min_luma), "1000")));
        }
        else {
            stopMotionDetection();
        }
    }

    public void stopMotionDetection() {
        Log.d(TAG, "stopMotionDetection Called");
        if (motionDetector != null) {
            motionDetector.onPause();
            motionDetector = null;
            motionDetectorCallback = null;
        }
    }

    private void switchScreenOn(){
        Log.d(TAG, "switchScreenOn Called");
        // redundant if the screen is already being kept on
        if (!fullWakeLock.isHeld()) {
            fullWakeLock.acquire();
            fullWakeLock.release();
        }
    }

    private void reloadPage(){
        Log.d(TAG, "reloadPage Called");
        Intent intent = new Intent(BrowserActivity.BROADCAST_ACTION_RELOAD_PAGE);
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getApplicationContext());
        bm.sendBroadcast(intent);
    }

    private void clearBrowserCache(){
        Log.d(TAG, "clearBrowserCache Called");
        Intent intent = new Intent(BrowserActivity.BROADCAST_ACTION_CLEAR_BROWSER_CACHE);
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getApplicationContext());
        bm.sendBroadcast(intent);
    }
}
