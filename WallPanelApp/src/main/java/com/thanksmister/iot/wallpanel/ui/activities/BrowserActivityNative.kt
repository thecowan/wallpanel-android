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

package com.thanksmister.iot.wallpanel.ui.activities

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import android.webkit.*
import com.thanksmister.iot.wallpanel.R
import kotlinx.android.synthetic.main.activity_browser.*
import timber.log.Timber
import android.webkit.WebView
import android.widget.Toast


class BrowserActivityNative : BrowserActivity() {

    private var mWebView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {

        setContentView(R.layout.activity_browser)

        swipeContainer.setOnRefreshListener { loadUrl(configuration.appLaunchUrl)}

        mWebView = findViewById<View>(R.id.activity_browser_webview_native) as WebView
        mWebView!!.visibility = View.VISIBLE
        clearCache()

        // Force links and redirects to open in the WebView instead of in a browser
        mWebView!!.webChromeClient = object : WebChromeClient() {

            var snackbar: Snackbar? = null

            override fun onProgressChanged(view: WebView, newProgress: Int) {
                if (!displayProgress) return

                if (newProgress == 100 && snackbar != null) {
                    snackbar!!.dismiss()
                    pageLoadComplete(view.url)
                    return
                }
                val text= getString(R.string.text_loading_percent, newProgress.toString(), view.url)
                if (snackbar == null) {
                    snackbar = Snackbar.make(view, text, Snackbar.LENGTH_INDEFINITE)
                } else {
                    snackbar!!.setText(text)
                }
                snackbar!!.show()
            }

            override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
                dialogUtils.showAlertDialog(view.context, message)
                return true
            }

        }

        mWebView!!.webViewClient = object : WebViewClient() {
            //If you will not use this method url links are open in new browser not in webview
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                return true
            }

            override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
                Toast.makeText(this@BrowserActivityNative, description, Toast.LENGTH_SHORT).show()
            }
        }

        mWebView!!.setOnTouchListener { v, event ->
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

        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        swipeContainer.viewTreeObserver.addOnScrollChangedListener {
            swipeContainer.isEnabled = mWebView!!.scrollY == 0
        }
    }

    override fun onStop() {
        super.onStop()
        swipeContainer.viewTreeObserver.removeOnScrollChangedListener(mOnScrollChangedListener)
    }

    override fun complete() {
        if(swipeContainer.isRefreshing) {
            swipeContainer.isRefreshing = false
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun configureWebSettings(userAgent: String) {
        val webSettings = mWebView!!.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webSettings.setAppCacheEnabled(true)

        if(!TextUtils.isEmpty(userAgent)) {
            webSettings.userAgentString = userAgent
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        Timber.d(webSettings.userAgentString)
    }

    override fun loadUrl(url: String) {
        Timber.d("loadUrl $url")
        if (zoomLevel.toDouble() != 1.0) {
            mWebView!!.setInitialScale((zoomLevel * 100).toInt())
        }
        mWebView?.loadUrl(url)
    }

    override fun evaluateJavascript(js: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mWebView!!.evaluateJavascript(js, null)
        }
    }

    override fun clearCache() {
        mWebView!!.clearCache(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().removeAllCookies(null)
        }
    }

    override fun reload() {
        mWebView!!.reload()
    }
}
