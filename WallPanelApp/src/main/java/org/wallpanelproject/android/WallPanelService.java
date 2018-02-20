package org.wallpanelproject.android;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;

import android.util.Log;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.http.body.JSONObjectBody;
import com.koushikdutta.async.http.body.StringBody;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.wallpanelproject.android.R;


public class WallPanelService extends Service {
    private static final int ONGOING_NOTIFICATION_ID = 1;
    public static final String BROADCAST_EVENT_URL_CHANGE = "BROADCAST_EVENT_URL_CHANGE";
    public static final String BROADCAST_EVENT_SCREEN_TOUCH = "BROADCAST_EVENT_SCREEN_TOUCH";
    private final String TAG = WallPanelService.class.getName();

    private SensorReader sensorReader;
    public CameraReader cameraReader;
    private Config config;

    private PowerManager.WakeLock fullWakeLock;
    private PowerManager.WakeLock partialWakeLock;
    private WifiManager.WifiLock wifiLock;
    private KeyguardManager.KeyguardLock keyguardLock;

    private MediaPlayer audioPlayer;
    private boolean audioPlayerBusy;
  
    private Handler brightTimer = new Handler();
    private boolean timerActive = false;

    private MqttAndroidClient mqttAndroidClient;
    private String topicPrefix;

    private AsyncHttpServer httpServer;
    private String currentUrl;

    private final IBinder mBinder = new WallPanelServiceBinder();
    public class WallPanelServiceBinder extends Binder {
        WallPanelService getService() {
            return WallPanelService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate Called");

        cameraReader = new CameraReader(getApplicationContext());
        config = new Config(getApplicationContext());
        currentUrl = config.getAppLaunchUrl();

        sensorReader = new SensorReader(this, getApplicationContext());

        // prepare the lock types we may use
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        //noinspection deprecation
        fullWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                PowerManager.ON_AFTER_RELEASE |
                PowerManager.ACQUIRE_CAUSES_WAKEUP, "fullWakeLock");
        partialWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "partialWakeLock");
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "wifiLock");

        KeyguardManager km = (KeyguardManager) getSystemService(Activity.KEYGUARD_SERVICE);
        keyguardLock = km.newKeyguardLock(KEYGUARD_SERVICE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BROADCAST_EVENT_URL_CHANGE);
        filter.addAction(BROADCAST_EVENT_SCREEN_TOUCH);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(this);
        bm.registerReceiver(mBroadcastReceiver, filter);

        configurePowerOptions();
        configureHttp();
        configureMqtt();
        configureCamera();
        configureAudioPlayer();

        config.startListeningForConfigChanges(prefsChangedListener);

        startForeground();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy Called");

        config.stopListeningForConfigChanges();

        stopCamera();
        stopMqtt();
        stopHttp();
        stopPowerOptions();
    }

    private void startForeground(){
        Log.d(TAG, "startForeground Called");
        Intent notificationIntent = new Intent(this, WallPanelService.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            notification = new Notification.Builder(this)
                    .setContentTitle(getText(R.string.wallpanel_service_notification_title))
                    .setContentText(getText(R.string.wallpanel_service_notification_message))
                    .setSmallIcon(R.drawable.ic_home_white_24dp)
                    .setLargeIcon(BitmapFactory.decodeResource(getApplication().getResources(),R.mipmap.wallpanel_icon))
                    .setContentIntent(pendingIntent)
                    .setLocalOnly(true)
                    .build();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                notification = new Notification.Builder(this)
                        .setContentTitle(getText(R.string.wallpanel_service_notification_title))
                        .setContentText(getText(R.string.wallpanel_service_notification_message))
                        .setSmallIcon(R.drawable.ic_home_white_24dp)
                        .setLargeIcon(BitmapFactory.decodeResource(getApplication().getResources(),R.mipmap.wallpanel_icon))
                        .setContentIntent(pendingIntent)
                        .build();
            }
        }

        if (notification != null)
            startForeground(ONGOING_NOTIFICATION_ID, notification);
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BROADCAST_EVENT_URL_CHANGE)) {
                final String url = intent.getStringExtra(BROADCAST_EVENT_URL_CHANGE);
                if (!url.equals(currentUrl)) {
                    Log.i(TAG, "Url changed to " + url);
                    currentUrl = url;
                    stateChanged();
                }
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF) ||
                    intent.getAction().equals(Intent.ACTION_SCREEN_ON) ||
                    intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
                Log.i(TAG, "Screen state changed");
                stateChanged();
            } else if (intent.getAction().equals(BROADCAST_EVENT_SCREEN_TOUCH)) {
                Log.i(TAG, "Screen touched");
                if (config.getCameraMotionBright()) { setBrightScreen(255); }
                stateChanged();
            }
        }
    };

    private final SharedPreferences.OnSharedPreferenceChangeListener prefsChangedListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            if (s.contains("_mqtt_")) {
                Log.i(TAG, "A MQTT Setting Changed");
                configureMqtt();
            } else if (s.contains("_camera_")) {
                Log.i(TAG, "A Camera Setting Changed");
                configureCamera();
            } else if (s.equals(getApplicationContext().getString(R.string.key_setting_app_preventsleep))) {
                Log.i(TAG, "A Power Option Changed");
                configurePowerOptions();
            } else if (s.contains("_http_")) {
                Log.i(TAG, "A HTTP Option Changed");
                configureHttp();
            }
        }
    };

    //******** Power Related Functions

    private void configurePowerOptions() {
        Log.d(TAG, "configurePowerOptions Called");

        // We always grab partialWakeLock & WifiLock
        Log.i(TAG, "Acquiring Partial Wake Lock and WiFi Lock");
        if (!partialWakeLock.isHeld()) partialWakeLock.acquire();
        if (!wifiLock.isHeld()) wifiLock.acquire();

        if (config.getAppPreventSleep())
        {
            Log.i(TAG, "Acquiring WakeLock to prevent screen sleep");
            if (!fullWakeLock.isHeld()) fullWakeLock.acquire();
        }
        else
        {
            Log.i(TAG, "Will not prevent screen sleep");
            if (fullWakeLock.isHeld()) fullWakeLock.release();
        }

        try {
            keyguardLock.disableKeyguard();
        }
        catch (Exception ex) {
            Log.i(TAG, "Disabling keyguard didn't work");
            ex.printStackTrace();
        }
    }

    private void stopPowerOptions() {
        Log.i(TAG, "Releasing Screen/WiFi Locks");
        if (partialWakeLock.isHeld()) partialWakeLock.release();
        if (fullWakeLock.isHeld()) fullWakeLock.release();
        if (wifiLock.isHeld()) wifiLock.release();

        try {
            keyguardLock.reenableKeyguard();
        }
        catch (Exception ex) {
            Log.i(TAG, "Reenabling keyguard didn't work");
            ex.printStackTrace();
        }
    }


    //******** MQTT Related Functions

    private void configureMqtt() {
        Log.d(TAG, "configureMqtt Called");
        stopMqtt();
        if (config.getMqttEnabled()) {
            startMqttConnection(
                    config.getMqttUrl(),
                    config.getMqttClientId(),
                    config.getMqttBaseTopic(),
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
                        stateChanged();
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.i(TAG, "MQTT connection failure: ", exception);
                    }
                });
            } catch (MqttException ex) {
                ex.printStackTrace();
            }
            sensorReader.startReadings(config.getMqttSensorFrequency());
        }
    }

    private void stopMqtt(){
        Log.d(TAG, "stopMqtt Called");
        sensorReader.stopReadings();
        try {
            if(mqttAndroidClient != null && mqttAndroidClient.isConnected()) {
                mqttAndroidClient.disconnect();
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
        mqttAndroidClient = null;
    }

    public void publishMessage(JSONObject data, String topicPostfix){
        publishMessage(data.toString().getBytes(), topicPostfix);
    }

    private void publishMessage(byte[] message, String topicPostfix){
        Log.d(TAG, "publishMessage Called");
        if (mqttAndroidClient != null && mqttAndroidClient.isConnected()) {
            try {
                String test = new String(message, Charset.forName("UTF-8"));
                Log.i(TAG, "Publishing: [" + topicPrefix+topicPostfix + "]: " + test);
                mqttAndroidClient.publish(topicPrefix+topicPostfix, message, 0, false);
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
                        Log.i(TAG, "messageArrived: " + payload);
                        processCommand(payload);
                    }
                });
                return true;

            } catch (MqttException ex) {
                System.err.println("Exception while subscribing");
                ex.printStackTrace();
            }
        }
        return false;
    }

    @SuppressWarnings("unused")
    private class MqttServiceBinder extends Binder {
        WallPanelService getService() {
            Log.d(TAG, "mqttServiceBinder.getService Called");
            return WallPanelService.this;
        }
    }

    //******** Camera Related Functions

    private void configureCamera(){
        Log.d(TAG, "configureCamera Called");
        stopCamera();
        if (config.getCameraEnabled()) {
            cameraReader.start(config.getCameraCameraId(), config.getCameraProcessingInterval(),
                    this.cameraDetectorCallback);
            if (config.getCameraMotionEnabled()) {
                Log.d(TAG, "Camera Motion detection is enabled");
                cameraReader.startMotionDetection(config.getCameraMotionMinLuma(),
                        config.getCameraMotionLeniency());
            }
            if (config.getCameraFaceEnabled()) {
                Log.d(TAG, "Camera Face detection is enabled");
                cameraReader.startFaceDetection();
            }
            if (config.getCameraQRCodeEnabled()) {
                Log.d(TAG, "Camera QR Code detection is enabled");
                cameraReader.startQRCodeDetection();
            }
        }
    }

    private void configureAudioPlayer() {
        audioPlayer = new MediaPlayer();

        audioPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer audioPlayer) {
                Log.d(TAG, "audioPlayer: File buffered, playing it now");
                audioPlayerBusy = false;
                audioPlayer.start();
            }
        });
        audioPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer audioPlayer) {
                Log.d(TAG, "audioPlayer: Cleanup");
                if (audioPlayer.isPlaying()) {  // should never happen, just in case
                    audioPlayer.stop();
                }
                audioPlayer.reset();
                audioPlayerBusy = false;
            }
        });
        audioPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer audioPlayer, int i, int i1) {
                Log.d(TAG, "audioPlayer: Error playing file");
                audioPlayerBusy = false;
                return false;
            }
        });
    }

    private void stopCamera() {
        Log.d(TAG, "stopCamera Called");
        cameraReader.stop();
    }

    private final CameraDetectorCallback cameraDetectorCallback = new CameraDetectorCallback() {
        @Override
        public void onMotionDetected() {
            Log.i(TAG, "Motion detected");
            if (config.getCameraMotionWake()) { switchScreenOn(); }
            if (config.getCameraMotionBright()) { setBrightScreen(255); }
            sensorReader.doMotionDetected();

            Intent intent = new Intent(CameraTestActivity.BROADCAST_CAMERA_TEST_MSG);
            intent.putExtra("message","Motion Detected!");
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }

        @Override
        public void onTooDark() {
            Log.i(TAG, "Too dark for motion detection");

            Intent intent = new Intent(CameraTestActivity.BROADCAST_CAMERA_TEST_MSG);
            intent.putExtra("message","Too dark for motion detection");
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }

        @Override
        public void onFaceDetected() {
            Log.i(TAG, "Face detected");
            if (config.getCameraFaceWake()) { switchScreenOn(); }
            if (config.getCameraMotionBright()) { setBrightScreen(255); }
            sensorReader.doFaceDetected();

            Intent intent = new Intent(CameraTestActivity.BROADCAST_CAMERA_TEST_MSG);
            intent.putExtra("message","Face Detected!");
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }

        @Override
        public void onQRCode(String data) {
            Log.i(TAG, "QR Code Received");
            sensorReader.doQRCode(data);

            Intent intent = new Intent(CameraTestActivity.BROADCAST_CAMERA_TEST_MSG);
            intent.putExtra("message","QR Code: " + data);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }
    };

    //******** HTTP Related Functions

    private void configureHttp() {
        Log.d(TAG, "configureHttp Called");
        stopHttp();
        if (config.getHttpEnabled()) {
            startHttp();
        }
    }

    private void startHttp() {
        Log.d(TAG, "startHttp Called");
        if (httpServer == null) {
            httpServer = new AsyncHttpServer();

            if (config.getHttpRestEnabled()) {
                httpServer.addAction("POST", "/api/command", new HttpServerRequestCallback() {
                    @Override
                    public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                        boolean result = false;
                        if (request.getBody() instanceof JSONObjectBody) {
                            Log.i(TAG, "POST Json Arrived (command)");
                            result = processCommand(((JSONObjectBody) request.getBody()).get());
                        } else if (request.getBody() instanceof StringBody) {
                            Log.i(TAG, "POST String Arrived (command)");
                            result = processCommand(((StringBody) request.getBody()).get());
                        }

                        JSONObject j = new JSONObject();
                        try {
                            j.put("result", result);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        response.send(j);
                    }
                });

                httpServer.addAction("GET", "/api/state", new HttpServerRequestCallback() {
                    @Override
                    public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                        Log.i(TAG, "GET Arrived (/api/state)");
                        response.send(getState());
                    }
                });

                Log.i(TAG, "Enabled REST Endpoints");
            }

            if (config.getHttpMJPEGEnabled()) {
                startmJpeg();
                httpServer.addAction("GET", "/camera/stream", new HttpServerRequestCallback() {
                    @Override
                    public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                        Log.i(TAG, "GET Arrived (/camera/stream)");
                        startmJpeg(response);
                    }
                });

                Log.i(TAG, "Enabled MJPEG Endpoint");
            }

            httpServer.addAction("*", "*", new HttpServerRequestCallback() {
                @Override
                public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                    Log.i(TAG, "Unhandled Request Arrived");
                    response.code(404);
                    response.send("");
                }
            });


            httpServer.listen(AsyncServer.getDefault(), config.getHttpPort());
            Log.i(TAG, "Started HTTP server on " + config.getHttpPort());
        }
    }

    private void stopHttp() {
        Log.d(TAG, "stopHttp Called");
        if (httpServer != null) {
            stopmJpeg();
            httpServer.stop();
            httpServer = null;
        }
    }

    //******** MJPEG Services

    private final ArrayList<AsyncHttpServerResponse> mJpegSockets = new ArrayList<>();
    private final Handler mJpegHandler = new Handler();

    private void startmJpeg() {
        Log.d(TAG, "startmJpeg Called");
        mJpegHandler.post(sendmJpegDataAll);
    }

    private void stopmJpeg() {
        Log.d(TAG, "stopmJpeg Called");
        mJpegHandler.removeCallbacks(sendmJpegDataAll);
        mJpegSockets.clear();
    }

    private final Runnable sendmJpegDataAll = new Runnable() {
        @Override
        public void run () {
            if (mJpegSockets.size() > 0) {
                final byte[] buffer = cameraReader.getJpeg();
                for (int i = 0; i < mJpegSockets.size(); i++) {
                    final AsyncHttpServerResponse s = mJpegSockets.get(i);
                    final ByteBufferList bb = new ByteBufferList();
                    if (s.isOpen()) {
                        bb.recycle();
                        bb.add(ByteBuffer.wrap("--jpgboundary\r\nContent-Type: image/jpeg\r\n".getBytes()));
                        bb.add(ByteBuffer.wrap(("Content-Length: " + buffer.length + "\r\n\r\n").getBytes()));
                        bb.add(ByteBuffer.wrap(buffer));
                        bb.add(ByteBuffer.wrap("\r\n".getBytes()));
                        s.write(bb);
                    } else {
                        mJpegSockets.remove(i);
                        i--;
                        Log.i(TAG, "MJPEG Session Count is " + mJpegSockets.size());
                    }
                }
            }
            mJpegHandler.postDelayed(this, 100);
        }
    };

    private void startmJpeg(AsyncHttpServerResponse response) {
        Log.d(TAG, "startmJpeg Called");
        if (mJpegSockets.size() < config.getHttpMJPEGMaxStreams()) {
            Log.i(TAG, "Starting new MJPEG stream");
            response.getHeaders().add("Cache-Control", "no-cache");
            response.getHeaders().add("Connection", "close");
            response.getHeaders().add("Pragma", "no-cache");
            response.setContentType("multipart/x-mixed-replace; boundary=--jpgboundary");
            response.code(200);
            response.writeHead();
            mJpegSockets.add(response);
        } else {
            Log.i(TAG, "MJPEG stream limit was reached, not starting");
            response.send("Max streams exceeded");
            response.end();
        }
        Log.i(TAG, "MJPEG Session Count is " + mJpegSockets.size());
    }

    //******** API Functions

    private boolean processCommand(JSONObject commandJson) {
        Log.d(TAG, "processCommand Called");
        try {
            if (commandJson.has("url")) {
                browseUrl(commandJson.getString("url"));
            }
            if (commandJson.has("relaunch")) {
                if (commandJson.getBoolean("relaunch")) {
                    browseUrl(config.getAppLaunchUrl());
                }
            }
            if(commandJson.has("wake")){
                if (commandJson.getBoolean("wake")) { switchScreenOn(); }
            }
            if(commandJson.has("brightness")){
                setBrightScreen(commandJson.getInt("brightness"));
            }
            if(commandJson.has("reload")) {
                if (commandJson.getBoolean("reload")) { reloadPage(); }
            }
            if(commandJson.has("clearCache")){
                if (commandJson.getBoolean("clearCache")) { clearBrowserCache(); }
            }
            if(commandJson.has("eval")) {
                evalJavascript(commandJson.getString("eval"));
            }
            if(commandJson.has("audio")) {
                playAudio(commandJson.getString("audio"));
            }
        }
        catch (JSONException ex) {
            Log.e(TAG, "Invalid JSON passed as a command: " + commandJson.toString());
            return false;
        }
        return true;
    }

    private boolean processCommand(String command) {
        Log.d(TAG, "processCommand Called");
        try {
            return processCommand(new JSONObject(command));
        }
        catch (JSONException ex) {
            Log.e(TAG, "Invalid JSON passed as a command: " + command);
            return false;
        }
    }

    private boolean isScreenOn() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        return Build.VERSION.SDK_INT>= Build.VERSION_CODES.KITKAT_WATCH&&powerManager.isInteractive()|| Build.VERSION.SDK_INT< Build.VERSION_CODES.KITKAT_WATCH&&powerManager.isScreenOn();
    }

    private int getScreenBrightness(){
        Log.d(TAG, "getScreenBrightness called");
        int brightness = 0;
        try {
             brightness = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);  //returns integer value 0-255
        } catch (Exception e) { e.printStackTrace(); }

        return brightness;
    }

    private JSONObject getState() {
        Log.d(TAG, "getState Called");
        JSONObject state = new JSONObject();
        try {
            state.put("currentUrl", currentUrl);
        } catch(JSONException e) { e.printStackTrace(); }
        try {
            state.put("screenOn", isScreenOn());
        }
        catch(JSONException e) { e.printStackTrace(); }
        try {
            state.put("brightness", getScreenBrightness());
        }
        catch(JSONException e) { e.printStackTrace(); }
        return state;
    }

    private void stateChanged() {
        Log.d(TAG, "stateChanged Called");
        publishMessage(getState().toString().getBytes(), "state");
    }

    private void browseUrl(String url) {
        Log.d(TAG, "browseUrl Called");
        Intent intent = new Intent(BrowserActivity.BROADCAST_ACTION_LOAD_URL);
        intent.putExtra(BrowserActivity.BROADCAST_ACTION_LOAD_URL, url);
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getApplicationContext());
        bm.sendBroadcast(intent);
    }

    private void playAudio(String audioUrl) {
        Log.d(TAG, "audioPlayer Called");

        if (audioPlayerBusy) {
            Log.d(TAG, "audioPlayer: Cancelling all previous buffers because new audio was requested");
            audioPlayer.reset();
        }
        else if (audioPlayer.isPlaying()) {
            Log.d(TAG, "audioPlayer: Stopping all media playback because new audio was requested");
            audioPlayer.stop();
            audioPlayer.reset();
        }

        audioPlayerBusy = true;
        try {
            audioPlayer.setDataSource(audioUrl);
        } catch (IOException e) {
            Log.e(TAG, "audioPlayer: An error occurred while preparing audio (" + e.getMessage() + ")");
            audioPlayerBusy = false;
            audioPlayer.reset();
            return;
        }
        Log.d(TAG, "audioPlayer: Buffering " + audioUrl);
        audioPlayer.prepareAsync();
    }

    private void switchScreenOn(){
        Log.d(TAG, "switchScreenOn Called");

        //issue #26 to try to wake from screensaver
        if (fullWakeLock.isHeld()) { fullWakeLock.release(); }
        fullWakeLock.acquire();
        fullWakeLock.release();
        if (config.getAppPreventSleep()) { fullWakeLock.acquire(); }
    }

    private void changeScreenBrightness(int brightness){
        Log.d(TAG, "changeScreenBrightness Called");

        if (getScreenBrightness() != brightness){
            int mode = -1;
            try {
                mode = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE); //this will return integer (0 or 1)
            } catch (Exception e) { e.printStackTrace(); }
            if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                //Automatic mode, need to be in manual to change brightness
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            }
            if (brightness > 0 && brightness < 256) {
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, brightness);
            }
        }
    }

    private void setBrightScreen(int brightness){
        Log.d(TAG, "setBrightScreen called");
        changeScreenBrightness(brightness);
        if (config.getCameraMotionOnTime() > 0) {
            if (!timerActive) {
                timerActive = true;
                brightTimer.postDelayed(dimScreen, config.getCameraMotionOnTime() * 1000);
            } else {
                brightTimer.removeCallbacks(dimScreen);
                brightTimer.postDelayed(dimScreen, config.getCameraMotionOnTime() * 1000);
            }
        }
    }

    private void evalJavascript(String js) {
        Log.d(TAG, "evalJavascript Called");
        Intent intent = new Intent(BrowserActivity.BROADCAST_ACTION_JS_EXEC);
        intent.putExtra(BrowserActivity.BROADCAST_ACTION_JS_EXEC, js);
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getApplicationContext());
        bm.sendBroadcast(intent);
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

    private Runnable dimScreen = new Runnable() {
        @Override
        public void run() {
            // Dim screen, most devices won't go below 5
            changeScreenBrightness(5);
            timerActive = false;
        }
    };
}
