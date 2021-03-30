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

import android.content.Intent
import timber.log.Timber

class BrowserUtils {
    /**
     * This methods parses url's with intent strings
     * "intent:#Intent;launchFlags=0x10000000;component=com.amazon.avod/com.amazon.avod.client.activity.HomeScreenActivity;end"
     */
    fun parseIntent(url: String): Intent? {
        val separated = url.split(";").toTypedArray()
        return if (separated.size == 4) {
            Timber.d(separated[0]) // this will contain "intent:#Intent"
            Timber.d(separated[1]) // this will contain "launch flag"
            Timber.d(separated[2]) // this will contain "component"
            Timber.d(separated[3]) // this will contain "end"
            val component = separated[2].removePrefix("component=")
            val classname = component.split("/").toTypedArray()
            val pkgName = classname[0] // this will set the packageName:
            val clsName = classname[1] // this will set the className:
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.setClassName(pkgName, clsName)
            intent
        } else {
            null
        }
    }
}