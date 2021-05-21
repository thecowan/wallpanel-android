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

import com.thanksmister.iot.wallpanel.utils.BrowserUtils
import junit.framework.TestCase

import org.junit.After
import org.junit.Before
import org.junit.Test

class BrowserActivityNativeTest : TestCase() {

    @Before
    public override fun setUp() {
    }

    @After
    public override fun tearDown() {
    }

    @Test
    fun testParseIntentMethod() {
        val browserUtils =  BrowserUtils()
        var intent = browserUtils.parseIntent("intent:#Intent;launchFlags=0x10000000;component=com.google.android.apps.maps/com.google.android.maps.MapsActivity;end")
        assertNotNull(intent)
        assertEquals("com.google.android.maps.MapsActivity", intent?.component?.className)
        assertEquals("com.google.android.apps.maps", intent?.component?.packageName)
        assertEquals("android.intent.action.VIEW", intent?.action)

        intent = browserUtils.parseIntent("intent:#Intent;launchFlags=0x10000000;component=com.amazon.avod/.client.activity.HomeScreenActivity;end")
        assertNotNull(intent)
        assertEquals(".client.activity.HomeScreenActivity", intent?.component?.className)
        assertEquals("com.amazon.avod", intent?.component?.packageName)
        assertEquals("android.intent.action.VIEW", intent?.action)
    }
}