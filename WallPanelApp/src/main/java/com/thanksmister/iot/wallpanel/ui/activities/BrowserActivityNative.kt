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
import android.content.DialogInterface
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import com.thanksmister.iot.wallpanel.R
import kotlinx.android.synthetic.main.activity_browser.*
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit


class BrowserActivityNative : BrowserActivity() {

    private var mWebView: WebView? = null
    private var certPermissionsShown = false
    private var playlistHandler: Handler? = null

    val calendar: Calendar = Calendar.getInstance()

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
                loadUrl(urls[playlistIndex])
                playlistHandler?.postDelayed(this, TimeUnit.SECONDS.toMillis(offset))
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        if (configuration.browserRefresh) {
            swipeContainer.setOnRefreshListener {
                clearCache()
                loadUrl(configuration.appLaunchUrl)
            }
            mOnScrollChangedListener = ViewTreeObserver.OnScrollChangedListener { swipeContainer?.isEnabled = mWebView?.scrollY == 0 }
        } else {
            swipeContainer.isEnabled = false
        }

        mWebView = findViewById<View>(R.id.activity_browser_webview_native) as WebView
        mWebView?.visibility = View.VISIBLE
        mWebView?.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        // Force links and redirects to open in the WebView instead of in a browser
        mWebView?.webChromeClient = object : WebChromeClient() {
            var snackbar: Snackbar? = null
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                if (newProgress == 100) {
                    if (snackbar != null) {
                        snackbar!!.dismiss()
                    }
                    if (view.url != null) {
                        pageLoadComplete(view.url)
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
                        snackbar!!.setText(text)
                    }
                    snackbar!!.show()
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

        mWebView?.webViewClient = object : WebViewClient() {
            private var isRedirect = false

            //If you will not use this method url links are open in new browser not in webview
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                isRedirect = true
                view.loadUrl(url)
                return true
            }

            override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
                if (!isFinishing) {
                    Toast.makeText(this@BrowserActivityNative, description, Toast.LENGTH_SHORT).show()
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
                            DialogInterface.OnClickListener { _, which -> handler?.proceed() },
                            DialogInterface.OnClickListener { _, which -> handler?.proceed() }
                    )
                } else {
                    handler?.proceed()
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                if (isRedirect) {
                    isRedirect = false
                    return
                }
            }
        }

        mWebView?.setOnTouchListener { v, event ->
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

        if (configuration.hardwareAccelerated && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // chromium, enable hardware acceleration
            mWebView?.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        } else {
            // older android version, disable hardware acceleration
            mWebView?.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        configureWebSettings(configuration.browserUserAgent)

        if (1.equals(configuration.appLaunchUrl.lines().size)) {
            loadUrl(configuration.appLaunchUrl)
        } else {
            startPlaylist()
        }
    }

    override fun onStart() {
        super.onStart()
        if (swipeContainer != null && mOnScrollChangedListener != null && configuration.browserRefresh) {
            swipeContainer.viewTreeObserver.addOnScrollChangedListener(mOnScrollChangedListener)
        }
    }

    override fun onStop() {
        super.onStop()
        if (swipeContainer != null && mOnScrollChangedListener != null && configuration.browserRefresh) {
            swipeContainer.viewTreeObserver.removeOnScrollChangedListener(mOnScrollChangedListener)
        }
    }

    override fun complete() {
        if (swipeContainer != null && swipeContainer.isRefreshing && configuration.browserRefresh) {
            swipeContainer.isRefreshing = false
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun configureWebSettings(userAgent: String) {
        val webSettings = mWebView?.settings
        webSettings?.javaScriptEnabled = true
        webSettings?.domStorageEnabled = true
        webSettings?.databaseEnabled = true
        webSettings?.javaScriptCanOpenWindowsAutomatically = true
        webSettings?.setAppCacheEnabled(true)
        webSettings?.allowFileAccess = true
        webSettings?.allowFileAccessFromFileURLs = true
        webSettings?.allowContentAccess = true
        webSettings?.setSupportZoom(true)
        webSettings?.loadWithOverviewMode = true;
        webSettings?.useWideViewPort = true;

        if (!TextUtils.isEmpty(userAgent)) {
            webSettings?.userAgentString = userAgent
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings?.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        Timber.d(webSettings?.userAgentString)
    }

    override fun loadUrl(url: String) {
        Timber.d("loadUrl $url")
        if (zoomLevel != 0.0f) {
            val zoomPercent = (zoomLevel * 100).toInt()
            mWebView?.setInitialScale(zoomPercent)
        }
        mWebView?.loadUrl(url)
    }

    override fun evaluateJavascript(js: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mWebView?.evaluateJavascript(js, null)
        }
    }

    override fun clearCache() {
        mWebView?.clearCache(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().removeAllCookies(null)
        }
    }

    override fun reload() {
        mWebView?.reload()
    }

    private fun startPlaylist() {
        playlistHandler = Handler()
        playlistHandler?.postDelayed(playlistRunnable, 10)
    }
}