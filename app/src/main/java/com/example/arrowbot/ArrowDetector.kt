package com.example.arrowbot

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import kotlin.math.max
import kotlin.math.min

class ArrowDetector private constructor(
    private val service: AccessibilityService
) {

    companion object {

        @Volatile
        private var instance: ArrowDetector? = null

        fun get(
            service: AccessibilityService
        ): ArrowDetector {

            return instance
                ?: synchronized(this) {

                    instance
                        ?: ArrowDetector(service)
                            .also {
                                instance = it
                            }
                }
        }
    }

    private var projection: MediaProjection? = null

    private var reader: ImageReader? = null

    private val thread =
        HandlerThread("ArrowCapture")
            .apply { start() }

    private val handler =
        Handler(thread.looper)

    fun findNextArrow(
        callback: (Point?) -> Unit
    ) {

        val intent =
            ScreenCaptureHolder.data
                ?: run {
                    callback(null)
                    return
                }

        if (projection == null) {

            val manager =
                service.getSystemService(
                    MediaProjectionManager::class.java
                )

            projection =
                manager.getMediaProjection(
                    ScreenCaptureHolder.resultCode,
                    intent
                )
        }

        val metrics =
            service.resources.displayMetrics

        val width =
            metrics.widthPixels

        val height =
            metrics.heightPixels

        if (reader == null) {

            reader =
                ImageReader.newInstance(
                    width,
                    height,
                    android.graphics.PixelFormat.RGBA_8888,
                    2
                )

            projection?.createVirtualDisplay(
                "ArrowBotCapture",
                width,
                height,
                metrics.densityDpi,
                DisplayManager
                    .VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader!!.surface,
                null,
                handler
            )
        }

        handler.post {

            val image =
                reader?.acquireLatestImage()

            if (image == null) {
                callback(null)
                return@post
            }

            val plane =
                image.planes[0]

            val buffer =
                plane.buffer

            val pixelStride =
                plane.pixelStride

            val rowStride =
                plane.rowStride

            val rowPadding =
                rowStride -
                    pixelStride * width

            val bitmap =
                Bitmap.createBitmap(
                    width +
                        rowPadding / pixelStride,
                    height,
                    Bitmap.Config.ARGB_8888
                )

            bitmap.copyPixelsFromBuffer(buffer)

            image.close()

            val result =
                detect(bitmap)

            bitmap.recycle()

            callback(result)
        }
    }

    private fun detect(
        bitmap: Bitmap
    ): Point? {

        val width =
            bitmap.width

        val height =
            bitmap.height

        if (width < 20 || height < 20) {
            return null
        }

        val left = width / 10
        val right = width - width / 10
        val top = height / 10
        val bottom = height - height / 10

        val step =
            max(
                2,
                min(width, height) / 300
            )

        var bestX = -1
        var bestY = -1
        var bestScore = 0

        var y = top

        while (y < bottom) {

            var x = left

            while (x < right) {

                val pixel =
                    bitmap.getPixel(x, y)

                val r =
                    pixel shr 16 and 255

                val g =
                    pixel shr 8 and 255

                val b =
                    pixel and 255

                val highest =
                    max(r, max(g, b))

                val lowest =
                    min(r, min(g, b))

                val contrast =
                    highest - lowest

                val score =
                    if (
                        highest > 210 &&
                        contrast > 35
                    ) {
                        contrast
                    } else {
                        0
                    }

                if (score > bestScore) {

                    bestScore = score
                    bestX = x
                    bestY = y
                }

                x += step
            }

            y += step
        }

        return if (bestScore >= 45) {
            Point(bestX, bestY)
        } else {
            null
        }
    }
}
