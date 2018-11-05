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
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import com.thanksmister.iot.wallpanel.R
import kotlinx.android.synthetic.main.activity_browser.*
import org.xwalk.core.XWalkCookieManager
import org.xwalk.core.XWalkResourceClient
import org.xwalk.core.XWalkView
import timber.log.Timber


class BrowserActivityLegacy : BrowserActivity() {

    private var xWebView: XWalkView? = null

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        Timber.i("BrowserActivityLegacy")

        try {
            setContentView(R.layout.activity_browser)
         } catch (e: Exception) {
            Timber.e(e.message)
            //dialogUtils.showAlertDialog(this@BrowserActivityLegacy, getString(R.string.dialog_missing_webview_warning))
            AlertDialog.Builder(this@BrowserActivityLegacy)
                    .setMessage(getString(R.string.dialog_missing_webview_warning))
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            return
        }


        xWebView = findViewById<View>(R.id.activity_browser_webview_legacy) as XWalkView
        xWebView!!.visibility = View.VISIBLE
        xWebView!!.setResourceClient(object : XWalkResourceClient(xWebView) {
            var snackbar: Snackbar

            init {
                snackbar = Snackbar.make(xWebView!!, "", Snackbar.LENGTH_INDEFINITE)
            }

            override fun onProgressChanged(view: XWalkView, progressInPercent: Int) {
                if (!displayProgress) return
                if (progressInPercent == 100) {
                    snackbar.dismiss()
                    if(view.url != null) {
                        pageLoadComplete(view.url)
                    } else {
                        Toast.makeText(this@BrowserActivityLegacy, getString(R.string.toast_empty_url), Toast.LENGTH_SHORT).show()
                        complete()
                    }
                } else {
                    //val text = "Loading " + progressInPercent + "% " + view.url
                    val text = getString(R.string.text_loading_percent, progressInPercent.toString(), view.url)
                    snackbar.setText(text)
                    snackbar.show()
                }
            }
        })

        xWebView!!.setOnTouchListener { v, event ->
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

        if(configuration.browserRefresh) {
            swipeContainer.setOnRefreshListener {
                clearCache()
                loadUrl(configuration.appLaunchUrl)
            }
            mOnScrollChangedListener = ViewTreeObserver.OnScrollChangedListener { swipeContainer?.isEnabled = xWebView!!.scrollY == 0 }
        } else {
            swipeContainer.isEnabled = false
        }

        configureWebSettings(configuration.browserUserAgent)
        loadUrl(configuration.appLaunchUrl)
    }

    override fun onStart() {
        super.onStart()
        if(swipeContainer != null && mOnScrollChangedListener != null && configuration.browserRefresh) {
            swipeContainer.viewTreeObserver.addOnScrollChangedListener(mOnScrollChangedListener)
        }
    }

    override fun onStop() {
        super.onStop()
        if(swipeContainer != null && mOnScrollChangedListener != null && configuration.browserRefresh) {
            swipeContainer.viewTreeObserver.removeOnScrollChangedListener(mOnScrollChangedListener)
        }
    }

    override fun complete() {
        if(swipeContainer != null && swipeContainer.isRefreshing && configuration.browserRefresh) {
            swipeContainer.isRefreshing = false
        }
    }

    override fun configureWebSettings(userAgent: String) {
        val webSettings = xWebView!!.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true

        if(!TextUtils.isEmpty(userAgent)) {
            webSettings.userAgentString = userAgent
        }
        Timber.d(webSettings.userAgentString)
    }

    override fun loadUrl(url: String) {
        if (zoomLevel.toDouble() != 1.0) {
            xWebView!!.setInitialScale((zoomLevel * 100).toInt())
        }
        xWebView!!.loadUrl(url)
    }

    override fun evaluateJavascript(js: String) {
        xWebView!!.evaluateJavascript(js, null)
    }

    override fun clearCache() {
        xWebView!!.clearCache(true)
        val manager = XWalkCookieManager()
        manager.removeAllCookie()
    }

    override fun reload() {
        xWebView!!.reload(XWalkView.RELOAD_NORMAL)
    }
}