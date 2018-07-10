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
import com.thanksmister.iot.wallpanel.controls.Motion.*
import com.thanksmister.iot.wallpanel.controls.Motion.Companion.MOTION_DETECTED
import com.thanksmister.iot.wallpanel.controls.Motion.Companion.MOTION_NOT_DETECTED
import com.thanksmister.iot.wallpanel.controls.Motion.Companion.MOTION_TOO_DARK

import java.nio.ByteBuffer

import timber.log.Timber

/**
 * Created by Michael Ritchie on 7/6/18.
 */
class MotionDetector private constructor(private val minLuma: Int, private val motionLeniency: Int) : Detector<Motion>() {

    private var aggregateLumaMotionDetection: AggregateLumaMotionDetection? = null

    init {
        aggregateLumaMotionDetection = AggregateLumaMotionDetection()
        aggregateLumaMotionDetection!!.setLeniency(motionLeniency)
    }

    override fun detect(frame: Frame?): SparseArray<Motion> {
        if (frame == null) {
            throw IllegalArgumentException("No frame supplied.")
        } else {
            val byteBuffer = frame.grayscaleImageData
            val bytes = byteBuffer.array()
            val w = frame.metadata.width
            val h = frame.metadata.height
            val sparseArray = SparseArray<Motion>()
            val motion = Motion()
            motion.byteArray = bytes
            motion.width = w
            motion.height = h

            val img = ImageProcessing.decodeYUV420SPtoLuma(bytes, w, h)
            var lumaSum = 0
            for (i in img) {
                lumaSum += i
            }
            if (lumaSum < minLuma) {
                motion.type = MOTION_TOO_DARK
                sparseArray.put(0, motion)
                return sparseArray
            }

            val motionDetected = aggregateLumaMotionDetection!!.detect(img, w, h)
            if (motionDetected) {
                motion.type = MOTION_DETECTED
                Timber.d("MOTION_DETECTED")
            } else {
                motion.type = MOTION_NOT_DETECTED
                Timber.d("MOTION_NOT_DETECTED")
            }
            sparseArray.put(0, motion)
            return sparseArray
        }
    }

    class Builder(private val minLuma: Int, private val motionLeniency: Int) {
        fun build(): MotionDetector {
            return MotionDetector(minLuma, motionLeniency)
        }
    }
}