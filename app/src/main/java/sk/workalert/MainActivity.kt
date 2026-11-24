
package sk.workalert

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView

class MainActivity : Activity() {
    private val prefs by lazy {
        getSharedPreferences("workalert_session", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        startWorkAlertSession()
        buildScreen()
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    private fun startWorkAlertSession() {
        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Restore a previous session if Android killed the app unexpectedly.
        if (prefs.getBoolean("session_active", false)) {
            val previous = prefs.getInt("original_alarm_volume", -1)
            if (previous >= 0) {
                try {
                    audio.setStreamVolume(AudioManager.STREAM_ALARM, previous, 0)
                } catch (_: SecurityException) {}
            }
            prefs.edit().clear().apply()
        }

        val original = audio.getStreamVolume(AudioManager.STREAM_ALARM)
        prefs.edit()
            .putInt("original_alarm_volume", original)
            .putBoolean("session_active", true)
            .apply()

        try {
            audio.setStreamVolume(
                AudioManager.STREAM_ALARM,
                audio.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                0
            )
        } catch (_: SecurityException) {}
    }

    private fun buildScreen() {
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(18, 22, 29))
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }

        val title = TextView(this).apply {
            text = "WorkAlert"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        root.addView(title, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.CENTER_HORIZONTAL
        ).apply {
            topMargin = dp(30)
        })

        val signal = TextView(this).apply {
            text = "SIGNÁL"
            textSize = 30f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            background = roundedBackground(Color.rgb(28, 132, 67), dp(24))

            setOnClickListener {
                // Local sound test for now; remote colleague alerts are not connected yet.
                AlertTone.play(this@MainActivity)
            }
        }

        root.addView(signal, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            dp(170),
            Gravity.CENTER
        ))

        val close = TextView(this).apply {
            text = "×"
            textSize = 29f
            setTextColor(Color.argb(210, 255, 255, 255))
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            contentDescription = "Zatvoriť WorkAlert"
            background = roundedBackground(Color.argb(95, 150, 150, 150), dp(16))

            setOnClickListener {
                endWorkAlertSession()
                finish()
            }
        }

        root.addView(close, FrameLayout.LayoutParams(
            dp(46), dp(46), Gravity.TOP or Gravity.START
        ).apply {
            leftMargin = dp(4)
            topMargin = dp(4)
        })

        setContentView(root)
    }

    private fun endWorkAlertSession() {
        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val previous = prefs.getInt("original_alarm_volume", -1)

        if (previous >= 0) {
            try {
                audio.setStreamVolume(AudioManager.STREAM_ALARM, previous, 0)
            } catch (_: SecurityException) {}
        }

        prefs.edit().clear().apply()
    }

    @Deprecated("Use the X button to end WorkAlert mode.")
    override fun onBackPressed() {
        // Keep the active screen open; the X button ends the session.
    }

    private fun roundedBackground(color: Int, radius: Int) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
