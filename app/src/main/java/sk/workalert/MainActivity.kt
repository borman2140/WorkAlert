package sk.workalert

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WorkAlertNotification.createChannel(this)
        WorkAlertSession.start(this)
        FirebaseBootstrap.start(this)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATIONS
            )
        } else {
            WorkAlertNotification.show(this)
        }

        buildScreen()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATIONS) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                WorkAlertNotification.show(this)
            }
        }
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(28), dp(28), dp(28))
            setBackgroundColor(Color.rgb(18, 22, 29))
        }

        val title = TextView(this).apply {
            text = "WorkAlert"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        root.addView(title)

        val info = TextView(this).apply {
            text = "WorkAlert je aktívny.\nOvládanie nájdeš v oznámení na obrazovke uzamknutia alebo v paneli oznámení."
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, dp(20), 0, dp(28))
        }
        root.addView(info)

        val test = makeButton("TEST SIGNÁLU", Color.rgb(0, 150, 65))
        test.setOnClickListener { AlertTone.play(this) }
        root.addView(test, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(64)
        ))

        val stop = makeButton("VYPNÚŤ WORKALERT", Color.rgb(75, 80, 90))
        stop.setOnClickListener {
            WorkAlertSession.stop(this)
            WorkAlertNotification.cancel(this)
            finish()
        }
        root.addView(stop, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(56)
        ).apply { topMargin = dp(14) })

        setContentView(root)
    }

    private fun makeButton(label: String, color: Int) =
        TextView(this).apply {
            text = label
            textSize = 17f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            background = GradientDrawable().apply {
                setColor(color)
                cornerRadius = dp(18).toFloat()
            }
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val REQUEST_NOTIFICATIONS = 1001
    }
}

object WorkAlertSession {
    private const val PREFS = "workalert_session"
    private const val KEY_ACTIVE = "session_active"
    private const val KEY_ORIGINAL_VOLUME = "original_alarm_volume"

    fun start(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (!prefs.getBoolean(KEY_ACTIVE, false)) {
            prefs.edit()
                .putInt(KEY_ORIGINAL_VOLUME, audio.getStreamVolume(AudioManager.STREAM_ALARM))
                .putBoolean(KEY_ACTIVE, true)
                .apply()
        }

        try {
            audio.setStreamVolume(
                AudioManager.STREAM_ALARM,
                audio.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                0
            )
        } catch (_: SecurityException) {
        }
    }

    fun stop(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val previous = prefs.getInt(KEY_ORIGINAL_VOLUME, -1)

        if (previous >= 0) {
            try {
                audio.setStreamVolume(AudioManager.STREAM_ALARM, previous, 0)
            } catch (_: SecurityException) {
            }
        }

        prefs.edit().clear().apply()
    }
}
