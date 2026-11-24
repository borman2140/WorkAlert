
package sk.workalert

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private val prefs by lazy {
        getSharedPreferences("workalert_session", Context.MODE_PRIVATE)
    }

    private lateinit var volumeStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureWindow()
        startWorkAlertSession()
        buildScreen()
    }

    private fun configureWindow() {
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

        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

        // Touches outside the small panel can reach the lock screen.
        window.addFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        )
    }

    private fun startWorkAlertSession() {
        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (prefs.getBoolean("session_active", false)) {
            val previous = prefs.getInt("original_alarm_volume", -1)
            if (previous >= 0) {
                try {
                    audio.setStreamVolume(AudioManager.STREAM_ALARM, previous, 0)
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
            audio.setStreamVolume(
                AudioManager.STREAM_ALARM,
                audio.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                0
            )
        } catch (_: SecurityException) {
        }
    }

    private fun buildScreen() {
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = roundedBackground(
                Color.argb(235, 25, 31, 43),
                dp(24)
            )
        }

        // Close button
        val close = TextView(this).apply {
            text = "×"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            background = roundedBackground(
                Color.argb(150, 100, 108, 125),
                dp(16)
            )

            setOnClickListener {
                endWorkAlertSession()
                finish()
            }
        }

        panel.addView(close, LinearLayout.LayoutParams(
            dp(42),
            dp(42)
        ))

        // Divider
        val divider = android.view.View(this).apply {
            setBackgroundColor(Color.argb(100, 220, 225, 235))
        }

        panel.addView(divider, LinearLayout.LayoutParams(
            dp(1),
            dp(48)
        ).apply {
            leftMargin = dp(10)
            rightMargin = dp(10)
        })

        // Title and volume information
        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = "WorkAlert"
            textSize = 18f
            setTextColor(Color.WHITE)
            maxLines = 1
        }

        info.addView(title)

        volumeStatus = TextView(this).apply {
            textSize = 10f
            setTextColor(Color.LTGRAY)
            maxLines = 1
        }

        info.addView(volumeStatus)
        updateVolumeStatus()

        panel.addView(info, LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        ))

        // Signal button
        val signal = TextView(this).apply {
            text = "SIGNÁL"
            textSize = 19f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            background = roundedBackground(
                Color.rgb(0, 160, 65),
                dp(18)
            )

            setOnClickListener {
                AlertTone.play(this@MainActivity)
            }
        }

        panel.addView(signal, LinearLayout.LayoutParams(
            dp(112),
            dp(66)
        ).apply {
            leftMargin = dp(8)
        })

        root.addView(panel, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        setContentView(root)

        // Restrict the Activity window to the small top panel.
        val statusBarId = resources.getIdentifier(
            "status_bar_height",
            "dimen",
            "android"
        )

        val statusBarHeight = if (statusBarId > 0) {
            resources.getDimensionPixelSize(statusBarId)
        } else {
            dp(24)
        }

        window.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL)
        window.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            dp(124)
        )

        window.attributes = window.attributes.apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = statusBarHeight + dp(4)
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = dp(124)
            dimAmount = 0f
        }
    }

    private fun updateVolumeStatus() {
        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val current = audio.getStreamVolume(AudioManager.STREAM_ALARM)
        val maximum = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)

        volumeStatus.text = if (audio.isVolumeFixed) {
            "Hlasitosť budíka: bez zmeny"
        } else {
            "Hlasitosť budíka: maximum ($current/$maximum)"
        }
    }

    private fun endWorkAlertSession() {
        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val previous = prefs.getInt("original_alarm_volume", -1)

        if (previous >= 0) {
            try {
                audio.setStreamVolume(AudioManager.STREAM_ALARM, previous, 0)
            } catch (_: SecurityException) {
            }
        }

        prefs.edit().clear().apply()
    }

    @Deprecated("Use the X button to end WorkAlert mode.")
    override fun onBackPressed() {
        // Close WorkAlert using the X button.
    }

    private fun roundedBackground(color: Int, radius: Int) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
