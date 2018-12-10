/*
 * Copyright (c) 2018 LocalBuzz
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

class ScreenSaverView : RelativeLayout {

    private var timeHandler: Handler? = null
    private var saverContext: Context? = null

    private val timeRunnable = object : Runnable {
        override fun run() {
            val currentTimeString = DateUtils.formatDateTime(context, Date().time, DateUtils.FORMAT_SHOW_TIME)
            Timber.d("currentTimeString ${currentTimeString}")
            screenSaverClock.text = currentTimeString
            if (timeHandler != null) {
                timeHandler!!.postDelayed(this, 1000)
            }
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
        if (timeHandler != null) {
            timeHandler!!.removeCallbacks(timeRunnable)
        }
    }

    // setup clock size based on screen and weather settings
    private fun seClockViews() {
        val initialRegular = screenSaverClock.textSize
        screenSaverClock.setTextSize(TypedValue.COMPLEX_UNIT_PX, initialRegular + 100)
    }

    fun init() {
        seClockViews()
        timeHandler = Handler()
        timeHandler!!.postDelayed(timeRunnable, 10)
    }
}