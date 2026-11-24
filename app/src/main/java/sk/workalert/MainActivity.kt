
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

    private lateinit var volumeStatus: TextView

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

        // If an earlier app process was interrupted, restore its saved alarm volume first.
        if (prefs.getBoolean("session_active", false)) {
            val saved = prefs.getInt("original_alarm_volume", -1)
            if (saved >= 0) {
                try {
                    audio.setStreamVolume(AudioManager.STREAM_ALARM, saved, 0)
                } catch (_: SecurityException) {
                }
            }
            prefs.edit().clear().apply()
        }

        val original = audio.getStreamVolume(AudioManager.STREAM_ALARM)
        prefs.edit()
            .putInt("original_alarm_volume", original)
            .putBoolean("session_active", true)
            .apply()

        try {
            val max = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audio.setStreamVolume(AudioManager.STREAM_ALARM, max, 0)
        } catch (_: SecurityException) {
            // The actual result is checked below and shown to the user.
        }
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

        volumeStatus = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }
        root.addView(volumeStatus, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        ).apply {
            bottomMargin = dp(18)
        })
        updateVolumeStatus()

        val signal = TextView(this).apply {
            text = "SIGNÁL"
            textSize = 30f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            background = roundedBackground(Color.rgb(28, 132, 67), dp(24))
            setOnClickListener {
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

    private fun updateVolumeStatus() {
        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val current = audio.getStreamVolume(AudioManager.STREAM_ALARM)
        val maximum = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val fixed = audio.isVolumeFixed

        volumeStatus.text = when {
            fixed -> "Hlasitosť budíka: zariadenie nepovoľuje zmenu"
            current == maximum -> "Hlasitosť budíka: maximum ($current/$maximum)"
            else -> "Hlasitosť budíka: $current/$maximum — maximum sa nepodarilo nastaviť"
        }
    }

    private fun endWorkAlertSession() {
        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val saved = prefs.getInt("original_alarm_volume", -1)

        if (saved >= 0) {
            try {
                audio.setStreamVolume(AudioManager.STREAM_ALARM, saved, 0)
            } catch (_: SecurityException) {
            }
        }
        prefs.edit().clear().apply()
    }

    @Deprecated("Use the X button to end WorkAlert mode.")
    override fun onBackPressed() {
        // WorkAlert mode is ended with the X button.
    }

    private fun roundedBackground(color: Int, radius: Int) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
