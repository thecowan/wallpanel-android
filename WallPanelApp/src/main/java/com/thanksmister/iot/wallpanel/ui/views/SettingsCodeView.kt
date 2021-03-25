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

package com.thanksmister.iot.wallpanel.ui.views

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import com.thanksmister.iot.wallpanel.R
import kotlinx.android.synthetic.main.dialog_code_set.view.*

class SettingsCodeView : BaseView {

    internal var alarmListener: ViewListener? = null

    private val delayRunnable = object : Runnable {
        override fun run() {
            if (handler != null) {
                handler.removeCallbacks(this)
            }
            if (alarmListener != null) {
                alarmListener?.onComplete(enteredCode)
            }
        }
    }

    interface ViewListener {
        fun onComplete(code: String)
        fun onError()
        fun onCancel()
    }

    fun setListener(listener: ViewListener) {
        this.alarmListener = listener
    }

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    override fun onFinishInflate() {
        super.onFinishInflate()
        codeTitle.text = context.getString(R.string.dialog_enter_code)
    }

    override fun onCancel() {
        codeComplete = false
        enteredCode = ""
        showFilledPins(0)

        if (handler != null) {
            handler.removeCallbacks(delayRunnable)
        }
        if (alarmListener != null) {
            alarmListener?.onCancel()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (handler != null) {
            handler.removeCallbacks(delayRunnable)
        }
    }

    override fun reset() {}

    override fun addPinCode(code: String) {
        if (codeComplete)
            return

        enteredCode += code

        showFilledPins(enteredCode.length)

        if (enteredCode.length == BaseView.Companion.MAX_CODE_LENGTH) {
            codeComplete = true
            handler.postDelayed(delayRunnable, 500)
        }
    }

    override fun removePinCode() {
        if (codeComplete) return
        if (!TextUtils.isEmpty(enteredCode)) {
            enteredCode = enteredCode.substring(0, enteredCode.length - 1)
            showFilledPins(enteredCode.length)
        }
    }
}