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

package com.localbuzz.mobile.android.di;

import android.app.Application;
import android.content.Context;


import android.content.res.Resources;
import android.location.LocationManager;
import android.view.LayoutInflater;

import com.localbuzz.mobile.android.persistence.Preferences;
import com.localbuzz.mobile.android.utils.DialogUtils;

import net.grandcentrix.tray.AppPreferences;

import dagger.Module;
import dagger.Provides;

@Module
class ActivityModule {

    @Provides
    static Resources providesResources(Application application) {
        return application.getResources();
    }

    @Provides
    static LayoutInflater providesInflater(Application application) {
        return (LayoutInflater) application.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Provides
    static LocationManager provideLocationManager(Application application) {
        return (LocationManager) application.getSystemService(Context.LOCATION_SERVICE);
    }

    @Provides
    static AppPreferences provideAppPreferences(Application application) {
        return new AppPreferences(application);
    }

    @Provides
    static Preferences provideConfiguration(AppPreferences appPreferences) {
        return new Preferences(appPreferences);
    }
}