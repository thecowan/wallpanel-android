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


import com.localbuzz.mobile.android.BaseApplication;
import com.localbuzz.mobile.android.persistence.AppDatabase;
import com.localbuzz.mobile.android.persistence.MessageDao;
import com.localbuzz.mobile.android.utils.DialogUtils;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;

@Module
abstract class ApplicationModule {

    @Binds
    abstract Application application(BaseApplication baseApplication);

    @Provides
    @Singleton
    static Context provideContext(Application application) {
        return application;
    }

    @Singleton
    @Provides
    static AppDatabase provideDatabase(Application app) {
        return AppDatabase.getInstance(app);
    }

    @Singleton
    @Provides
    static MessageDao provideMessageDao(AppDatabase database) {
        return database.messageDao();
    }

    @Provides
    static DialogUtils providesDialogUtils(Context context) {
        return new DialogUtils(context);
    }
}