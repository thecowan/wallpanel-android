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

package com.thanksmister.iot.wallpanel.utils

import android.hardware.Camera
import android.util.Log
import java.util.ArrayList

/**
 * Created by Michael Ritchie on 7/9/18.
 */
class CameraUtils {

    companion object {
        fun getCameraList(): ArrayList<String> {
            val cameraList: ArrayList<String> = ArrayList()
            for (i in 0 until Camera.getNumberOfCameras()) {
                var description: String
                try {
                    val c = Camera.open(i)
                    val p = c.parameters
                    val previewSize = p.previewSize
                    val width = previewSize.width
                    val height = previewSize.height
                    val info = Camera.CameraInfo()
                    Camera.getCameraInfo(i, info)
                    description = java.text.MessageFormat.format(
                            "{0}: {1} Camera {3}x{4} {2}º",
                            i,
                            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) "Front" else "Back",
                            info.orientation,
                            width,
                            height)
                    c.release()
                } catch (e: Exception) {
                    Log.e("CameraReader", "Had a problem reading camera $i")
                    e.printStackTrace()
                    description = java.text.MessageFormat.format("{0}: Error", i)
                }
                cameraList.add(description)
            }
            return cameraList
        }
    }
}
