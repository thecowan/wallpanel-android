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

package com.thanksmister.iot.wallpanel

import android.content.Intent
import com.thanksmister.iot.wallpanel.di.DaggerApplicationComponent
import com.thanksmister.iot.wallpanel.network.WallPanelService
import dagger.android.AndroidInjector
import dagger.android.support.DaggerApplication

import timber.log.Timber

class WallPanel : DaggerApplication() {

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerApplicationComponent.builder().create(this);
    }

    private var wallPanelService: Intent? = null

    override fun onCreate() {
        super.onCreate()
        //wallPanelService = Intent(this, WallPanelService::class.java)
        //startService(wallPanelService)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}