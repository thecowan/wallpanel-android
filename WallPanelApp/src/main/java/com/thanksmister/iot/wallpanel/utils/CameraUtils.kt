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

import android.content.Context
import android.hardware.Camera
import android.util.Log
import com.thanksmister.iot.wallpanel.R
import timber.log.Timber
import java.util.ArrayList

/**
 * Created by Michael Ritchie on 7/9/18.
 */
class CameraUtils {

    companion object {
        @Throws(RuntimeException::class)
        fun getCameraList(context: Context): ArrayList<String> {
            val cameraList: ArrayList<String> = ArrayList()
            for (i in 0 until Camera.getNumberOfCameras()) {
                var description: String
                val c = Camera.open(i)
                val p = c.parameters
                val previewSize = p.previewSize
                val width = previewSize.width
                val height = previewSize.height
                val info = Camera.CameraInfo()
                Camera.getCameraInfo(i, info)
                val patternString = context.getString(R.string.text_camera_pattern)
                val facing =
                        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
                            context.getString(R.string.text_front)
                        else
                            context.getString(R.string.text_back)
                description = java.text.MessageFormat.format(
                        patternString,
                        i,
                        facing,
                        info.orientation,
                        width,
                        height)
                c.stopPreview()
                c.release()
                cameraList.add(description)
            }
            return cameraList
        }
    }
}
