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

/**
 * Created by Michael Ritchie on 7/6/18.
 */
class Motion {

    var type = MOTION_NOT_DETECTED
    var byteArray: ByteArray? = null
    var width: Int? = null
    var height: Int? = null

    companion object {
        val MOTION_TOO_DARK = "motion_too_dark"
        val MOTION_DETECTED = "motion_detected"
        val MOTION_NOT_DETECTED = "motion_not_detected"
    }
}
