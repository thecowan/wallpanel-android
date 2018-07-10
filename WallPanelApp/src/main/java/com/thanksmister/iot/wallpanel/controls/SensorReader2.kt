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

package com.thanksmister.iot.wallpanel.controls

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Handler
import android.util.Log

import com.thanksmister.iot.wallpanel.network.WallPanelService

import org.json.JSONException
import org.json.JSONObject

import java.util.ArrayList

import timber.log.Timber

import android.content.Context.SENSOR_SERVICE

class SensorReader2(private val wallPanelService: WallPanelService, private val context: Context) {

    private val VALUE = "value"
    private val UNIT = "unit"
    private val ID = "id"

    private val mSensorManager: SensorManager?
    private val mSensorList = ArrayList<Sensor>()

    private val sensorHandler = Handler()
    private var updateFrequencyMilliSeconds: Int? = 0

    private val sensorHandlerRunnable = object : Runnable {
        override fun run() {
            if (updateFrequencyMilliSeconds > 0) {
                Timber.d("Updating Sensors")
                getSensorReadings()
                getBatteryReading()
                sensorHandler.postDelayed(this, updateFrequencyMilliSeconds!!.toLong())
            }
        }
    }

    init {
        Timber.d("Creating SensorReader")
        mSensorManager = context.getSystemService(SENSOR_SERVICE) as SensorManager
        if (mSensorManager != null) {
            for (s in mSensorManager.getSensorList(Sensor.TYPE_ALL)) {
                if (getSensorName(s.type) != null)
                    mSensorList.add(s)
            }
        }
    }

    fun startReadings(freqSeconds: Int) {
        Timber.d("startReadings Called")
        if (freqSeconds >= 0) {
            updateFrequencyMilliSeconds = 1000 * freqSeconds
            sensorHandler.postDelayed(sensorHandlerRunnable, updateFrequencyMilliSeconds!!.toLong())
        }
    }

    fun stopReadings() {
        Timber.d("stopSensorJob Called")
        sensorHandler.removeCallbacksAndMessages(sensorHandlerRunnable)
        updateFrequencyMilliSeconds = 0
    }

    private fun publishSensorData(sensorName: String?, sensorData: JSONObject) {
        Timber.d("publishSensorData Called")
        wallPanelService.publishMessage(
                sensorData,
                "sensor/" + sensorName!!)
    }

    private fun getSensorName(sensorType: Int): String? {
        when (sensorType) {
            Sensor.TYPE_AMBIENT_TEMPERATURE -> return "temperature"
            Sensor.TYPE_LIGHT // TODO change in API to light
            -> return "light"
            Sensor.TYPE_MAGNETIC_FIELD -> return "magneticField"
            Sensor.TYPE_PRESSURE -> return "pressure"
            Sensor.TYPE_RELATIVE_HUMIDITY -> return "humidity"
        }
        return null
    }

    private fun getSensorReadings() {
        Timber.d("getSensorReadings")
        for (sensor in mSensorList) {
            mSensorManager!!.registerListener(object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val data = JSONObject()
                    try {
                        data.put(VALUE, event.values[0].toDouble())
                        data.put(UNIT, "??") // todo not useful units :)
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

    private fun getBatteryReading() {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, intentFilter)

        val batteryStatusIntExtra = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                ?: -1
        val isCharging = batteryStatusIntExtra == BatteryManager.BATTERY_STATUS_CHARGING || batteryStatusIntExtra == BatteryManager.BATTERY_STATUS_FULL

        val chargePlug = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
        val acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        //int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;

        val data = JSONObject()
        try {
            data.put(VALUE, level)
            val BATTERYSENSOR_UNIT = "%"
            data.put(UNIT, BATTERYSENSOR_UNIT)
            data.put("charging", isCharging)
            data.put("acPlugged", acCharge)
            data.put("usbPlugged", usbCharge)
        } catch (ex: JSONException) {
            ex.printStackTrace()
        }

        publishSensorData("battery", data)
    }
}