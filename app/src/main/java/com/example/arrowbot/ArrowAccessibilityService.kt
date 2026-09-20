package com.example.arrowbot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class ArrowAccessibilityService :
    AccessibilityService() {

    companion object {
        var instance: ArrowAccessibilityService? = null
    }

    private val handler =
        Handler(Looper.getMainLooper())

    private var running = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        stopBot()
        instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(
        event: AccessibilityEvent?
    ) {
    }

    override fun onInterrupt() {
        stopBot()
    }

    fun startBot() {

        if (ScreenCaptureHolder.data == null) {
            return
        }

        running = true
        scan()
    }

    fun stopBot() {

        running = false

        handler.removeCallbacksAndMessages(null)
    }

    private fun scan() {

        if (!running) return

        ArrowDetector
            .get(this)
            .findNextArrow { point ->

                if (!running) return@findNextArrow

                if (point != null) {
                    tap(
                        point.x.toFloat(),
                        point.y.toFloat()
                    )
                }

                handler.postDelayed(
                    {
                        scan()
                    },
                    150
                )
            }
    }

    private fun tap(
        x: Float,
        y: Float
    ) {

        val path = Path()

        path.moveTo(x, y)

        val gesture =
            GestureDescription
                .Builder()
                .addStroke(
                    GestureDescription.StrokeDescription(
                        path,
                        0,
                        60
                    )
                )
                .build()

        dispatchGesture(
            gesture,
            null,
            null
        )
    }
    }
