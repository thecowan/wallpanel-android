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

package com.thanksmister.iot.wallpanel.modules

import android.content.Context
import android.content.Context.SENSOR_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Handler
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class SensorReader @Inject
constructor(private val context: Context) {

    private val VALUE = "value"
    private val UNIT = "unit"
    private val ID = "id"
    private val mSensorManager: SensorManager?
    private val mSensorList = ArrayList<Sensor>()
    private val sensorHandler = Handler()
    private var updateFrequencyMilliSeconds: Int = 0
    private var callback: SensorCallback? = null

    private val sensorHandlerRunnable = object : Runnable {
        override fun run() {
            if (updateFrequencyMilliSeconds > 0) {
                Timber.d("Updating Sensors")
                getSensorReadings()
                getBatteryReading()
                sensorHandler.postDelayed(this, updateFrequencyMilliSeconds.toLong())
            }
        }
    }

    init {
        Timber.d("Creating SensorReader")
        mSensorManager = context.getSystemService(SENSOR_SERVICE) as SensorManager
        for (s in mSensorManager.getSensorList(Sensor.TYPE_ALL)) {
            if (getSensorName(s.type) != null)
                mSensorList.add(s)
        }
    }

    fun startReadings(freqSeconds: Int, callback: SensorCallback) {
        Timber.d("startReadings Called")
        this.callback = callback
        if (freqSeconds >= 0) {
            updateFrequencyMilliSeconds = 1000 * freqSeconds
            sensorHandler.postDelayed(sensorHandlerRunnable, updateFrequencyMilliSeconds.toLong())
        }
    }

    fun stopReadings() {
        Timber.d("stopSensorJob Called")
        sensorHandler.removeCallbacksAndMessages(sensorHandlerRunnable)
        updateFrequencyMilliSeconds = 0
    }

    // TODO add a call back same as camera reader to return data all publishing happens in service
    private fun publishSensorData(sensorName: String?, sensorData: JSONObject) {
        Timber.d("publishSensorData Called")
        if(sensorName != null) {
            callback?.publishSensorData(sensorName, sensorData)
        }
    }

    private fun getSensorName(sensorType: Int): String? {
        when (sensorType) {
            Sensor.TYPE_AMBIENT_TEMPERATURE -> return "temperature"
            Sensor.TYPE_LIGHT -> return "light"
            Sensor.TYPE_MAGNETIC_FIELD -> return "magneticField"
            Sensor.TYPE_PRESSURE -> return "pressure"
            Sensor.TYPE_RELATIVE_HUMIDITY -> return "humidity"
        }
        return null
    }

    private fun getSensorUnit(sensorType: Int): String? {
        when (sensorType) {
            Sensor.TYPE_AMBIENT_TEMPERATURE -> return "°C"
            Sensor.TYPE_LIGHT -> return "lx"
            Sensor.TYPE_MAGNETIC_FIELD -> return "uT"
            Sensor.TYPE_PRESSURE -> return "hPa"
            Sensor.TYPE_RELATIVE_HUMIDITY -> return "%"
        }
        return null
    }

    // TODO determine the sensor and give it proper values for UNIT
    private fun getSensorReadings() {
        Timber.d("getSensorReadings")
        for (sensor in mSensorList) {
            mSensorManager!!.registerListener(object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    Timber.d("Sensor Type ${event.sensor.type}")
                    val unit = getSensorUnit(event.sensor.type)
                    val data = JSONObject()
                    try {
                        data.put(VALUE, event.values[0].toDouble())
                        data.put(UNIT, unit)
                        data.put(ID, event.sensor.name)
                    } catch (ex: JSONException) {
                        ex.printStackTrace()
                    }
                    publishSensorData(getSensorName(event.sensor.type), data)
                    mSensorManager.unregisterListener(this)
                }
                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
            }, sensor, 1000)
        }
    }

    // TODO let's move this to its own setting
    private fun getBatteryReading() {
        Timber.d("getBatteryReading")
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, intentFilter)
        val batteryStatusIntExtra = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = batteryStatusIntExtra == BatteryManager.BATTERY_STATUS_CHARGING || batteryStatusIntExtra == BatteryManager.BATTERY_STATUS_FULL
        val chargePlug = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
        val acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val data = JSONObject()
        try {
            data.put(VALUE, level)
            data.put(UNIT, "%")
            data.put("charging", isCharging)
            data.put("acPlugged", acCharge)
            data.put("usbPlugged", usbCharge)
        } catch (ex: JSONException) {
            ex.printStackTrace()
        }

        publishSensorData("battery", data)
    }
}