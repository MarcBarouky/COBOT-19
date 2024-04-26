/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.mediapipe.examples.gesturerecognizer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min


@SuppressLint("UseCompatLoadingForDrawables")
class OverlayView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    private var results: GestureRecognizerResult? = null

    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1
    private var helloDetected = false
    private val TAG = "Overlay"

    private var picture_neutral: Bitmap? = null
    private var picture_hello: Bitmap? = null

    var bluetoothActionDelegate: BluetoothActionDelegate? = null

    init {
        if (context != null) {
            picture_neutral = BitmapFactory.decodeResource(context.resources, R.drawable.ic_neutral)
            picture_hello = BitmapFactory.decodeResource(context.resources, R.drawable.ic_smiling)

        }
    }

    interface BluetoothActionDelegate {
        fun onPerformBluetoothAction()
    }

    fun clear() {
        results = null
    }

    private fun calculateCenteredRect(canvasWidth: Int, canvasHeight: Int, bitmap: Bitmap?): Rect {
        val bitmapWidth = bitmap?.width ?: 0
        val bitmapHeight = bitmap?.height ?: 0

        //make it 1.5 times bigger
        val scaleFactor = 0.4f

        val scaledWidth = (bitmapWidth * scaleFactor).toInt()
        val scaledHeight = (bitmapHeight * scaleFactor).toInt()
        val left = (canvasWidth - scaledWidth) / 2
        val top = (canvasHeight - scaledHeight) / 2
        val right = left + scaledWidth
        val bottom = top + scaledHeight

        return Rect(left, top, right, bottom)
    }

    private fun notSmilingFace(canvas: Canvas) {
        val dstRect = calculateCenteredRect(canvas.width, canvas.height, picture_hello)
        picture_neutral?.let {
            canvas.drawBitmap(it, null, dstRect, null)
        }
    }

    private fun smilingFace(canvas: Canvas) {
        val dstRect = calculateCenteredRect(canvas.width, canvas.height, picture_hello)
        picture_hello?.let {
            canvas.drawBitmap(it, null, dstRect, null)
        }
        canvas.drawText("Hello !", 0F, 0F, Paint())
    }

    private fun handDetection () {
        results?.let { gestureRecognizerResult ->

            val gestures = gestureRecognizerResult.gestures()
            for (gesture in gestures) {
                if (gesture[0].toString().contains("Open_Palm")) {

                    CoroutineScope(Dispatchers.Main).launch {
                        if (!helloDetected) {
                            bluetoothActionDelegate?.onPerformBluetoothAction()
                            helloDetected = true
                            delay(5000) // Delay for 5 seconds
                            Log.d(TAG, "HELLO DETECTED")
                            helloDetected = false
                        }
                        
                        invalidate() // Invalidate the view to trigger redraw
                    }
                }
            }
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        handDetection()
        if (!helloDetected) {
            notSmilingFace(canvas)
        } else {
            smilingFace(canvas)
        }
    }

    fun setResults(
        gestureRecognizerResult: GestureRecognizerResult,
        imageHeight: Int,
        imageWidth: Int,
        runningMode: RunningMode = RunningMode.IMAGE
    )   {
        results = gestureRecognizerResult

        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        scaleFactor = when (runningMode) {
            RunningMode.IMAGE,
            RunningMode.VIDEO -> {
                min(width * 1f / imageWidth, height * 1f / imageHeight)
            }
            RunningMode.LIVE_STREAM -> {
                max(width * 1f / imageWidth, height * 1f / imageHeight)
            }
        }
        invalidate()
    }
}