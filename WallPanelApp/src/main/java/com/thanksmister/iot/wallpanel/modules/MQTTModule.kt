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

package com.thanksmister.iot.wallpanel.modules

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import android.content.Context
import android.content.ContextWrapper
import com.thanksmister.iot.wallpanel.network.MQTTOptions
import com.thanksmister.iot.wallpanel.network.MQTTService

import org.eclipse.paho.client.mqttv3.MqttException
import timber.log.Timber

class MQTTModule (base: Context?, var mqttOptions: MQTTOptions, private val listener: MQTTListener) : ContextWrapper(base),
        LifecycleObserver,
        MQTTService.MqttManagerListener {

    private var mqttService: MQTTService? = null

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private fun start() {
        Timber.d("start")
        if (mqttService == null) {
            try {
                mqttService = MQTTService(applicationContext, mqttOptions, this)
            } catch (t: Throwable) {
                // TODO should we loop back and try again?
                Timber.e("Could not create MQTTPublisher: " + t.message)
            }
        } else {
            try {
                mqttService?.reconfigure(applicationContext, mqttOptions, this)
            } catch (t: Throwable) {
                // TODO should we loop back and try again?
                Timber.e("Could not create MQTTPublisher: " + t.message)
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun stop() {
        Timber.d("stop")
        mqttService?.let {
            try {
                it.close()
            } catch (e: MqttException) {
                e.printStackTrace()
            }
            mqttService = null
        }
    }

    fun restart() {
        Timber.d("restart")
        stop()
        start()
    }

    fun pause() {
        Timber.d("pause")
        stop()
    }

    fun publish(command: String, message : String) {
        Timber.d("command: " + command)
        Timber.d("message: " + message)
         mqttService?.publish(command, message)
    }

    override fun subscriptionMessage(id: String, topic: String, payload: String) {
        Timber.d("topic: " + topic)
        listener.onMQTTMessage(id, topic, payload)
    }

    override fun handleMqttException(errorMessage: String) {
        listener.onMQTTException(errorMessage)
    }

    override fun handleMqttDisconnected() {
        listener.onMQTTDisconnect()
    }

    override fun handleMqttConnected() {
        listener.onMQTTConnect()
    }

    interface MQTTListener {
        fun onMQTTConnect()
        fun onMQTTDisconnect()
        fun onMQTTException(message : String)
        fun onMQTTMessage(id: String, topic: String, payload: String)
    }
}