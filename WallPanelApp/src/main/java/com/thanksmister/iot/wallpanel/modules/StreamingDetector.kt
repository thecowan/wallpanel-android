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

package com.thanksmister.iot.wallpanel.modules

import android.util.SparseArray

import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Frame
import com.jjoe64.motiondetection.motiondetection.AggregateLumaMotionDetection
import com.jjoe64.motiondetection.motiondetection.ImageProcessing
import com.thanksmister.iot.wallpanel.modules.Motion.Companion.MOTION_DETECTED
import com.thanksmister.iot.wallpanel.modules.Motion.Companion.MOTION_NOT_DETECTED
import com.thanksmister.iot.wallpanel.modules.Motion.Companion.MOTION_TOO_DARK

import timber.log.Timber

/**
 * Created by Michael Ritchie on 7/6/18.
 */
class StreamingDetector private constructor() : Detector<Stream>() {
    init {
    }
    override fun detect(frame: Frame?): SparseArray<Stream> {
        if (frame == null) {
            throw IllegalArgumentException("No frame supplied.")
        } else {
            val sparseArray = SparseArray<Stream>()
            val byteBuffer = frame.grayscaleImageData
            val bytes = byteBuffer.array()
            val w = frame.metadata.width
            val h = frame.metadata.height
            val stream = Stream()
            stream.byteArray = bytes
            stream.width = w
            stream.height = h
            sparseArray.put(0, stream)
            return sparseArray
        }
    }
    class Builder() {
        fun build(): StreamingDetector {
            return StreamingDetector()
        }
    }
}