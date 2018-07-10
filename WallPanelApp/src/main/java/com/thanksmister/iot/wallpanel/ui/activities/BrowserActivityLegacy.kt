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

package com.thanksmister.iot.wallpanel.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.MotionEvent
import android.view.View
import com.thanksmister.iot.wallpanel.R

import org.xwalk.core.XWalkResourceClient
import org.xwalk.core.XWalkView
import org.xwalk.core.XWalkCookieManager

import timber.log.Timber

class BrowserActivityLegacy : BrowserActivity() {

    private var xWebView: XWalkView? = null

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {

        setContentView(R.layout.activity_browser)
        xWebView = findViewById<View>(R.id.activity_browser_webview_legacy) as XWalkView
        xWebView!!.visibility = View.VISIBLE

        xWebView!!.setResourceClient(object : XWalkResourceClient(xWebView) {

            internal var snackbar: Snackbar

            init {
                snackbar = Snackbar.make(xWebView!!, "", Snackbar.LENGTH_INDEFINITE)
            }

            override fun onProgressChanged(view: XWalkView, progressInPercent: Int) {
                if (!displayProgress) return

                if (progressInPercent == 100) {
                    snackbar.dismiss()
                    pageLoadComplete(view.url)
                } else {
                    val text = "Loading " + progressInPercent + "% " + view.url
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

        val webSettings = xWebView!!.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true

        Timber.d(webSettings.userAgentString)

        super.onCreate(savedInstanceState)
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
