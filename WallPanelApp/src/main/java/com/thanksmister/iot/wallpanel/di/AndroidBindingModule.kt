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

package com.localbuzz.mobile.android.di

import android.arch.lifecycle.ViewModel
import com.localbuzz.mobile.android.BaseActivity
import com.localbuzz.mobile.android.BaseFragment
import com.localbuzz.mobile.android.ui.*
import com.localbuzz.mobile.android.viewmodel.CameraViewModel
import com.localbuzz.mobile.android.viewmodel.MainViewModel
import com.localbuzz.mobile.android.viewmodel.StartViewModel

import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap

@Module
internal abstract class AndroidBindingModule {

    @Binds
    @IntoMap
    @ViewModelKey(MainViewModel::class)
    abstract fun bindsMessageViewModel(mainViewModel: MainViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(StartViewModel::class)
    abstract fun bindsStartViewModel(startViewModel: StartViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(CameraViewModel::class)
    abstract fun bindsCameraViewModel(cameraViewModel: CameraViewModel): ViewModel

    @ContributesAndroidInjector
    internal abstract fun baseActivity(): BaseActivity

    @ContributesAndroidInjector
    internal abstract fun mainActivity(): MainActivity

    @ContributesAndroidInjector
    internal abstract fun mainFragment(): MainFragment

    @ContributesAndroidInjector
    internal abstract fun cameraFragment(): CameraFragment

    @ContributesAndroidInjector
    internal abstract fun baseFragment(): BaseFragment

    @ContributesAndroidInjector
    internal abstract fun startFragment(): StartFragment

    @ContributesAndroidInjector
    internal abstract fun previewFragment(): PreviewFragment

    @ContributesAndroidInjector
    internal abstract fun transitFragment(): TransmitFragment
}