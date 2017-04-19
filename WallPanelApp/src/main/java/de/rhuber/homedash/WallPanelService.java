package de.rhuber.homedash;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;

import android.util.Log;

import com.jjoe64.motiondetection.motiondetection.MotionDetector;
import com.jjoe64.motiondetection.motiondetection.MotionDetectorCallback;

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


public class WallPanelService extends Service {
    private static final int ONGOING_NOTIFICATION_ID = 1;
    private static final String MQTT_COMMAND_WAKEUP = "wakeup";
    private static final String MQTT_COMMAND_CLEAR_BROWSER_CACHE = "clearBrowserCache";
    private static final String MQTT_COMMAND_JS_EXEC = "jsExec";
    private static final String MQTT_COMMAND_URL = "url";
    private static final String MQTT_COMMAND_SAVE = "save";
    private static final String MQTT_COMMAND_RELOAD = "reload";

    private final String TAG = WallPanelService.class.getName();
    private final IBinder mBinder = new MqttServiceBinder();
    private MqttAndroidClient mqttAndroidClient;
    private Handler sensorHandler;
    private Runnable sensorHandlerRunnable;
    private SensorReader sensorReader;
    private String topicPrefix;
    @SuppressWarnings("FieldCanBeLocal")

    private final String MOTION_SENSOR_MOTION_DETECTED_JSON ="{\"sensor\":\"cameraMotionDetector\",\"unit\":\"Boolean\",\"value\":\"true\"}";
    private MotionDetector motionDetector;
    private MotionDetectorCallback motionDetectorCallback;

    private PowerManager.WakeLock fullWakeLock;
    private PowerManager.WakeLock partialWakeLock;
    private WifiManager.WifiLock wifiLock;

    private static WallPanelService myInstance;

    private Config config;

    public static WallPanelService getInstance() {
        return myInstance;
    }

    public Bitmap getMotionPicture() {
        try {
            if (motionDetector != null)
                return motionDetector.getLastBitmap();
        }
        catch (Exception ignored) {}

        Bitmap b = Bitmap.createBitmap(320,200,Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        c.drawPaint(paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(20);
        Rect r = new Rect();
        String text = getString(R.string.motion_detection_not_enabled);
        c.getClipBounds(r);
        int cHeight = r.height();
        int cWidth = r.width();
        paint.setTextAlign(Paint.Align.LEFT);
        paint.getTextBounds(text, 0, text.length(), r);
        float x = cWidth / 2f - r.width() / 2f - r.left;
        float y = cHeight / 2f + r.height() / 2f - r.bottom;
        c.drawText(text, x, y, paint);

        return b;
    }

    public WallPanelService() {
        if (myInstance == null)
            myInstance = this;
        else
            throw new RuntimeException("Only instantiate WallPanelService once!");
    }

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

        config = new Config(getApplicationContext());

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

        configurePowerOptions();
        configureMqtt();
        configureMotionDetection(false);

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

    public void configurePowerOptions() {
        Log.d(TAG, "configurePowerOptions Called");

        if (config.getPreventSleep())
        {
            Log.i(TAG, "Acquiring WakeLock to prevent sleep");
            if (!fullWakeLock.isHeld()) fullWakeLock.acquire();
        }
        else
        {
            Log.i(TAG, "Will not prevent sleep");
            if (fullWakeLock.isHeld()) fullWakeLock.release();
        }

        if (config.getKeepWiFiOn()) {
            Log.i(TAG, "Acquiring WakeLock to keep WiFi active");
            if (!wifiLock.isHeld()) wifiLock.acquire();
        }
        else {
            Log.i(TAG, "Will not stop WiFi turning off");
            if (wifiLock.isHeld()) wifiLock.release();
        }
    }

    public void configureMqtt() {
        Log.d(TAG, "configureMqtt Called");
        stopMqttConnection();
        if (config.getMqttEnabled()) {
            startMqttConnection(
                    config.getMqttUrl(),
                    config.getMqttClientId(),
                    config.getMqttTopic(),
                    config.getMqttUsername(),
                    config.getMqttPassword()
            );
        }
    }

    private void startMqttConnection(String serverUri, String clientId, final String topic,
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

        sensorReader.getLightReading(new SensorReader.SensorDataListener() {
            @Override
            public void sensorData(Map<String, String> sensorData) {
                publishSensorMessage(sensorData, "brightness");
            }
        });

        sensorReader.getBatteryReading(new SensorReader.SensorDataListener() {
            @Override
            public void sensorData(Map<String, String> sensorData) {
                publishSensorMessage(sensorData, "battery");
            }
        });

        sensorReader.getPressureReading(new SensorReader.SensorDataListener() {
            @Override
            public void sensorData(Map<String, String> sensorData) {
                publishSensorMessage(sensorData, "pressure");
            }
        });

    }

    private void publishSensorMessage(Map<String, String> map, String topicPostfix){
        Log.d(TAG, "publishSensorMessage Called");
        String message = new JSONObject(map).toString();
        publishMessage(message.getBytes(),topicPostfix);
    }

    private void stopMqttConnection(){
        Log.d(TAG, "stopMqttConnection Called");
        stopSensorJob();
        try {
            if(mqttAndroidClient != null && mqttAndroidClient.isConnected()) {
                mqttAndroidClient.disconnect();
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
        mqttAndroidClient = null;
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
                                config.setLaunchUrl(url);
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
            Integer updateFrequencySeconds = config.getMqttSensorUpdateFrequency();
            if(updateFrequencySeconds!= 0){
                sensorHandler = new Handler();
                final Integer updateFrequencyMilliSeconds = 1000 * updateFrequencySeconds;

                sensorHandlerRunnable = new Runnable() {
                    public void run() {
                        Log.i(TAG, "Updating Sensors");
                        updateSensorData(); //todo there seems to be a race condition here
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

    private class MqttServiceBinder extends Binder {
        WallPanelService getService() {
            Log.d(TAG, "mqttServiceBinder.getService Called");
            return WallPanelService.this;
        }
    }

    private void startForeground(){
        Log.d(TAG, "startForeground Called");
        Intent notificationIntent = new Intent(this, WallPanelService.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            notification = new Notification.Builder(this)
                    .setContentTitle(getText(R.string.wallpanel_service_notification_title))
                    .setContentText(getText(R.string.wallpanel_service_notification_message))
                    .setSmallIcon(R.drawable.ic_home_white_24dp)
                    .setLargeIcon(BitmapFactory.decodeResource(getApplication().getResources(),R.mipmap.wallpanel_icon))
                    .setContentIntent(pendingIntent)
                    .setLocalOnly(true)
                    .build();
        } else {
            notification = new Notification.Builder(this)
                    .setContentTitle(getText(R.string.wallpanel_service_notification_title))
                    .setContentText(getText(R.string.wallpanel_service_notification_message))
                    .setSmallIcon(R.drawable.ic_home_white_24dp)
                    .setLargeIcon(BitmapFactory.decodeResource(getApplication().getResources(),R.mipmap.wallpanel_icon))
                    .setContentIntent(pendingIntent)
                    .build();
        }

        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }

    public void configureMotionDetection(boolean forceStopFirst){
        Log.d(TAG, "updateMotionDetection Called");
        if (forceStopFirst) { stopMotionDetection(); }
        if (config.getCameraMotionDetectionEnabled()) {
            Log.d(TAG, "Motion detection is enabled");
            if (motionDetector == null) {
                final int cameraId = config.getCameraId();
                Log.d(TAG, "Creating Motion Detector object with camera #" + cameraId);
                motionDetector = new MotionDetector(cameraId);
                if (motionDetectorCallback == null) {
                    Log.d(TAG, "Creating Motion Detector Callback");
                    motionDetectorCallback = new MotionDetectorCallback() {
                        @Override
                        public void onMotionDetected() {
                            switchScreenOn();
                            publishMessage(MOTION_SENSOR_MOTION_DETECTED_JSON.getBytes(Charset.forName("UTF-8")), "motion");
                            Log.i(TAG, "Motion detected");

                            Intent intent = new Intent(MotionActivity.BROADCAST_MOTION_DETECTOR_MSG);
                            intent.putExtra("message","Motion Detected!");
                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                        }

                        @Override
                        public void onTooDark() {
                            Log.i(TAG, "Too dark for motion detection");

                            Intent intent = new Intent(MotionActivity.BROADCAST_MOTION_DETECTOR_MSG);
                            intent.putExtra("message","Too dark for motion detection");
                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                        }

                    };
                }
                Log.d(TAG, "Assigning Callback to motionDetector");
                motionDetector.setMotionDetectorCallback(motionDetectorCallback);
                Log.d(TAG, "Calling onResume() on motionDetector");
                motionDetector.onResume();
            }
            Log.d(TAG, "Setting Check Interval");
            motionDetector.setCheckInterval(config.getCameraMotionCheckInterval());
            Log.d(TAG, "Setting Leniency");
            motionDetector.setLeniency(config.getCameraMotionLeniency());
            Log.d(TAG, "Setting MinLuma");
            motionDetector.setMinLuma(config.getCameraMinLuma());
        }
        else {
            stopMotionDetection();
        }
    }

    private void stopMotionDetection() {
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
