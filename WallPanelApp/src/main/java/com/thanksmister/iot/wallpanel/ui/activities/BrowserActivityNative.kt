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

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.webkit.*
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleObserver
import com.google.android.material.snackbar.Snackbar
import com.thanksmister.iot.wallpanel.BuildConfig
import com.thanksmister.iot.wallpanel.R
import com.thanksmister.iot.wallpanel.network.ConnectionLiveData
import com.thanksmister.iot.wallpanel.ui.fragments.CodeBottomSheetFragment
import kotlinx.android.synthetic.main.activity_browser.*
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit


class BrowserActivityNative : BaseBrowserActivity(), LifecycleObserver {

    private val mWebView: WebView by lazy {
        findViewById<View>(R.id.activity_browser_webview_native) as WebView
    }

    private var certPermissionsShown = false
    private var playlistHandler: Handler? = null
    private var codeBottomSheet: CodeBottomSheetFragment? = null
    private var webSettings: WebSettings? = null
    private val calendar: Calendar = Calendar.getInstance()
    private val reconnectionHandler = Handler()
    private var connectionLiveData: ConnectionLiveData? = null
    private var isConnected = true
    private var webkitPermissionRequest: PermissionRequest? = null
    private var awaitingReconnect = false

    // To save current index
    private var playlistIndex = 0

    private val playlistRunnable = object : Runnable {
        override fun run() {
            // TODO: allow users to set their own value in settings
            val offset = 60L - calendar.get(Calendar.SECOND)
            val urls: List<String> = configuration.appLaunchUrl.lines()
            // Avoid IndexOutOfBound
            playlistIndex = (playlistIndex + 1) % urls.size
            if (urls.isNotEmpty() && urls.size >= playlistIndex) {
                loadWebViewUrl(urls[playlistIndex])
                playlistHandler?.postDelayed(this, TimeUnit.SECONDS.toMillis(offset))
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
            configuration.mqttBroker = BuildConfig.BROKER
            configuration.mqttUsername = BuildConfig.BROKER_USERNAME
            configuration.mqttPassword = BuildConfig.BROKER_PASS
            configuration.appLaunchUrl = BuildConfig.HASS_URL
            configuration.isFirstTime = false
            configuration.settingsCode = BuildConfig.CODE.toString()
            configuration.hasClockScreenSaver = true
        }

        try {
            setContentView(R.layout.activity_browser)
        } catch (e: Exception) {
            Timber.e(e.message)
            AlertDialog.Builder(this@BrowserActivityNative)
                    .setMessage(getString(R.string.dialog_missing_webview_warning))
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            return
        }

        launchSettingsFab.setOnClickListener {
            if (configuration.isFirstTime) {
                openSettings()
            } else {
                showCodeBottomSheet()
            }
        }

        connectionLiveData = ConnectionLiveData(this)
        connectionLiveData?.observe(this, { connected ->
            if (connected && isConnected.not()) {
                isConnected = true
                if(awaitingReconnect) { // reload the page if there was error initially loading page due to network disconnect
                    stopReloadDelay()
                    initWebPageLoad()
                } else if(configuration.browserRefreshDisconnect) { // reload page on network reconnect
                    initWebPageLoad()
                }
            } else if (connected.not()) {
                isConnected = false
            }
        })

        mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        // Force links and redirects to open in the WebView instead of in a browser
        mWebView.webChromeClient = object : WebChromeClient() {
            var snackbar: Snackbar? = null
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                if (newProgress == 100) {
                    snackbar?.dismiss()
                    if (view.url != null) {
                        pageLoadComplete(view.url.toString())
                    } else {
                        Toast.makeText(this@BrowserActivityNative, getString(R.string.toast_empty_url), Toast.LENGTH_SHORT).show()
                        complete()
                    }
                    return
                }
                if (displayProgress) {
                    val text = getString(R.string.text_loading_percent, newProgress.toString(), view.url)
                    if (snackbar == null) {
                        snackbar = Snackbar.make(view, text, Snackbar.LENGTH_INDEFINITE)
                    } else {
                        snackbar?.setText(text)
                    }
                    snackbar?.show()
                }
            }

            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun onPermissionRequest(request: PermissionRequest?) {
                super.onPermissionRequest(request)
                webkitPermissionRequest = request
                request?.resources?.forEach {
                    when(it){
                        PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {
                            askForWebkitPermission(it, REQUEST_CODE_PERMISSION_AUDIO)
                        }
                        PermissionRequest.RESOURCE_VIDEO_CAPTURE -> {
                            askForWebkitPermission(it, REQUEST_CODE_PERMISSION_CAMERA)
                        }
                    }
                }
            }

            override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
                if (view.context != null && !isFinishing) {
                    AlertDialog.Builder(this@BrowserActivityNative)
                            .setMessage(message)
                            .setPositiveButton(android.R.string.ok, null)
                            .show()
                }
                return true
            }


        }

        mWebView.webViewClient = object : WebViewClient() {
            private var isRedirect = false
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                isRedirect = true
                view.loadUrl(url)
                return true
            }

            // TODO load a special file here on disconnect and then reload page on timer
            override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
                if (!isFinishing) {
                    view.loadUrl("about:blank")
                    view.loadUrl("file:///android_asset/error_page.html")
                    isConnected = false
                    startReloadDelay()
                }
            }

            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler?, error: SslError?) {
                if (!certPermissionsShown && !isFinishing && !configuration.ignoreSSLErrors) {
                    var message = getString(R.string.dialog_message_ssl_generic)
                    when (error?.primaryError) {
                        SslError.SSL_UNTRUSTED -> message = getString(R.string.dialog_message_ssl_untrusted)
                        SslError.SSL_EXPIRED -> message = getString(R.string.dialog_message_ssl_expired)
                        SslError.SSL_IDMISMATCH -> message = getString(R.string.dialog_message_ssl_mismatch)
                        SslError.SSL_NOTYETVALID -> message = getString(R.string.dialog_message_ssl_not_yet_valid)
                    }
                    message += getString(R.string.dialog_message_ssl_continue)
                    dialogUtils.showAlertDialog(this@BrowserActivityNative,
                            getString(R.string.dialog_title_ssl_error),
                            getString(R.string.dialog_message_ssl_continue),
                            getString(R.string.button_continue),
                            { _, which -> handler?.proceed() },
                            { _, which -> handler?.proceed() }
                    )
                } else {
                    handler?.proceed()
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                if(isConnected) {
                    stopReloadDelay()
                }
                if (isRedirect) {
                    isRedirect = false
                    return
                }
            }
        }

        mWebView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    resetScreen()
                    if (!v.hasFocus()) {
                        v.requestFocus()
                    }
                }
                MotionEvent.ACTION_UP -> if (!v.hasFocus()) {
                    v.requestFocus()
                }
            }
            false
        }

        initWebPageLoad()
    }

    override fun onDestroy() {
        super.onDestroy()
        codeBottomSheet?.dismiss()
    }

    override fun onStart() {
        super.onStart()
        if (configuration.useDarkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            setLightTheme()
        }

        if (configuration.hardwareAccelerated && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // chromium, enable hardware acceleration
            mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        } else {
            // older android version, disable hardware acceleration
            mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        if (configuration.browserRefresh) {
            swipeContainer.setOnRefreshListener {
                clearCache()
                initWebPageLoad()
            }
            mOnScrollChangedListener = ViewTreeObserver.OnScrollChangedListener { swipeContainer?.isEnabled = mWebView.scrollY == 0 }
            swipeContainer.viewTreeObserver.addOnScrollChangedListener(mOnScrollChangedListener)
        } else {
            swipeContainer.isEnabled = false
        }

        setupSettingsButton()

        if(configuration.hasSettingsUpdates()) {
            initWebPageLoad()
        }
    }

    override fun onStop() {
        super.onStop()
        if (mOnScrollChangedListener != null && configuration.browserRefresh) {
            swipeContainer.viewTreeObserver.removeOnScrollChangedListener(mOnScrollChangedListener)
        }
    }

    override fun complete() {
        if (swipeContainer != null && swipeContainer.isRefreshing && configuration.browserRefresh) {
            swipeContainer.isRefreshing = false
        }
    }

    override fun openSettings() {
        hideScreenSaver()
        // Stop our service for performance reasons and to pick up changes
        stopService(wallPanelService)
        val intent = SettingsActivity.createStartIntent(this)
        startActivity(intent)
    }

    @SuppressLint("SetJavaScriptEnabled")
    // TODO handle deprecated web settings
    override fun configureWebSettings(userAgent: String) {
        if(webSettings == null) {
            webSettings = mWebView.settings
        }
        webSettings?.javaScriptEnabled = true
        webSettings?.domStorageEnabled = true
        webSettings?.databaseEnabled = true
        webSettings?.saveFormData = true
        webSettings?.javaScriptCanOpenWindowsAutomatically = true
        webSettings?.setAppCacheEnabled(true)
        webSettings?.allowFileAccess = true
        webSettings?.allowFileAccessFromFileURLs = true
        webSettings?.allowContentAccess = true
        webSettings?.setSupportZoom(true)
        webSettings?.loadWithOverviewMode = true
        webSettings?.useWideViewPort = true
        webSettings?.pluginState = WebSettings.PluginState.ON
        webSettings?.setRenderPriority(WebSettings.RenderPriority.HIGH);
       // webSettings?.cacheMode = WebSettings.LOAD_NO_CACHE;

        if (userAgent.isNotEmpty()) {
            webSettings?.userAgentString = userAgent
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings?.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
    }

    private fun initWebPageLoad() {
        progressView.visibility = View.GONE
        mWebView.visibility = View.VISIBLE
        // set user agent
        configureWebSettings(configuration.browserUserAgent)
        // set zoom level
        if (zoomLevel != 0.0f) {
            val zoomPercent = (zoomLevel * 100).toInt()
            mWebView.setInitialScale(zoomPercent)
        }
        // check if we are using playlist
        if (configuration.appLaunchUrl.lines().size == 1) {
            loadWebViewUrl(configuration.appLaunchUrl)
        } else {
            startPlaylist()
        }
    }

    override fun loadWebViewUrl(url: String) {
        Timber.d("loadUrl $url")
        mWebView.loadUrl(url)
    }

    override fun evaluateJavascript(js: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mWebView.evaluateJavascript(js, null)
        }
    }

    override fun clearCache() {
        mWebView.clearCache(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().removeAllCookies(null)
        }
    }

    override fun reload() {
        mWebView.reload()
    }

    private val reloadPageRunnable = Runnable {
        initWebPageLoad()
    }

    private fun startReloadDelay() {
        awaitingReconnect = true
        playlistHandler?.removeCallbacksAndMessages(null)
        reconnectionHandler.postDelayed(reloadPageRunnable, 30000)
    }

    private fun stopReloadDelay() {
        awaitingReconnect = false
        reconnectionHandler.removeCallbacks(reloadPageRunnable)
    }

    private fun startPlaylist() {
        playlistHandler = Handler()
        playlistHandler?.postDelayed(playlistRunnable, 10)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun askForWebkitPermission(permission: String, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(applicationContext, permission) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
            }
        } else {
            webkitPermissionRequest?.grant(webkitPermissionRequest?.resources)
        }
    }

    private fun showCodeBottomSheet() {
        codeBottomSheet = CodeBottomSheetFragment.newInstance(configuration.settingsCode.toString(),
                object : CodeBottomSheetFragment.OnAlarmCodeFragmentListener {
                    override fun onComplete(code: String) {
                        codeBottomSheet?.dismiss()
                        openSettings()
                    }

                    override fun onCodeError() {
                        Toast.makeText(this@BrowserActivityNative, R.string.toast_code_invalid, Toast.LENGTH_SHORT).show()
                    }

                    override fun onCancel() {
                        codeBottomSheet?.dismiss()
                    }
                })
        codeBottomSheet?.show(supportFragmentManager, codeBottomSheet?.tag)
    }

    private fun setupSettingsButton() {
        // Set the location and transparency of the fab button
        val params: CoordinatorLayout.LayoutParams = CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.WRAP_CONTENT, CoordinatorLayout.LayoutParams.WRAP_CONTENT)
        params.topMargin = 16
        params.leftMargin = 16
        params.rightMargin = 16
        params.bottomMargin = 16
        when (configuration.settingsLocation) {
            0 -> {
                params.gravity = Gravity.BOTTOM or Gravity.END
            }
            1 -> {
                params.gravity = Gravity.BOTTOM or Gravity.START
            }
            2 -> {
                params.gravity = Gravity.TOP or Gravity.END
            }
            3 -> {
                params.gravity = Gravity.TOP or Gravity.START
            }
        }
        launchSettingsFab.layoutParams = params
        when {
            configuration.settingsDisabled -> {
                launchSettingsFab.visibility = View.GONE
            }
            configuration.settingsTransparent -> {
                launchSettingsFab.visibility = View.VISIBLE
                launchSettingsFab.backgroundTintList = ContextCompat.getColorStateList(this, R.color.transparent)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    launchSettingsFab.compatElevation = 0f
                }
                launchSettingsFab.imageAlpha = 0
            }
            else -> {
                launchSettingsFab.visibility = View.VISIBLE
                launchSettingsFab.backgroundTintList = ContextCompat.getColorStateList(this, R.color.colorAccent)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    launchSettingsFab.compatElevation = 4f
                }
                launchSettingsFab.imageAlpha = 180
            }
        }
    }

}