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

package com.thanksmister.iot.wallpanel.network

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.app.Notification
import android.app.PendingIntent
import android.arch.lifecycle.LifecycleService
import android.arch.lifecycle.Observer
import android.content.*
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.wifi.WifiManager
import android.os.*
import android.provider.Settings
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.widget.Toast
import com.koushikdutta.async.AsyncServer
import com.koushikdutta.async.ByteBufferList
import com.koushikdutta.async.http.body.JSONObjectBody
import com.koushikdutta.async.http.body.StringBody
import com.koushikdutta.async.http.server.AsyncHttpServer
import com.koushikdutta.async.http.server.AsyncHttpServerResponse
import com.thanksmister.iot.wallpanel.R
import com.thanksmister.iot.wallpanel.modules.*
import com.thanksmister.iot.wallpanel.persistence.Configuration
import com.thanksmister.iot.wallpanel.ui.activities.BrowserActivity.Companion.BROADCAST_ACTION_CLEAR_BROWSER_CACHE
import com.thanksmister.iot.wallpanel.ui.activities.BrowserActivity.Companion.BROADCAST_ACTION_JS_EXEC
import com.thanksmister.iot.wallpanel.ui.activities.BrowserActivity.Companion.BROADCAST_ACTION_LOAD_URL
import com.thanksmister.iot.wallpanel.ui.activities.BrowserActivity.Companion.BROADCAST_ACTION_RELOAD_PAGE
import com.thanksmister.iot.wallpanel.ui.activities.WelcomeActivity
import com.thanksmister.iot.wallpanel.utils.MqttUtils
import com.thanksmister.iot.wallpanel.utils.NotificationUtils
import dagger.android.AndroidInjection
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class WallPanelService : LifecycleService(), MQTTModule.MQTTListener {

    @Inject
    lateinit var configuration: Configuration
    @Inject
    lateinit var cameraReader: CameraReader
    @Inject
    lateinit var sensorReader: SensorReader
    @Inject
    lateinit var mqttOptions: MQTTOptions

    private var motionDetectedCountdown = 0
    private var faceDetectedCountdown = 0
    private val mJpegSockets = ArrayList<AsyncHttpServerResponse>()
    private var fullWakeLock: PowerManager.WakeLock? = null
    private var partialWakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var keyguardLock: KeyguardManager.KeyguardLock? = null
    private var audioPlayer: MediaPlayer? = null
    private var audioPlayerBusy: Boolean = false
    private val brightTimer = Handler()
    private var timerActive = false
    private var httpServer: AsyncHttpServer? = null
    private val mBinder = WallPanelServiceBinder()
    private val motionHandler = Handler()
    private val faceHandler = Handler()
    private var textToSpeechModule: TextToSpeechModule? = null
    private var mqttModule: MQTTModule? = null
    private var connectionLiveData: ConnectionLiveData? = null
    private var hasNetwork = AtomicBoolean(true)

    inner class WallPanelServiceBinder : Binder() {
        val service: WallPanelService
            get() = this@WallPanelService
    }

    override fun onCreate() {
        super.onCreate()

        Timber.d("onCreate")

        AndroidInjection.inject(this)

        // prepare the lock types we may use
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        fullWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE or PowerManager.ACQUIRE_CAUSES_WAKEUP, "wallPanel:fullWakeLock")
        partialWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wallPanel:partialWakeLock")

        // wifi lock
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "wallPanel:wifiLock")

        // Some Amazon devices are not seeing this permission so we are trying to check
        val permission = "android.permission.DISABLE_KEYGUARD"
        val checkSelfPermission = ContextCompat.checkSelfPermission(this@WallPanelService, permission)
        if (checkSelfPermission == PackageManager.PERMISSION_GRANTED) {
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardLock = keyguardManager.newKeyguardLock("ALARM_KEYBOARD_LOCK_TAG")
            keyguardLock!!.disableKeyguard()
        }

        val filter = IntentFilter()
        filter.addAction(BROADCAST_EVENT_URL_CHANGE)
        filter.addAction(BROADCAST_EVENT_SCREEN_TOUCH)
        filter.addAction(Intent.ACTION_SCREEN_ON)
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        filter.addAction(Intent.ACTION_USER_PRESENT)
        val bm = LocalBroadcastManager.getInstance(this)
        bm.registerReceiver(mBroadcastReceiver, filter)

        configureMqtt()
        configurePowerOptions()
        startHttp()
        configureCamera()
        configureAudioPlayer()
        startForeground()
        configureTextToSpeach()
        startSensors()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraReader.stopCamera()
        sensorReader.stopReadings()
        stopHttp()
        stopPowerOptions()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return mBinder
    }

    private val isScreenOn: Boolean
        get() {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH && powerManager.isInteractive || Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT_WATCH && powerManager.isScreenOn
        }

    private val screenBrightness: Int
        get() {
            Timber.d("getScreenBrightness")
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
            Timber.d("getState")
            val state = JSONObject()
            try {
                state.put("currentUrl", configuration.appLaunchUrl)
                state.put("screenOn", isScreenOn)
                state.put("brightness", screenBrightness)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            return state
        }

    /**
     * Dim screen, most devices won't go below 5
     */
    private val dimScreen = Runnable {
        changeScreenBrightness(5)
        timerActive = false
    }

    private fun startForeground() {
        Timber.d("startForeground")

        // make a continuously running notification
        val notificationUtils = NotificationUtils(applicationContext, application.resources)
        val notification = notificationUtils.createNotification(getString(R.string.wallpanel_service_notification_title),
                getString(R.string.wallpanel_service_notification_message))
        if (notification != null) {
            startForeground(ONGOING_NOTIFICATION_ID, notification)
        }

        /*val notificationIntent = Intent(applicationContext, WallPanelService::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        var notification: Notification? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            notification = Notification.Builder(this)
                    .setContentTitle(getText(R.string.wallpanel_service_notification_title))
                    .setContentText(getText(R.string.wallpanel_service_notification_message))
                    .setSmallIcon(R.drawable.ic_dashboard)
                    .setLargeIcon(BitmapFactory.decodeResource(application.resources, R.mipmap.ic_launcher))
                    .setContentIntent(pendingIntent)
                    .setLocalOnly(true)
                    .build()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                notification = Notification.Builder(this)
                        .setContentTitle(getText(R.string.wallpanel_service_notification_title))
                        .setContentText(getText(R.string.wallpanel_service_notification_message))
                        .setSmallIcon(R.drawable.ic_dashboard)
                        .setLargeIcon(BitmapFactory.decodeResource(application.resources, R.mipmap.ic_launcher))
                        .setContentIntent(pendingIntent)
                        .build()
            }
        }
*/
        if (notification != null) {
            startForeground(ONGOING_NOTIFICATION_ID, notification)
        }
        // listen for network connectivity changes
        connectionLiveData = ConnectionLiveData(this)
        connectionLiveData?.observe(this, Observer { connected ->
            if(connected!!) {
                handleNetworkConnect()
            } else {
                handleNetworkDisconnect()
            }
        })
    }

    private fun handleNetworkConnect() {
        Timber.d("handleNetworkConnect")
        if (mqttModule != null && !hasNetwork.get()) {
            mqttModule?.restart()
        }
        hasNetwork.set(true)
    }

    private fun handleNetworkDisconnect() {
        Timber.d("handleNetworkDisconnect")
        if (mqttModule != null && hasNetwork.get()) {
            mqttModule?.pause()
        }
        hasNetwork.set(false)
    }

    @SuppressLint("WakelockTimeout")
    private fun configurePowerOptions() {
        Timber.d("configurePowerOptions")

        // We always grab partialWakeLock & WifiLock
        Timber.i("Acquiring Partial Wake Lock and WiFi Lock")
        if (!partialWakeLock!!.isHeld) partialWakeLock!!.acquire()
        if (!wifiLock!!.isHeld) wifiLock!!.acquire()

        if (configuration.appPreventSleep) {
            Timber.i("Acquiring WakeLock to prevent screen sleep")
            if (!fullWakeLock!!.isHeld) fullWakeLock!!.acquire()
        } else {
            Timber.i("Will not prevent screen sleep")
            if (fullWakeLock!!.isHeld) fullWakeLock!!.release()
        }
        try {
            keyguardLock!!.disableKeyguard()
        } catch (ex: Exception) {
            Timber.i("Disabling keyguard didn't work")
            ex.printStackTrace()
        }
    }

    private fun stopPowerOptions() {
        Timber.i("Releasing Screen/WiFi Locks")
        if (partialWakeLock!!.isHeld) partialWakeLock!!.release()
        if (fullWakeLock!!.isHeld) fullWakeLock!!.release()
        if (wifiLock!!.isHeld) wifiLock!!.release()
        try {
            // TODO fix keygaurd
            keyguardLock!!.reenableKeyguard()
        } catch (ex: Exception) {
            Timber.i("Enabling keyguard didn't work")
            ex.printStackTrace()
        }
    }

    private fun startSensors() {
        if (configuration.sensorsEnabled && mqttOptions.isValid) {
            sensorReader.startReadings(configuration.mqttSensorFrequency, sensorCallback)
        }
    }

    private fun configureMqtt() {
        Timber.d("configureMqtt")
        if (mqttModule == null && mqttOptions.isValid) {
            mqttModule = MQTTModule(this@WallPanelService.applicationContext, mqttOptions,this@WallPanelService)
            lifecycle.addObserver(mqttModule!!)
            publishMessage(MqttUtils.TOPIC_STATE, state.toString())
        }
    }

    override fun onMQTTDisconnect() {
        Timber.d("onMQTTDisconnect")
        Toast.makeText(this, getString(R.string.error_mqtt_connection), Toast.LENGTH_SHORT).show()
        //mqttModule!!.restart()
    }

    override fun onMQTTException(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        //mqttModule!!.restart()
    }

    override fun onMQTTMessage(id: String, topic: String, payload: String) {
        Timber.i("onMQTTMessage: $payload")
        processCommand(payload)
    }

    private fun publishMessage(command: String, data: JSONObject) {
        publishMessage(command, data.toString())
    }

    private fun publishMessage(command: String, message: String) {
        Timber.d("publishMessage")
        if(mqttModule != null) {
            mqttModule!!.publish(command, message)
        }
    }

    private fun configureCamera() {
        Timber.d("configureCamera ${configuration.cameraEnabled}")
        if (configuration.cameraEnabled) {
            cameraReader.startCamera(cameraDetectorCallback, configuration)
        }
    }

    private fun configureTextToSpeach() {
        Timber.d("configureTextToSpeach")
        if (textToSpeechModule == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textToSpeechModule = TextToSpeechModule(this)
            lifecycle.addObserver(textToSpeechModule!!)
        }
    }

    private fun configureAudioPlayer() {
        audioPlayer = MediaPlayer()
        audioPlayer!!.setOnPreparedListener { audioPlayer ->
            Timber.d("audioPlayer: File buffered, playing it now")
            audioPlayerBusy = false
            audioPlayer.start()
        }
        audioPlayer!!.setOnCompletionListener { audioPlayer ->
            Timber.d("audioPlayer: Cleanup")
            if (audioPlayer.isPlaying) {  // should never happen, just in case
                audioPlayer.stop()
            }
            audioPlayer.reset()
            audioPlayerBusy = false
        }
        audioPlayer!!.setOnErrorListener { audioPlayer, i, i1 ->
            Timber.d("audioPlayer: Error playing file")
            audioPlayerBusy = false
            false
        }
    }

    private fun startHttp() {
        Timber.d("startHttp")
        if (httpServer == null && (configuration.httpEnabled || configuration.httpMJPEGEnabled)) {
            httpServer = AsyncHttpServer()
            if (configuration.httpRestEnabled) {

                httpServer!!.addAction("POST", "/api/command") { request, response ->
                    var result = false
                    if (request.body is JSONObjectBody) {
                        Timber.i("POST Json Arrived (command)")
                        result = processCommand((request.body as JSONObjectBody).get())
                    } else if (request.body is StringBody) {
                        Timber.i("POST String Arrived (command)")
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
                    Timber.i("GET Arrived (/api/state)")
                    response.send(state)
                }
                Timber.i("Enabled REST Endpoints")
            }

            if (configuration.httpMJPEGEnabled) {
                startMJPEG()
                httpServer!!.addAction("GET", "/camera/stream") { _, response ->
                    Timber.i("GET Arrived (/camera/stream)")
                    startMJPEG(response)
                }
                Timber.i("Enabled MJPEG Endpoint")
            }

            httpServer!!.addAction("*", "*") { request, response ->
                Timber.i("Unhandled Request Arrived")
                response.code(404)
                response.send("")
            }

            httpServer!!.listen(AsyncServer.getDefault(), configuration.httpPort)
            Timber.i("Started HTTP server on " + configuration.httpPort)
        }
    }

    private fun stopHttp() {
        Timber.d("stopHttp")
        if (httpServer != null) {
            stopMJPEG()
            httpServer!!.stop()
            httpServer = null
        }
    }

    private fun startMJPEG() {
        Timber.d("startMJPEG")
        cameraReader.getJpeg().observe(this, Observer { jpeg ->
            if (mJpegSockets.size > 0 && jpeg != null) {
                Timber.d("mJpegSockets")
                var i = 0
                while (i < mJpegSockets.size) {
                    val s = mJpegSockets[i]
                    val bb = ByteBufferList()
                    if (s.isOpen) {
                        bb.recycle()
                        bb.add(ByteBuffer.wrap("--jpgboundary\r\nContent-Type: image/jpeg\r\n".toByteArray()))
                        bb.add(ByteBuffer.wrap(("Content-Length: " + jpeg.size + "\r\n\r\n").toByteArray()))
                        bb.add(ByteBuffer.wrap(jpeg))
                        bb.add(ByteBuffer.wrap("\r\n".toByteArray()))
                        s.write(bb)
                    } else {
                        mJpegSockets.removeAt(i)
                        i--
                        Timber.i("MJPEG Session Count is " + mJpegSockets.size)
                    }
                    i++
                }
            }
        })
    }

    private fun stopMJPEG() {
        Timber.d("stopMJPEG Called")
        mJpegSockets.clear()
    }

    private fun startMJPEG(response: AsyncHttpServerResponse) {
        Timber.d("startmJpeg Called")
        if (mJpegSockets.size < configuration.httpMJPEGMaxStreams) {
            Timber.i("Starting new MJPEG stream")
            response.headers.add("Cache-Control", "no-cache")
            response.headers.add("Connection", "close")
            response.headers.add("Pragma", "no-cache")
            response.setContentType("multipart/x-mixed-replace; boundary=--jpgboundary")
            response.code(200)
            response.writeHead()
            mJpegSockets.add(response)
        } else {
            Timber.i("MJPEG stream limit was reached, not starting")
            response.send("Max streams exceeded")
            response.end()
        }
        Timber.i("MJPEG Session Count is " + mJpegSockets.size)
    }

    private fun processCommand(commandJson: JSONObject): Boolean {
        Timber.d("processCommand ${commandJson.toString()}")
        try {
            if (commandJson.has("url")) {
                browseUrl(commandJson.getString("url"))
            }
            if (commandJson.has("relaunch")) {
                if (commandJson.getBoolean("relaunch")) {
                    browseUrl(configuration.appLaunchUrl)
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
            if (commandJson.has("speak")) {
                speakMessage(commandJson.getString("speak"))
            }
        } catch (ex: JSONException) {
            Timber.e("Invalid JSON passed as a command: " + commandJson.toString())
            return false
        }

        return true
    }

    private fun processCommand(command: String): Boolean {
        Timber.d("processCommand Called")
        try {
            return processCommand(JSONObject(command))
        } catch (ex: JSONException) {
            Timber.e("Invalid JSON passed as a command: $command")
            return false
        }
    }

    private fun browseUrl(url: String) {
        Timber.d("browseUrl")
        val intent = Intent(BROADCAST_ACTION_LOAD_URL)
        intent.putExtra(BROADCAST_ACTION_LOAD_URL, url)
        val bm = LocalBroadcastManager.getInstance(applicationContext)
        bm.sendBroadcast(intent)
    }

    private fun playAudio(audioUrl: String) {
        Timber.d("audioPlayer")
        if (audioPlayerBusy) {
            Timber.d("audioPlayer: Cancelling all previous buffers because new audio was requested")
            audioPlayer!!.reset()
        } else if (audioPlayer!!.isPlaying) {
            Timber.d("audioPlayer: Stopping all media playback because new audio was requested")
            audioPlayer!!.stop()
            audioPlayer!!.reset()
        }

        audioPlayerBusy = true
        try {
            audioPlayer!!.setDataSource(audioUrl)
        } catch (e: IOException) {
            Timber.e("audioPlayer: An error occurred while preparing audio (" + e.message + ")")
            audioPlayerBusy = false
            audioPlayer!!.reset()
            return
        }

        Timber.d("audioPlayer: Buffering $audioUrl")
        audioPlayer!!.prepareAsync()
    }

    private fun speakMessage(message: String) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            if (textToSpeechModule != null) {
                Timber.d("speakMessage $message")
                textToSpeechModule!!.speakText(message)
            }
        }
    }

    @SuppressLint("WakelockTimeout")
    private fun switchScreenOn() {
        Timber.d("switchScreenOn")
        if (configuration.appPreventSleep && !fullWakeLock!!.isHeld) {
            fullWakeLock!!.acquire()
        } else if (!fullWakeLock!!.isHeld) {
            fullWakeLock!!.acquire(3000)
        }
    }

    private fun changeScreenBrightness(brightness: Int) {
        Timber.d("changeScreenBrightness")
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
                if (brightness in 1..255) {
                    Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness)
                }
            } catch (e: SecurityException) {
                Timber.e(e.message)
            }
        }
    }

    private fun setBrightScreen(brightness: Int) {
        Timber.d("setBrightScreen $brightness")
        changeScreenBrightness(brightness)
        if (configuration.cameraMotionOnTime > 0 && configuration.cameraMotionBright) {
            if (!timerActive) {
                timerActive = true
                brightTimer.postDelayed(dimScreen, (configuration.cameraMotionOnTime * 1000).toLong())
            } else {
                brightTimer.removeCallbacks(dimScreen)
                brightTimer.postDelayed(dimScreen, (configuration.cameraMotionOnTime * 1000).toLong())
            }
        }
    }

    private fun evalJavascript(js: String) {
        Timber.d("evalJavascript")
        val intent = Intent(BROADCAST_ACTION_JS_EXEC)
        intent.putExtra(BROADCAST_ACTION_JS_EXEC, js)
        val bm = LocalBroadcastManager.getInstance(applicationContext)
        bm.sendBroadcast(intent)
    }

    private fun reloadPage() {
        Timber.d("reloadPage")
        val intent = Intent(BROADCAST_ACTION_RELOAD_PAGE)
        val bm = LocalBroadcastManager.getInstance(applicationContext)
        bm.sendBroadcast(intent)
    }

    private fun clearBrowserCache() {
        Timber.d("clearBrowserCache")
        val intent = Intent(BROADCAST_ACTION_CLEAR_BROWSER_CACHE)
        val bm = LocalBroadcastManager.getInstance(applicationContext)
        bm.sendBroadcast(intent)
    }

    private fun publishMotionDetected() {
        Timber.d("publishMotionDetected")
        if (motionDetectedCountdown <= 0) {
            val data = JSONObject()
            try {
                data.put(MqttUtils.VALUE, true)
            } catch (ex: JSONException) {
                ex.printStackTrace()
            }
            publishMessage("sensor/motion", data)
        }
        motionDetectedCountdown = 2
        motionHandler.postDelayed({ clearMotionDetected() }, 1000)
    }

    private fun publishFaceDetected() {
        Timber.d("publishFaceDetected")
        if (faceDetectedCountdown <= 0) {
            val data = JSONObject()
            try {
                data.put(MqttUtils.VALUE, true)
            } catch (ex: JSONException) {
                ex.printStackTrace()
            }
            publishMessage("sensor/face", data)
        }
        faceDetectedCountdown = 2
        faceHandler.postDelayed({ clearFaceDetected() }, 1000)
    }

    private fun clearMotionDetected() {
        if (motionDetectedCountdown > 0) {
            motionDetectedCountdown--
            if (motionDetectedCountdown == 0) {
                Timber.d("Clearing motion detected status")
                val data = JSONObject()
                try {
                    data.put(MqttUtils.VALUE, false)
                } catch (ex: JSONException) {
                    ex.printStackTrace()
                }
                publishMessage("sensor/motion", data)
            }
        }
    }

    private fun clearFaceDetected() {
        if (faceDetectedCountdown > 0) {
            faceDetectedCountdown--
            if (faceDetectedCountdown == 0) {
                Timber.d("Clearing face detected status")
                val data = JSONObject()
                try {
                    data.put(MqttUtils.VALUE, false)
                } catch (ex: JSONException) {
                    ex.printStackTrace()
                }
                publishMessage("sensor/face", data) //todo add face to api docs
            }
        }
    }

    private fun publishQrCode(data: String) {
        Timber.d("publishQrCode")
        val jdata = JSONObject()
        try {
            jdata.put(MqttUtils.VALUE, data)
        } catch (ex: JSONException) {
            ex.printStackTrace()
        }
        publishMessage("sensor/qrcode", jdata)
    }

    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (BROADCAST_EVENT_URL_CHANGE == intent.action) {
                val url = intent.getStringExtra(BROADCAST_EVENT_URL_CHANGE)
                if (url != configuration.appLaunchUrl) {
                    Timber.i("Url changed to $url")
                    configuration.appLaunchUrl = url
                    publishMessage("state", state.toString())
                }
            } else if (Intent.ACTION_SCREEN_OFF == intent.action ||
                    intent.action == Intent.ACTION_SCREEN_ON ||
                    intent.action == Intent.ACTION_USER_PRESENT) {
                Timber.i("Screen state changed")
                publishMessage("state", state.toString())
            } else if (BROADCAST_EVENT_SCREEN_TOUCH == intent.action) {
                Timber.i("Screen touched")
                if (configuration.cameraMotionBright) {
                    setBrightScreen(255)
                }
                publishMessage("state", state.toString())
            }
        }
    }

    private val sensorCallback = object : SensorCallback {
        override fun publishSensorData(sensorName: String, sensorData: JSONObject) {
            publishMessage(sensorName, sensorData)
        }
    }

    private val cameraDetectorCallback = object : CameraCallback {
        override fun onMotionDetected() {
            Timber.i("Motion detected")
            if (configuration.cameraMotionWake) {
                switchScreenOn()
            }
            if (configuration.cameraMotionBright) {
                Timber.d("configuration.cameraMotionBright ${configuration.cameraMotionBright}")
                setBrightScreen(255)
            }
            publishMotionDetected()
        }

        override fun onTooDark() {
           // Timber.i("Too dark for motion detection")
        }

        override fun onFaceDetected() {
            Timber.i("Face detected")
            Timber.d("configuration.cameraMotionBright ${configuration.cameraMotionBright}")
            if (configuration.cameraFaceWake) {
                switchScreenOn()
            }
            if (configuration.cameraMotionBright) {
                setBrightScreen(255)
            }
            publishFaceDetected()
        }

        override fun onQRCode(data: String) {
            Timber.i("QR Code Received: $data")
            Toast.makeText(this@WallPanelService, getString(R.string.toast_qr_code_read), Toast.LENGTH_SHORT).show()
            publishQrCode(data)
        }
    }

    companion object {
        private val ONGOING_NOTIFICATION_ID = 1
        const val BROADCAST_EVENT_URL_CHANGE = "BROADCAST_EVENT_URL_CHANGE"
        const val BROADCAST_EVENT_SCREEN_TOUCH = "BROADCAST_EVENT_SCREEN_TOUCH"
    }
}