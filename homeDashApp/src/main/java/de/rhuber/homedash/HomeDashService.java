package de.rhuber.homedash;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.os.Handler;
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
    private final String MOTION_SENSOR_MOTION_DETECTED_JSON ="{\"sensor\":\"cameraMotionDetector\",\"unit\":\"Boolean\",\"value\":\"true\"}";
    private MotionDetector motionDetector;
    private MotionDetectorCallback motionDetectorCallback;

    public HomeDashService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSensorJob();
        stopMotionDetection();
        stopMqttConnection();
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSensorJob();
        stopMotionDetection();
        //stopMqttConnection();
    }

    public void startMqttConnection(String serverUri, String clientId, final String topic,
                                    final String username, final String password) {
        if (mqttAndroidClient == null) {
            topicPrefix = topic;
            if (!topicPrefix.endsWith("/")) {
                topicPrefix += "/";
            }

            mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), serverUri, clientId);
            mqttAndroidClient.setCallback(new MqttCallbackExtended() {

                @Override
                public void connectionLost(Throwable cause) {
                    Log.i("mqtt_service", "connectionLost ", cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    Log.i("mqtt_service", "messageArrived " + message);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.i("mqtt_service", "deliveryComplete " + token);
                }

                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    Log.i("mqtt_service", "connected to " + serverURI);
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
                        Log.i("mqtt_service", "Failed to connect to: ", exception);
                    }
                });
            } catch (MqttException ex) {
                ex.printStackTrace();
            }
            startSensorJob();
        }
    }

    private void updateSensorData(){
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
        String message = new JSONObject(map).toString();
        publishMessage(message.getBytes(),topicPostfix);
    }

    public void stopMqttConnection(){
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
        if(sensorHandler != null) {
            sensorHandler.removeCallbacksAndMessages(sensorHandlerRunnable);
            sensorHandler = null;
        }
    }

    public class MqttServiceBinder extends Binder {
        HomeDashService getService() {
            return HomeDashService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        startForeground();
    }


    private void startForeground(){
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


    public void startMotionDetection(){
        if(motionDetector==null){
            motionDetector = new MotionDetector(this, null);
            motionDetector.setCheckInterval(Long.valueOf(sharedPreferences.getString(getString(R.string.key_setting_motion_detection_interval),"500")));
            motionDetector.setLeniency(Integer.valueOf(sharedPreferences.getString(getString(R.string.key_setting_motion_detection_leniency),"20")));
            motionDetector.setMinLuma(Integer.valueOf(sharedPreferences.getString(getString(R.string.key_setting_motion_detection_min_luma),"1000")));
            if(motionDetectorCallback==null){
                motionDetectorCallback = new MotionDetectorCallback() {
                    @Override
                    public void onMotionDetected() {
                        switchScreenOn();
                        publishMessage(MOTION_SENSOR_MOTION_DETECTED_JSON.getBytes(Charset.forName("UTF-8")),"motion");
                        Log.i(TAG, "Motion detected");
                    }

                    @Override
                    public void onTooDark() {
                        Log.i(TAG, "Too dark for motion detection");
                    }
                };
            }
            motionDetector.setMotionDetectorCallback(motionDetectorCallback);
            motionDetector.onResume();
        }
    }


    public void stopMotionDetection() {
        if (motionDetector != null) {
            motionDetector.onPause();
            motionDetector = null;
            motionDetectorCallback = null;
        }
    }


    private void switchScreenOn(){
        Intent intent = new Intent(BrowserActivity.BROADCAST_ACTION_SCREEN_ON);
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getApplicationContext());
        bm.sendBroadcast(intent);
    }

    private void reloadPage(){
        Intent intent = new Intent(BrowserActivity.BROADCAST_ACTION_RELOAD_PAGE);
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getApplicationContext());
        bm.sendBroadcast(intent);
    }

    private void clearBrowserCache(){
        Intent intent = new Intent(BrowserActivity.BROADCAST_ACTION_CLEAR_BROWSER_CACHE);
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getApplicationContext());
        bm.sendBroadcast(intent);
    }
}
