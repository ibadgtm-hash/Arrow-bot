package com.example.arrowbot

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)

        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(30, 30, 30, 30)

        val title = TextView(this)

        title.text =
            "ARROW BOT\n\n" +
            "Bot membaca layar game dan " +
            "melakukan tap otomatis."

        title.textSize = 18f

        val accessibility = Button(this)

        accessibility.text = "1. Aktifkan Accessibility"

        accessibility.setOnClickListener {
            startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            )
        }

        val capture = Button(this)

        capture.text = "2. Izinkan Screen Capture"

        capture.setOnClickListener {

            val manager =
                getSystemService(
                    MEDIA_PROJECTION_SERVICE
                ) as MediaProjectionManager

            startActivityForResult(
                manager.createScreenCaptureIntent(),
                100
            )
        }

        val start = Button(this)

        start.text = "3. START BOT"

        start.setOnClickListener {

            ArrowAccessibilityService
                .instance
                ?.startBot()
        }

        val stop = Button(this)

        stop.text = "STOP BOT"

        stop.setOnClickListener {

            ArrowAccessibilityService
                .instance
                ?.stopBot()
        }

        layout.addView(title)
        layout.addView(accessibility)
        layout.addView(capture)
        layout.addView(start)
        layout.addView(stop)

        setContentView(layout)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (
            requestCode == 100 &&
            resultCode == RESULT_OK &&
            data != null
        ) {

            ScreenCaptureHolder.resultCode = resultCode
            ScreenCaptureHolder.data = data
        }
    }
}

object ScreenCaptureHolder {

    var resultCode: Int = 0

    var data: Intent? = null
}
