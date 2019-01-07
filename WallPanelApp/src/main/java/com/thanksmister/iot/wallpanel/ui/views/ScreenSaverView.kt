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
import android.widget.RelativeLayout
import kotlinx.android.synthetic.main.dialog_screen_saver.view.*
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

class ScreenSaverView : RelativeLayout {

    private var timeHandler: Handler? = null
    private var saverContext: Context? = null
    private var parentWidth: Int = 0
    private var parentHeight: Int = 0

    val calendar: Calendar = Calendar.getInstance()

    private val timeRunnable = object : Runnable {
        override fun run() {
            val date = Date()
            calendar.time = date
            val currentTimeString = DateUtils.formatDateTime(context, date.time, DateUtils.FORMAT_SHOW_TIME)
            screenSaverClock.text = currentTimeString

            val width = screenSaverClockLayout.width
            val height = screenSaverClockLayout.height
            parentWidth = screenSaverView.width
            parentHeight = screenSaverView.height
            if(width > 0 && height > 0) {
                val newX = Random().nextInt(parentWidth - width - 25)
                val newY = Random().nextInt(parentHeight - height - 25)
                screenSaverClockLayout.x = newX.toFloat()
                screenSaverClockLayout.y = newY.toFloat()
            }

            val offset = 60L - calendar.get(Calendar.SECOND)
            timeHandler?.postDelayed(this, TimeUnit.SECONDS.toMillis(offset))
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

    fun init() {

    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        setClockViews()
        timeHandler = Handler()
        timeHandler?.postDelayed(timeRunnable, 10)
    }

    // setup clock size based on screen and weather settings
    private fun setClockViews() {
        val initialRegular = screenSaverClock.textSize
        screenSaverClock.setTextSize(TypedValue.COMPLEX_UNIT_PX, initialRegular + 100)
    }
}