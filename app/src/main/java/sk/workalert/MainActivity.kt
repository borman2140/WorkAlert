package sk.workalert

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.util.Log

class MainActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WorkAlertNotification.createChannel(this)
        WorkAlertSession.start(this)
        FirebaseBootstrap.start(this)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
        } else {
            WorkAlertNotification.show(this)
        }
        buildScreen()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATIONS && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            WorkAlertNotification.show(this)
        }
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(24), dp(24), dp(24))
            setBackgroundColor(Color.rgb(18, 22, 29))
        }
        root.addView(TextView(this).apply {
            text = "WorkAlert"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        })
        status = TextView(this).apply {
            text = "Pripárovanie telefónov"
            textSize = 15f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, dp(18))
        }
        root.addView(status)

        val create = makeButton("VYTVORIŤ KÓD", Color.rgb(0, 125, 190))
        create.setOnClickListener {
            status.text = "Vytváram kód…"
            WorkAlertBackend.createPairCode { result ->
                runOnUiThread {
                    result.onSuccess { (code, seconds) ->
                        status.text = "Kód pre druhý telefón: $code\nPlatnosť: ${seconds / 60} min."
                    }.onFailure { showError(it) }
                }
            }
        }
        root.addView(create, buttonParams())

        val codeInput = EditText(this).apply {
            hint = "Zadaj 4-miestny kód"
            textSize = 18f
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = GradientDrawable().apply {
                setColor(Color.rgb(42, 48, 58))
                cornerRadius = dp(12).toFloat()
            }
        }
        root.addView(codeInput, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(54)
        ).apply { topMargin = dp(14) })

        val join = makeButton("PRIPOJIŤ TENTO TELEFÓN", Color.rgb(0, 150, 65))
        join.setOnClickListener {
            val code = codeInput.text.toString().trim()
            if (!code.matches(Regex("\\d{4}"))) {
                status.text = "Zadaj presne 4 číslice."
                return@setOnClickListener
            }
            status.text = "Pripájam telefón…"
            WorkAlertBackend.joinWithCode(code) { result ->
                runOnUiThread {
                    result.onSuccess { status.text = "Telefóny sú spárované." }
                        .onFailure { showError(it) }
                }
            }
        }
        root.addView(join, buttonParams().apply { topMargin = dp(12) })

        val send = makeButton("ODOSLAŤ SIGNÁL", Color.rgb(180, 45, 45))
        send.setOnClickListener {
            AlertTone.vibrateOnly(this)
            WorkAlertBackend.sendSignal { result ->
                runOnUiThread {
                    result.onSuccess { status.text = "Signál odoslaný." }
                        .onFailure { showError(it) }
                }
            }
        }
        root.addView(send, buttonParams().apply { topMargin = dp(20) })

        val test = makeButton("TEST ZVUKU", Color.rgb(75, 80, 90))
        test.setOnClickListener { AlertTone.play(this) }
        root.addView(test, buttonParams().apply { topMargin = dp(12) })

        val stop = makeButton("VYPNÚŤ WORKALERT", Color.rgb(55, 60, 70))
        stop.setOnClickListener {
            WorkAlertSession.stop(this)
            WorkAlertNotification.cancel(this)
            finish()
        }
        root.addView(stop, buttonParams().apply { topMargin = dp(12) })
        setContentView(root)
    }

    private fun showError(error: Throwable) {
        Log.e("WorkAlert", "Backend operation failed", error)
        status.text = error.localizedMessage ?: "Chyba pripojenia. Skús to znova."
    }

    private fun makeButton(label: String, color: Int) = TextView(this).apply {
        text = label
        textSize = 16f
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        isClickable = true
        isFocusable = true
        background = GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(16).toFloat()
        }
    }

    private fun buttonParams() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, dp(58)
    )

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object { private const val REQUEST_NOTIFICATIONS = 1001 }
}

object WorkAlertSession {
    private const val PREFS = "workalert_session"
    private const val KEY_ACTIVE = "session_active"
    private const val KEY_ORIGINAL_VOLUME = "original_alarm_volume"

    fun start(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        if (!prefs.getBoolean(KEY_ACTIVE, false)) {
            prefs.edit()
                .putInt(KEY_ORIGINAL_VOLUME, audio.getStreamVolume(android.media.AudioManager.STREAM_ALARM))
                .putBoolean(KEY_ACTIVE, true).apply()
        }
        try {
            audio.setStreamVolume(
                android.media.AudioManager.STREAM_ALARM,
                audio.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM), 0
            )
        } catch (_: SecurityException) {}
    }

    fun stop(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val previous = prefs.getInt(KEY_ORIGINAL_VOLUME, -1)
        if (previous >= 0) {
            try { audio.setStreamVolume(android.media.AudioManager.STREAM_ALARM, previous, 0) }
            catch (_: SecurityException) {}
        }
        prefs.edit().clear().apply()
    }
}
