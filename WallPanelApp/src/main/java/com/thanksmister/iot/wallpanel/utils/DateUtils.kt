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

package com.thanksmister.iot.wallpanel.utils

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


/**
 * Date utils
 */
object DateUtils {

    var SECONDS_VALUE = 60000
    var MINUTES_VALUE = 1800000

    fun generateCreatedAtDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
        return dateFormat.format(Date())
    }

    fun padTimePickerOutput(timeValue: String): String {
        var value = timeValue
        if (value.length == 1) {
            value = "0$value"
        }
        return value
    }

    fun getHourFromTimePicker(timeValue: String): Int {
        val values = timeValue.split(":")
        if(values.size > 1) {
            val value = values[0].toInt()
            return value
        }
        return 0
    }

    fun getMinutesFromTimePicker(timeValue: String): Int {
        val values = timeValue.split(":")
        if(values.size > 1) {
            val value = values[1].toInt()
            return value
        }
        return 0
    }

    fun getHourAndMinutesFromTimePicker(timePickerValue: String): Float {
        return timePickerValue.replace(":", ".").toFloat()
    }

    /**
     * This converts the milliseconds to a day of the week, but we try to account
     * for time that is shorter than expected from DarkSky API .
     * @param apiTime
     * @return
     */
    fun dayOfWeek(apiTime: Long): String {
        var time = apiTime
        if (apiTime.toString().length == 10) {
            time = apiTime * 1000
        }
        val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
        return sdf.format(Date(time))
    }

    fun convertInactivityTime(inactivityValue: Long): String {
        return if (inactivityValue < SECONDS_VALUE) {
            TimeUnit.MILLISECONDS.toSeconds(inactivityValue).toString()
        } else if (inactivityValue > MINUTES_VALUE) {
            TimeUnit.MILLISECONDS.toHours(inactivityValue).toString()
        } else {
            TimeUnit.MILLISECONDS.toMinutes(inactivityValue).toString()
        }
    }
}