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

package com.thanksmister.iot.wallpanel.network

import android.arch.lifecycle.MutableLiveData
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager

import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

class ConnectionLiveData(private val context: Context) : MutableLiveData<Boolean>() {

    private var connCallbackListener: ConnCallbackListener? = null

    init {
        connCallbackListener = object : ConnCallbackListener {
            override fun networkConnect() {
                value = true
            }
            override fun networkDisconnect() {
                value = false
            }
        }
    }

    override fun onActive() {
        super.onActive()
        context.registerReceiver(connectionReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    override fun onInactive() {
        super.onInactive()
        context.unregisterReceiver(connectionReceiver)
    }

    private val connectionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val currentNetworkInfo = connectivityManager.activeNetworkInfo
            if (currentNetworkInfo != null && currentNetworkInfo.isConnected) {
                Timber.d("Network Connected")
                hasNetwork.set(true)
                value = true
            } else if (hasNetwork.get()) {
                Timber.d("Network Disconnected")
                hasNetwork.set(false)
                value = false
            }
        }
    }

    interface ConnCallbackListener {
        fun networkConnect()
        fun networkDisconnect()
    }

    companion object {
        var hasNetwork = AtomicBoolean(true)
    }
}
