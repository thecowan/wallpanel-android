/*
 * Copyright (c) 2018 ThanksMister LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wallpanelproject.android.network

import android.annotation.SuppressLint
import android.app.Activity
import android.app.KeyguardManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.util.Log

import com.koushikdutta.async.AsyncServer
import com.koushikdutta.async.ByteBufferList
import com.koushikdutta.async.http.body.JSONObjectBody
import com.koushikdutta.async.http.body.StringBody
import com.koushikdutta.async.http.server.AsyncHttpServer
import com.koushikdutta.async.http.server.AsyncHttpServerRequest
import com.koushikdutta.async.http.server.AsyncHttpServerResponse
import com.koushikdutta.async.http.server.HttpServerRequestCallback

import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttMessageListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.json.JSONException
import org.json.JSONObject
import org.wallpanelproject.android.R
import org.wallpanelproject.android.controls.CameraDetectorCallback
import org.wallpanelproject.android.controls.CameraReader
import org.wallpanelproject.android.controls.SensorReader
import org.wallpanelproject.android.persistence.Configuration

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.ArrayList

import timber.log.Timber

import org.wallpanelproject.android.ui.BrowserActivity.Companion.BROADCAST_ACTION_CLEAR_BROWSER_CACHE
import org.wallpanelproject.android.ui.BrowserActivity.Companion.BROADCAST_ACTION_JS_EXEC
import org.wallpanelproject.android.ui.BrowserActivity.Companion.BROADCAST_ACTION_LOAD_URL
import org.wallpanelproject.android.ui.BrowserActivity.Companion.BROADCAST_ACTION_RELOAD_PAGE
import org.wallpanelproject.android.ui.CameraTestActivity.BROADCAST_CAMERA_TEST_MSG


class WallPanelService : Service() {
    
    private var sensorReader: SensorReader? = null
    private var cameraReader: CameraReader? = null
    private var config: Configuration? = null

    private var fullWakeLock: PowerManager.WakeLock? = null
    private var partialWakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var keyguardLock: KeyguardManager.KeyguardLock? = null

    private var audioPlayer: MediaPlayer? = null
    private var audioPlayerBusy: Boolean = false

    private val brightTimer = Handler()
    private var timerActive = false

    private var mqttAndroidClient: MqttAndroidClient? = null
    private var topicPrefix: String? = null

    private var httpServer: AsyncHttpServer? = null
    private var currentUrl: String? = null

    private val mBinder = WallPanelServiceBinder()

    fun getCameraReader() : CameraReader {
        if(cameraReader == null) {
            cameraReader = CameraReader(applicationContext)
        }
        return cameraReader!!;
    }

    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (BROADCAST_EVENT_URL_CHANGE == intent.action) {
                val url = intent.getStringExtra(BROADCAST_EVENT_URL_CHANGE)
                if (url != currentUrl) {
                    Timber.i( "Url changed to $url")
                    currentUrl = url
                    stateChanged()
                }
            } else if (Intent.ACTION_SCREEN_OFF == intent.action ||
                    intent.action == Intent.ACTION_SCREEN_ON ||
                    intent.action == Intent.ACTION_USER_PRESENT) {
                Timber.i( "Screen state changed")
                stateChanged()
            } else if (BROADCAST_EVENT_SCREEN_TOUCH == intent.action) {
                Timber.i( "Screen touched")
                if (config!!.cameraMotionBright) {
                    setBrightScreen(255)
                }
                stateChanged()
            }
        }
    }

    private val prefsChangedListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, s ->
        if (s.contains("_mqtt_")) {
            Timber.i( "A MQTT Setting Changed")
            configureMqtt()
        } else if (s.contains("_camera_")) {
            Timber.i( "A Camera Setting Changed")
            configureCamera()
        } else if (s == applicationContext.getString(R.string.key_setting_app_preventsleep)) {
            Timber.i( "A Power Option Changed")
            configurePowerOptions()
        } else if (s.contains("_http_")) {
            Timber.i( "A HTTP Option Changed")
            configureHttp()
        }
    }

    private val cameraDetectorCallback = object : CameraDetectorCallback {
        override fun onMotionDetected() {
            Timber.i( "Motion detected")
            if (config!!.cameraMotionWake) {
                switchScreenOn()
            }
            if (config!!.cameraMotionBright) {
                setBrightScreen(255)
            }
            sensorReader!!.doMotionDetected()

            val intent = Intent(BROADCAST_CAMERA_TEST_MSG)
            intent.putExtra("message", "Motion Detected!")
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
        }

        override fun onTooDark() {
            Timber.i( "Too dark for motion detection")

            val intent = Intent(BROADCAST_CAMERA_TEST_MSG)
            intent.putExtra("message", "Too dark for motion detection")
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
        }

        override fun onFaceDetected() {
            Timber.i( "Face detected")
            if (config!!.cameraFaceWake) {
                switchScreenOn()
            }
            if (config!!.cameraMotionBright) {
                setBrightScreen(255)
            }
            sensorReader!!.doFaceDetected()

            val intent = Intent(BROADCAST_CAMERA_TEST_MSG)
            intent.putExtra("message", "Face Detected!")
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
        }

        override fun onQRCode(data: String) {
            Timber.i( "QR Code Received")
            sensorReader!!.doQRCode(data)

            val intent = Intent(BROADCAST_CAMERA_TEST_MSG)
            intent.putExtra("message", "QR Code: $data")
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
        }
    }

    //******** MJPEG Services

    private val mJpegSockets = ArrayList<AsyncHttpServerResponse>()
    private val mJpegHandler = Handler()

    private val sendmJpegDataAll = object : Runnable {
        override fun run() {
            if (mJpegSockets.size > 0) {
                val buffer = getCameraReader().jpeg
                var i = 0
                while (i < mJpegSockets.size) {
                    val s = mJpegSockets[i]
                    val bb = ByteBufferList()
                    if (s.isOpen) {
                        bb.recycle()
                        bb.add(ByteBuffer.wrap("--jpgboundary\r\nContent-Type: image/jpeg\r\n".toByteArray()))
                        bb.add(ByteBuffer.wrap(("Content-Length: " + buffer.size + "\r\n\r\n").toByteArray()))
                        bb.add(ByteBuffer.wrap(buffer))
                        bb.add(ByteBuffer.wrap("\r\n".toByteArray()))
                        s.write(bb)
                    } else {
                        mJpegSockets.removeAt(i)
                        i--
                        Timber.i( "MJPEG Session Count is " + mJpegSockets.size)
                    }
                    i++
                }
            }
            mJpegHandler.postDelayed(this, 100)
        }
    }

    private val isScreenOn: Boolean
        get() {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH && powerManager.isInteractive || Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT_WATCH && powerManager.isScreenOn
        }

    private//returns integer value 0-255
    val screenBrightness: Int
        get() {
            Timber.d( "getScreenBrightness called")
            var brightness = 0
            try {
                brightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return brightness
        }

    private val state: JSONObject
        get() {
            Timber.d( "getState Called")
            val state = JSONObject()
            try {
                state.put("currentUrl", currentUrl)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            try {
                state.put("screenOn", isScreenOn)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            try {
                state.put("brightness", screenBrightness)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            return state
        }

    private val dimScreen = Runnable {
        // Dim screen, most devices won't go below 5
        changeScreenBrightness(5)
        timerActive = false
    }

    inner class WallPanelServiceBinder : Binder() {
        val service: WallPanelService
            get() = this@WallPanelService
    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    override fun onCreate() {
        super.onCreate()
        Timber.d( "onCreate Called")

     
        config = Configuration(applicationContext)
        currentUrl = config!!.appLaunchUrl

        sensorReader = SensorReader(this, applicationContext)

        // prepare the lock types we may use
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        fullWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE or PowerManager.ACQUIRE_CAUSES_WAKEUP, "wallPanel:fullWakeLock")
        partialWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wallPanel:partialWakeLock")
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "wallPanel:wifiLock")
        val km = getSystemService(Activity.KEYGUARD_SERVICE) as KeyguardManager
        keyguardLock = km.newKeyguardLock(Context.KEYGUARD_SERVICE)
        // Some Amazon devices are not seeing this permission so we are trying to check
        val permission = "android.permission.DISABLE_KEYGUARD"
        val checkSelfPermission = ContextCompat.checkSelfPermission(this, permission)
        if (checkSelfPermission == PackageManager.PERMISSION_GRANTED) {
            val keyguardLock = km.newKeyguardLock("ALARM_KEYBOARD_LOCK_TAG")
            keyguardLock.disableKeyguard()
        }

        val filter = IntentFilter()
        filter.addAction(BROADCAST_EVENT_URL_CHANGE)
        filter.addAction(BROADCAST_EVENT_SCREEN_TOUCH)
        filter.addAction(Intent.ACTION_SCREEN_ON)
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        filter.addAction(Intent.ACTION_USER_PRESENT)
        val bm = LocalBroadcastManager.getInstance(this)
        bm.registerReceiver(mBroadcastReceiver, filter)

        configurePowerOptions()
        configureHttp()
        configureMqtt()
        configureCamera()
        configureAudioPlayer()

        config!!.startListeningForConfigChanges(prefsChangedListener)

        startForeground()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d( "onDestroy Called")

        config!!.stopListeningForConfigChanges()

        stopCamera()
        stopMqtt()
        stopHttp()
        stopPowerOptions()
    }

    private fun startForeground() {
        Timber.d( "startForeground Called")
        val notificationIntent = Intent(this, WallPanelService::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        var notification: Notification? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            notification = Notification.Builder(this)
                    .setContentTitle(getText(R.string.wallpanel_service_notification_title))
                    .setContentText(getText(R.string.wallpanel_service_notification_message))
                    .setSmallIcon(R.drawable.ic_home_white_24dp)
                    .setLargeIcon(BitmapFactory.decodeResource(application.resources, R.mipmap.wallpanel_icon))
                    .setContentIntent(pendingIntent)
                    .setLocalOnly(true)
                    .build()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                notification = Notification.Builder(this)
                        .setContentTitle(getText(R.string.wallpanel_service_notification_title))
                        .setContentText(getText(R.string.wallpanel_service_notification_message))
                        .setSmallIcon(R.drawable.ic_home_white_24dp)
                        .setLargeIcon(BitmapFactory.decodeResource(application.resources, R.mipmap.wallpanel_icon))
                        .setContentIntent(pendingIntent)
                        .build()
            }
        }

        if (notification != null)
            startForeground(ONGOING_NOTIFICATION_ID, notification)
    }

    //******** Power Related Functions

    private fun configurePowerOptions() {
        Timber.d( "configurePowerOptions Called")

        // We always grab partialWakeLock & WifiLock
        Timber.i( "Acquiring Partial Wake Lock and WiFi Lock")
        if (!partialWakeLock!!.isHeld) partialWakeLock!!.acquire()
        if (!wifiLock!!.isHeld) wifiLock!!.acquire()

        if (config!!.appPreventSleep) {
            Timber.i( "Acquiring WakeLock to prevent screen sleep")
            if (!fullWakeLock!!.isHeld) fullWakeLock!!.acquire()
        } else {
            Timber.i( "Will not prevent screen sleep")
            if (fullWakeLock!!.isHeld) fullWakeLock!!.release()
        }

        try {
            keyguardLock!!.disableKeyguard()
        } catch (ex: Exception) {
            Timber.i( "Disabling keyguard didn't work")
            ex.printStackTrace()
        }
    }

    private fun stopPowerOptions() {
        Timber.i( "Releasing Screen/WiFi Locks")
        if (partialWakeLock!!.isHeld) partialWakeLock!!.release()
        if (fullWakeLock!!.isHeld) fullWakeLock!!.release()
        if (wifiLock!!.isHeld) wifiLock!!.release()

        try {
            keyguardLock!!.reenableKeyguard()
        } catch (ex: Exception) {
            Timber.i( "Reenabling keyguard didn't work")
            ex.printStackTrace()
        }

    }


    //******** MQTT Related Functions

    private fun configureMqtt() {
        Timber.d( "configureMqtt Called")
        stopMqtt()
        if (config!!.mqttEnabled) {
            startMqttConnection(
                    config!!.mqttUrl,
                    config!!.mqttClientId,
                    config!!.mqttBaseTopic,
                    config!!.mqttUsername,
                    config!!.mqttPassword
            )
        }
    }

    private fun startMqttConnection(serverUri: String, clientId: String, topic: String,
                                    username: String, password: String) {
        Timber.d( "startMqttConnection Called")
        if (mqttAndroidClient == null) {
            topicPrefix = topic
            if (!topicPrefix!!.endsWith("/")) {
                topicPrefix += "/"
            }

            mqttAndroidClient = MqttAndroidClient(applicationContext, serverUri, clientId)
            mqttAndroidClient!!.setCallback(object : MqttCallbackExtended {

                override fun connectionLost(cause: Throwable) {
                    Timber.i( "MQTT connectionLost ", cause)
                }

                @Throws(Exception::class)
                override fun messageArrived(topic: String, message: MqttMessage) {
                    Timber.i( "MQTT messageArrived $message")
                }

                override fun deliveryComplete(token: IMqttDeliveryToken) {
                    Timber.i( "MQTT deliveryComplete $token")
                }

                override fun connectComplete(reconnect: Boolean, serverURI: String) {
                    Timber.i( "MQTT connectComplete $serverURI")
                }
            })
            val mqttConnectOptions = MqttConnectOptions()
            if (username.length > 0) {
                mqttConnectOptions.userName = username
                mqttConnectOptions.password = password.toCharArray()
            }
            mqttConnectOptions.isAutomaticReconnect = true
            mqttConnectOptions.isCleanSession = false
            try {
                mqttAndroidClient!!.connect(mqttConnectOptions, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken) {
                        val disconnectedBufferOptions = DisconnectedBufferOptions()
                        disconnectedBufferOptions.isBufferEnabled = true
                        disconnectedBufferOptions.bufferSize = 100
                        disconnectedBufferOptions.isPersistBuffer = false
                        disconnectedBufferOptions.isDeleteOldestMessages = false
                        mqttAndroidClient!!.setBufferOpts(disconnectedBufferOptions)
                        subscribeToTopic(topicPrefix!! + "command")
                        stateChanged()
                    }

                    override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                        Timber.i( "MQTT connection failure: ", exception)
                    }
                })
            } catch (ex: MqttException) {
                ex.printStackTrace()
            }

            sensorReader!!.startReadings(config!!.mqttSensorFrequency)
        }
    }

    private fun stopMqtt() {
        Timber.d( "stopMqtt Called")
        sensorReader!!.stopReadings()
        try {
            if (mqttAndroidClient != null && mqttAndroidClient!!.isConnected) {
                mqttAndroidClient!!.disconnect()
            }
        } catch (e: MqttException) {
            e.printStackTrace()
        }

        mqttAndroidClient = null
    }

    fun publishMessage(data: JSONObject, topicPostfix: String) {
        publishMessage(data.toString().toByteArray(), topicPostfix)
    }

    private fun publishMessage(message: ByteArray, topicPostfix: String) {
        Timber.d( "publishMessage Called")
        if (mqttAndroidClient != null && mqttAndroidClient!!.isConnected) {
            try {
                val test = String(message, Charset.forName("UTF-8"))
                Timber.i( "Publishing: [$topicPrefix$topicPostfix]: $test")
                mqttAndroidClient!!.publish(topicPrefix!! + topicPostfix, message, 0, false)
            } catch (e: MqttException) {
                e.printStackTrace()
            }

        }
    }

    private fun subscribeToTopic(subscriptionTopic: String): Boolean {
        Timber.d( "subscribeToTopic Called")
        if (mqttAndroidClient!!.isConnected) {
            try {
                mqttAndroidClient!!.subscribe(subscriptionTopic, 0, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken) {
                        Timber.i( "subscribed to: $subscriptionTopic")
                    }

                    override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                        Timber.i( "Failed to subscribe to: $subscriptionTopic")
                    }
                })

                mqttAndroidClient!!.subscribe(subscriptionTopic, 0) { topic, message ->
                    val payload = String(message.payload, Charset.forName("UTF-8"))
                    Timber.i( "messageArrived: $payload")
                    processCommand(payload)
                }
                return true

            } catch (ex: MqttException) {
                System.err.println("Exception while subscribing")
                ex.printStackTrace()
            }

        }
        return false
    }

    private inner class MqttServiceBinder : Binder() {
        internal val service: WallPanelService
            get() {
                Timber.d( "mqttServiceBinder.getService Called")
                return this@WallPanelService
            }
    }

    //******** Camera Related Functions

    private fun configureCamera() {
        Timber.d( "configureCamera Called")
        stopCamera()
        if (config!!.cameraEnabled) {
            getCameraReader().start(config!!.cameraCameraId, config!!.cameraProcessingInterval,
                    this.cameraDetectorCallback)
            if (config!!.cameraMotionEnabled) {
                Timber.d( "Camera Motion detection is enabled")
                getCameraReader().startMotionDetection(config!!.cameraMotionMinLuma,
                        config!!.cameraMotionLeniency)
            }
            if (config!!.cameraFaceEnabled) {
                Timber.d( "Camera Face detection is enabled")
                getCameraReader().startFaceDetection()
            }
            if (config!!.cameraQRCodeEnabled) {
                Timber.d( "Camera QR Code detection is enabled")
                getCameraReader().startQRCodeDetection()
            }
        }
    }

    private fun configureAudioPlayer() {
        audioPlayer = MediaPlayer()

        audioPlayer!!.setOnPreparedListener { audioPlayer ->
            Timber.d( "audioPlayer: File buffered, playing it now")
            audioPlayerBusy = false
            audioPlayer.start()
        }
        audioPlayer!!.setOnCompletionListener { audioPlayer ->
            Timber.d( "audioPlayer: Cleanup")
            if (audioPlayer.isPlaying) {  // should never happen, just in case
                audioPlayer.stop()
            }
            audioPlayer.reset()
            audioPlayerBusy = false
        }
        audioPlayer!!.setOnErrorListener { audioPlayer, i, i1 ->
            Timber.d( "audioPlayer: Error playing file")
            audioPlayerBusy = false
            false
        }
    }

    private fun stopCamera() {
        Timber.d( "stopCamera Called")
        getCameraReader().stop()
    }

    //******** HTTP Related Functions

    private fun configureHttp() {
        Timber.d( "configureHttp Called")
        stopHttp()
        if (config!!.httpEnabled) {
            startHttp()
        }
    }

    private fun startHttp() {
        Timber.d( "startHttp Called")
        if (httpServer == null) {
            httpServer = AsyncHttpServer()

            if (config!!.httpRestEnabled) {
                httpServer!!.addAction("POST", "/api/command") { request, response ->
                    var result = false
                    if (request.body is JSONObjectBody) {
                        Timber.i( "POST Json Arrived (command)")
                        result = processCommand((request.body as JSONObjectBody).get())
                    } else if (request.body is StringBody) {
                        Timber.i( "POST String Arrived (command)")
                        result = processCommand((request.body as StringBody).get())
                    }

                    val j = JSONObject()
                    try {
                        j.put("result", result)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }

                    response.send(j)
                }

                httpServer!!.addAction("GET", "/api/state") { request, response ->
                    Timber.i( "GET Arrived (/api/state)")
                    response.send(state)
                }

                Timber.i( "Enabled REST Endpoints")
            }

            if (config!!.httpMJPEGEnabled) {
                startmJpeg()
                httpServer!!.addAction("GET", "/camera/stream") { request, response ->
                    Timber.i( "GET Arrived (/camera/stream)")
                    startmJpeg(response)
                }

                Timber.i( "Enabled MJPEG Endpoint")
            }

            httpServer!!.addAction("*", "*") { request, response ->
                Timber.i( "Unhandled Request Arrived")
                response.code(404)
                response.send("")
            }


            httpServer!!.listen(AsyncServer.getDefault(), config!!.httpPort)
            Timber.i( "Started HTTP server on " + config!!.httpPort)
        }
    }

    private fun stopHttp() {
        Timber.d( "stopHttp Called")
        if (httpServer != null) {
            stopmJpeg()
            httpServer!!.stop()
            httpServer = null
        }
    }

    private fun startmJpeg() {
        Timber.d( "startmJpeg Called")
        mJpegHandler.post(sendmJpegDataAll)
    }

    private fun stopmJpeg() {
        Timber.d( "stopmJpeg Called")
        mJpegHandler.removeCallbacks(sendmJpegDataAll)
        mJpegSockets.clear()
    }

    private fun startmJpeg(response: AsyncHttpServerResponse) {
        Timber.d( "startmJpeg Called")
        if (mJpegSockets.size < config!!.httpMJPEGMaxStreams) {
            Timber.i( "Starting new MJPEG stream")
            response.headers.add("Cache-Control", "no-cache")
            response.headers.add("Connection", "close")
            response.headers.add("Pragma", "no-cache")
            response.setContentType("multipart/x-mixed-replace; boundary=--jpgboundary")
            response.code(200)
            response.writeHead()
            mJpegSockets.add(response)
        } else {
            Timber.i( "MJPEG stream limit was reached, not starting")
            response.send("Max streams exceeded")
            response.end()
        }
        Timber.i( "MJPEG Session Count is " + mJpegSockets.size)
    }

    //******** API Functions

    private fun processCommand(commandJson: JSONObject): Boolean {
        Timber.d( "processCommand Called")
        try {
            if (commandJson.has("url")) {
                browseUrl(commandJson.getString("url"))
            }
            if (commandJson.has("relaunch")) {
                if (commandJson.getBoolean("relaunch")) {
                    browseUrl(config!!.appLaunchUrl)
                }
            }
            if (commandJson.has("wake")) {
                if (commandJson.getBoolean("wake")) {
                    switchScreenOn()
                }
            }
            if (commandJson.has("brightness")) {
                setBrightScreen(commandJson.getInt("brightness"))
            }
            if (commandJson.has("reload")) {
                if (commandJson.getBoolean("reload")) {
                    reloadPage()
                }
            }
            if (commandJson.has("clearCache")) {
                if (commandJson.getBoolean("clearCache")) {
                    clearBrowserCache()
                }
            }
            if (commandJson.has("eval")) {
                evalJavascript(commandJson.getString("eval"))
            }
            if (commandJson.has("audio")) {
                playAudio(commandJson.getString("audio"))
            }
        } catch (ex: JSONException) {
            Timber.e( "Invalid JSON passed as a command: " + commandJson.toString())
            return false
        }

        return true
    }

    private fun processCommand(command: String): Boolean {
        Timber.d( "processCommand Called")
        try {
            return processCommand(JSONObject(command))
        } catch (ex: JSONException) {
            Timber.e( "Invalid JSON passed as a command: $command")
            return false
        }
    }

    private fun stateChanged() {
        Timber.d( "stateChanged Called")
        publishMessage(state.toString().toByteArray(), "state")
    }

    private fun browseUrl(url: String) {
        Timber.d( "browseUrl Called")
        val intent = Intent(BROADCAST_ACTION_LOAD_URL)
        intent.putExtra(BROADCAST_ACTION_LOAD_URL, url)
        val bm = LocalBroadcastManager.getInstance(applicationContext)
        bm.sendBroadcast(intent)
    }

    private fun playAudio(audioUrl: String) {
        Timber.d( "audioPlayer Called")
        if (audioPlayerBusy) {
            Timber.d( "audioPlayer: Cancelling all previous buffers because new audio was requested")
            audioPlayer!!.reset()
        } else if (audioPlayer!!.isPlaying) {
            Timber.d( "audioPlayer: Stopping all media playback because new audio was requested")
            audioPlayer!!.stop()
            audioPlayer!!.reset()
        }

        audioPlayerBusy = true
        try {
            audioPlayer!!.setDataSource(audioUrl)
        } catch (e: IOException) {
            Timber.e( "audioPlayer: An error occurred while preparing audio (" + e.message + ")")
            audioPlayerBusy = false
            audioPlayer!!.reset()
            return
        }

        Timber.d( "audioPlayer: Buffering $audioUrl")
        audioPlayer!!.prepareAsync()
    }

    @SuppressLint("WakelockTimeout")
    private fun switchScreenOn() {
        Timber.d( "switchScreenOn Called")
        if (config!!.appPreventSleep && !fullWakeLock!!.isHeld) {
            fullWakeLock!!.acquire()
        } else if (!fullWakeLock!!.isHeld) {
            fullWakeLock!!.acquire(3000)
        }
    }

    private fun changeScreenBrightness(brightness: Int) {
        Timber.d( "changeScreenBrightness Called")
        if (screenBrightness != brightness) {
            var mode = -1
            try {
                mode = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE) //this will return integer (0 or 1)
            } catch (e: Settings.SettingNotFoundException) {
                Timber.e(e.message)
            }

            try {
                if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                    //Automatic mode, need to be in manual to change brightness
                    Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                }
                if (brightness > 0 && brightness < 256) {
                    Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness)
                }
            } catch (e: SecurityException) {
                Timber.e(e.message)
            }
        }
    }

    private fun setBrightScreen(brightness: Int) {
        Timber.d( "setBrightScreen called")
        changeScreenBrightness(brightness)
        if (config!!.cameraMotionOnTime > 0) {
            if (!timerActive) {
                timerActive = true
                brightTimer.postDelayed(dimScreen, (config!!.cameraMotionOnTime * 1000).toLong())
            } else {
                brightTimer.removeCallbacks(dimScreen)
                brightTimer.postDelayed(dimScreen, (config!!.cameraMotionOnTime * 1000).toLong())
            }
        }
    }

    private fun evalJavascript(js: String) {
        Timber.d( "evalJavascript Called")
        val intent = Intent(BROADCAST_ACTION_JS_EXEC)
        intent.putExtra(BROADCAST_ACTION_JS_EXEC, js)
        val bm = LocalBroadcastManager.getInstance(applicationContext)
        bm.sendBroadcast(intent)
    }

    private fun reloadPage() {
        Timber.d( "reloadPage Called")
        val intent = Intent(BROADCAST_ACTION_RELOAD_PAGE)
        val bm = LocalBroadcastManager.getInstance(applicationContext)
        bm.sendBroadcast(intent)
    }

    private fun clearBrowserCache() {
        Timber.d( "clearBrowserCache Called")
        val intent = Intent(BROADCAST_ACTION_CLEAR_BROWSER_CACHE)
        val bm = LocalBroadcastManager.getInstance(applicationContext)
        bm.sendBroadcast(intent)
    }

    companion object {
        private val ONGOING_NOTIFICATION_ID = 1
        const val BROADCAST_EVENT_URL_CHANGE = "BROADCAST_EVENT_URL_CHANGE"
        const val BROADCAST_EVENT_SCREEN_TOUCH = "BROADCAST_EVENT_SCREEN_TOUCH"
    }
}
