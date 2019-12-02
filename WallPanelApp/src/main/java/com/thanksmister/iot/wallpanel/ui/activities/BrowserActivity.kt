/*
 * Copyright (c) 2019 ThanksMister LLC
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

package com.thanksmister.iot.wallpanel.ui.activities

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.KeyEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.thanksmister.iot.wallpanel.network.WallPanelService
import com.thanksmister.iot.wallpanel.network.WallPanelService.Companion.BROADCAST_ALERT_MESSAGE
import com.thanksmister.iot.wallpanel.network.WallPanelService.Companion.BROADCAST_CLEAR_ALERT_MESSAGE
import com.thanksmister.iot.wallpanel.network.WallPanelService.Companion.BROADCAST_EVENT_SCREEN_TOUCH
import com.thanksmister.iot.wallpanel.network.WallPanelService.Companion.BROADCAST_SCREEN_BRIGHTNESS_CHANGE
import com.thanksmister.iot.wallpanel.network.WallPanelService.Companion.BROADCAST_SCREEN_WAKE
import com.thanksmister.iot.wallpanel.network.WallPanelService.Companion.BROADCAST_TOAST_MESSAGE
import com.thanksmister.iot.wallpanel.persistence.Configuration
import com.thanksmister.iot.wallpanel.utils.DialogUtils
import com.thanksmister.iot.wallpanel.utils.ScreenUtils
import dagger.android.support.DaggerAppCompatActivity
import timber.log.Timber
import javax.inject.Inject

abstract class BrowserActivity : DaggerAppCompatActivity() {

    @Inject lateinit var dialogUtils: DialogUtils
    @Inject lateinit var configuration: Configuration
    @Inject lateinit var screenUtils: ScreenUtils

    var mOnScrollChangedListener: ViewTreeObserver.OnScrollChangedListener? = null
    private var wallPanelService: Intent? = null
    private var decorView: View? = null
    private val inactivityHandler: Handler = Handler()
    private var userPresent: Boolean = false
    var displayProgress = true
    var zoomLevel = 1.0f

    // handler for received data from service
    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (BROADCAST_ACTION_LOAD_URL == intent.action) {
                val url = intent.getStringExtra(BROADCAST_ACTION_LOAD_URL)
                Timber.d("Browsing to $url")
                loadUrl(url)
                stopDisconnectTimer()
            } else if (BROADCAST_ACTION_JS_EXEC == intent.action) {
                val js = intent.getStringExtra(BROADCAST_ACTION_JS_EXEC)
                Timber.d("Executing javascript in current browser: $js")
                stopDisconnectTimer()
                evaluateJavascript(js)
            } else if (BROADCAST_ACTION_CLEAR_BROWSER_CACHE == intent.action) {
                Timber.d("Clearing browser cache")
                clearCache()
            } else if (BROADCAST_ACTION_RELOAD_PAGE == intent.action) {
                Timber.d("Browser page reloading.")
                stopDisconnectTimer()
                reload()
            } else if (BROADCAST_TOAST_MESSAGE == intent.action && !isFinishing) {
                val message = intent.getStringExtra(BROADCAST_TOAST_MESSAGE)
                stopDisconnectTimer()
                Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
            } else if (BROADCAST_ALERT_MESSAGE == intent.action && !isFinishing) {
                val message = intent.getStringExtra(BROADCAST_ALERT_MESSAGE)
                stopDisconnectTimer()
                dialogUtils.showAlertDialog(this@BrowserActivity, message)
            } else if (BROADCAST_CLEAR_ALERT_MESSAGE == intent.action && !isFinishing) {
                dialogUtils.clearDialogs()
                resetInactivityTimer()
                resetScreenBrightness(false)
            } else if (BROADCAST_SCREEN_WAKE == intent.action && !isFinishing) {
                stopDisconnectTimer()
            } else if (BROADCAST_ACTION_RELOAD_PAGE == intent.action && !isFinishing) {
                hideScreenSaver()
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        displayProgress = configuration.appShowActivity
        zoomLevel = configuration.testZoomLevel

        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)

        decorView = window.decorView

        if(configuration.hardwareAccelerated && Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            window.setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
        }
        Timber.d("Prevent Sleep ${configuration.appPreventSleep}")
        if (configuration.appPreventSleep) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON )
            decorView?.keepScreenOn = true
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            decorView?.keepScreenOn = false
        }

        lifecycle.addObserver(dialogUtils)

        onUserInteraction()
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter()
        filter.addAction(BROADCAST_ACTION_LOAD_URL)
        filter.addAction(BROADCAST_ACTION_JS_EXEC)
        filter.addAction(BROADCAST_ACTION_CLEAR_BROWSER_CACHE)
        filter.addAction(BROADCAST_ACTION_RELOAD_PAGE)
        filter.addAction(BROADCAST_SCREEN_BRIGHTNESS_CHANGE)
        filter.addAction(BROADCAST_CLEAR_ALERT_MESSAGE)
        filter.addAction(BROADCAST_ALERT_MESSAGE)
        filter.addAction(BROADCAST_TOAST_MESSAGE)
        filter.addAction(BROADCAST_SCREEN_WAKE)
        val bm = LocalBroadcastManager.getInstance(this)
        bm.registerReceiver(mBroadcastReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        val bm = LocalBroadcastManager.getInstance(this)
        bm.unregisterReceiver(mBroadcastReceiver)
    }

    override fun onStart() {
        super.onStart()
        wallPanelService = Intent(this, WallPanelService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(wallPanelService)
        } else {
            startService(wallPanelService)
        }
        resetScreenBrightness(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        inactivityHandler.removeCallbacks(inactivityCallback)
        window.clearFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
    }

    override fun onUserInteraction() {
        onWindowFocusChanged(true)
        Timber.d("onUserInteraction")
        if(!userPresent) {
            userPresent = true
            resetScreenBrightness(false)
            val intent = Intent(BROADCAST_EVENT_SCREEN_TOUCH)
            intent.putExtra(BROADCAST_EVENT_SCREEN_TOUCH, true)
            val bm = LocalBroadcastManager.getInstance(applicationContext)
            bm.sendBroadcast(intent)
        }
        resetInactivityTimer()
    }

    private val inactivityCallback = Runnable {
        Timber.d("inactivityCallback")
        dialogUtils.clearDialogs()
        userPresent = false
        showScreenSaver()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        val visibility: Int
        if (hasFocus ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                visibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            } else  {
                visibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LOW_PROFILE
                        or View.SYSTEM_UI_FLAG_FULLSCREEN)

            }
            decorView?.systemUiVisibility = visibility
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            Timber.d("dispatchKeyEvent")
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            startActivity(Intent(this@BrowserActivity, SettingsActivity::class.java))
            finish()
            return true
        } else {
            return super.dispatchKeyEvent(event)
        }
    }

    internal fun resetScreen() {
        Timber.d("resetScreen Called")
        val intent = Intent(WallPanelService.BROADCAST_EVENT_SCREEN_TOUCH)
        intent.putExtra(WallPanelService.BROADCAST_EVENT_SCREEN_TOUCH, true)
        val bm = LocalBroadcastManager.getInstance(applicationContext)
        bm.sendBroadcast(intent)
    }

    internal fun pageLoadComplete(url: String) {
        Timber.d("pageLoadComplete currentUrl $url")
        val intent = Intent(WallPanelService.BROADCAST_EVENT_URL_CHANGE)
        intent.putExtra(WallPanelService.BROADCAST_EVENT_URL_CHANGE, url)
        val bm = LocalBroadcastManager.getInstance(applicationContext)
        bm.sendBroadcast(intent)
        complete()
    }

    private fun resetInactivityTimer() {
        hideScreenSaver()
        inactivityHandler.removeCallbacks(inactivityCallback)
        inactivityHandler.postDelayed(inactivityCallback, configuration.inactivityTime)
    }

    fun stopDisconnectTimer() {
        Timber.d("stopDisconnectTimer")
        if(!userPresent) {
            userPresent = true
            resetScreenBrightness(false)
        }
        resetInactivityTimer()
    }

    open fun hideScreenSaver() {
        Timber.d("hideScreenSaver")
        val isScreenSaver = dialogUtils.hideScreenSaverDialog()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if(isScreenSaver) {
            resetScreenBrightness(false)
        }
    }

    /**
     * Show the screen saver only if the alarm isn't triggered. This shouldn't be an issue
     * with the alarm disabled because the disable time will be longer than this.
     */
    open fun showScreenSaver() {
        Timber.d("showScreenSaver ${configuration.hasScreenSaver}")
        if (configuration.hasScreenSaver && !isFinishing) {
            inactivityHandler.removeCallbacks(inactivityCallback)
            try {
                dialogUtils.showScreenSaver(this@BrowserActivity,
                        View.OnClickListener {
                            dialogUtils.hideScreenSaverDialog()
                            resetScreenBrightness(false)
                            resetInactivityTimer()
                        },
                        configuration.hasScreenSaverWallpaper)
            } catch (e: Exception) {
                Timber.e(e.message)
            }
            resetScreenBrightness(true)
        }

    }

    open fun resetScreenBrightness(screenSaver: Boolean = false) {
        screenUtils.resetScreenBrightness(screenSaver)
    }

    protected abstract fun configureWebSettings(userAgent: String)
    protected abstract fun loadUrl(url: String)
    protected abstract fun evaluateJavascript(js: String)
    protected abstract fun clearCache()
    protected abstract fun reload()
    protected abstract fun complete()

    companion object {
        const val BROADCAST_ACTION_LOAD_URL = "BROADCAST_ACTION_LOAD_URL"
        const val BROADCAST_ACTION_JS_EXEC = "BROADCAST_ACTION_JS_EXEC"
        const val BROADCAST_ACTION_CLEAR_BROWSER_CACHE = "BROADCAST_ACTION_CLEAR_BROWSER_CACHE"
        const val BROADCAST_ACTION_RELOAD_PAGE = "BROADCAST_ACTION_RELOAD_PAGE"
    }
}