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

package com.thanksmister.iot.wallpanel.ui.views

import android.content.Context
import android.os.Handler
import android.text.format.DateUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.RelativeLayout
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.dialog_screen_saver.view.*
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

class ScreenSaverView : RelativeLayout {

    private var timeHandler: Handler? = null
    private var wallPaperHandler: Handler? = null
    private var saverContext: Context? = null
    private var parentWidth: Int = 0
    private var parentHeight: Int = 0
    private var showWallpaper: Boolean = false
    private var showClock: Boolean = false

    val calendar: Calendar = Calendar.getInstance()

    private val timeRunnable = object : Runnable {
        override fun run() {
            val date = Date()
            calendar.time = date
            val currentTimeString = DateUtils.formatDateTime(context, date.time, DateUtils.FORMAT_SHOW_TIME)
            val currentDayString = DateUtils.formatDateTime(context, date.time, DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_DATE)
            screenSaverClock.text = currentTimeString
            screenSaverDay.text = currentDayString

            val width = screenSaverClockLayout.width
            val height = screenSaverClockLayout.height

            parentWidth = screenSaverView.width
            parentHeight = screenSaverView.height

            try {
                if (width > 0 && height > 0 && parentWidth > 0 && parentHeight > 0) {
                    if (parentHeight - width > 0) {
                        val newX = Random().nextInt(parentWidth - width)
                        screenSaverClockLayout.x = newX.toFloat()
                    }
                    if (parentHeight - height > 0) {
                        val newY = Random().nextInt(parentHeight - height)
                        screenSaverClockLayout.y = newY.toFloat()
                    }
                }
            } catch (e: IllegalArgumentException) {
                Timber.e(e.message)
            }

            val offset = 60L - calendar.get(Calendar.SECOND)
            timeHandler?.postDelayed(this, TimeUnit.SECONDS.toMillis(offset))
        }
    }

    // TODO we could set a timer here to reload the image at set interval
    private val wallPaperRunnable = object : Runnable {
        override fun run() {
            setScreenSaverView()
            //wallPaperHandler?.postDelayed(this, TimeUnit.SECONDS.toMillis(900L))
        }
    }

    constructor(context: Context) : super(context) {
        saverContext = context
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        saverContext = context
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        timeHandler?.removeCallbacks(timeRunnable)
    }

    fun init(hasWallpaper: Boolean, hasClock: Boolean) {
        showWallpaper = hasWallpaper
        showClock = hasClock
        if(showClock) {
            setClockViews()
            timeHandler = Handler()
            timeHandler?.postDelayed(timeRunnable, 10)
        } else {
            screenSaverClockLayout.visibility = View.INVISIBLE
        }
        if (showWallpaper) {
            wallPaperHandler = Handler()
            wallPaperHandler?.postDelayed(wallPaperRunnable, 10)
        } else {
            screenSaverImageLayout.visibility  = View.INVISIBLE
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
    }

    // setup clock size based on screen and weather settings
    private fun setClockViews() {
        val initialRegular = screenSaverClock.textSize
        screenSaverClock.setTextSize(TypedValue.COMPLEX_UNIT_PX, initialRegular + 100)
    }

    // Picasso will cache url, in order to get a new wallpaper, we do not need to use a cache
    private fun setScreenSaverView() {
        Picasso.get()
                .load(String.format(UNSPLASH_IT_URL, screenSaverView.width, screenSaverView.height))
                .memoryPolicy(MemoryPolicy.NO_CACHE)
                .networkPolicy(NetworkPolicy.NO_CACHE)
                .into(screenSaverImageLayout)
    }

    companion object {
        const val UNSPLASH_IT_URL = "https://unsplash.it/%s/%s?random"
    }
}