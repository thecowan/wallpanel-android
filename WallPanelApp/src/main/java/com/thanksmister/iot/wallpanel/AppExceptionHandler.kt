/*
 * Copyright (c) 2021 ThanksMister LLC
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

package com.thanksmister.iot.wallpanel

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.thanksmister.iot.wallpanel.ui.activities.BrowserActivityNative
import kotlin.system.exitProcess

class AppExceptionHandler(private val activity: Activity) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, ex: Throwable) {
        val intent = Intent(activity, BrowserActivityNative::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                or Intent.FLAG_ACTIVITY_CLEAR_TASK
                or Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent = PendingIntent.getActivity(activity.applicationContext, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        val mgr = activity.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        mgr[AlarmManager.RTC, System.currentTimeMillis() + 1000] = pendingIntent
        activity.finish()
        exitProcess(2)
    }
}