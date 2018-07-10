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

import android.content.Context
import android.util.SparseArray

import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Frame
import com.google.common.primitives.Bytes
import com.jjoe64.motiondetection.motiondetection.AggregateLumaMotionDetection
import com.jjoe64.motiondetection.motiondetection.ImageProcessing

import java.nio.ByteBuffer

import timber.log.Timber

import com.thanksmister.iot.wallpanel.controls.Motion.MOTION_DETECTED
import com.thanksmister.iot.wallpanel.controls.Motion.MOTION_NOT_DETECTED

/**
 * Created by Michael Ritchie on 7/6/18.
 */
class MotionDetector private constructor(private val minLuma: Int) : Detector<Motion>() {

    private val aggregateLumaMotionDetection: AggregateLumaMotionDetection

    init {
        aggregateLumaMotionDetection = AggregateLumaMotionDetection()
    }

    override fun release() {
        super.release()
    }

    override fun detect(frame: Frame?): SparseArray<Motion> {
        if (frame == null) {
            throw IllegalArgumentException("No frame supplied.")
        } else {
            val byteBuffer = frame.grayscaleImageData
            val bytes = byteBuffer.array()
            val w = frame.metadata.width
            val h = frame.metadata.height

            val img = ImageProcessing.decodeYUV420SPtoLuma(bytes, w, h)

            val motionDetected = aggregateLumaMotionDetection.detect(img, w, h)

            val sparseArray = SparseArray<Motion>()
            val motion = Motion()
            if (motionDetected) {
                motion.type = MOTION_DETECTED
                Timber.d("MOTION_DETECTED")
            } else {
                motion.type = MOTION_NOT_DETECTED
                Timber.d("MOTION_NOT_DETECTED")
            }
            sparseArray.put(0, motion)

            /*
            val byteArray = params[0] as ByteArray
            val width = params[1] as Int
            val height = params[2] as Int
            val minLuma = params[3] as Int

            val img = ImageProcessing.decodeYUV420SPtoLuma(byteArray, width/10, height/10)
            var lumaSum = 0
            for (i in img) {
                lumaSum += i
            }
            if (lumaSum < minLuma) {
                return MOTION_TOO_DARK
            } else if (motionDetection.detect(img, width/10, height/10)) {
                return MOTION_DETECTED
            }
            return MOTION_NOT_DETECTED
            */

            return sparseArray
        }
    }

    class Builder(private val minLuma: Int) {

        fun build(): MotionDetector {
            return MotionDetector(minLuma)
        }
    }
}