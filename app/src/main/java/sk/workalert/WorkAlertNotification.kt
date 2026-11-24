package sk.workalert

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build

object WorkAlertNotification {
    private const val CHANNEL_ID = "workalert_status"
    private const val NOTIFICATION_ID = 1001

    const val ACTION_SIGNAL = "sk.workalert.ACTION_SIGNAL"
    const val ACTION_STOP = "sk.workalert.ACTION_STOP"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "WorkAlert",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Trvalé oznámenie WorkAlert s tlačidlom na odoslanie signálu"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }

            val manager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun show(context: Context) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        createChannel(context)

        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val current = audio.getStreamVolume(AudioManager.STREAM_ALARM)
        val maximum = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val status = if (audio.isVolumeFixed) {
            "Hlasitosť budíka: bez zmeny"
        } else {
            "Hlasitosť budíka: $current/$maximum"
        }

        val signalIntent = Intent(context, AlertActionReceiver::class.java).apply {
            action = ACTION_SIGNAL
        }
        val signalPendingIntent = PendingIntent.getBroadcast(
            context,
            1002,
            signalIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, AlertActionReceiver::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            1003,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = Intent(context, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            context,
            1004,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }

        val notification = builder
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("WorkAlert")
            .setContentText("Aktívny • $status")
            .setStyle(
                Notification.BigTextStyle()
                    .bigText("WorkAlert je aktívny.\n$status\nRozbaľ oznámenie a stlač SIGNÁL.")
            )
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(openPendingIntent)
            .addAction(
                android.R.drawable.ic_lock_idle_alarm,
                "SIGNÁL",
                signalPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "VYPNÚŤ",
                stopPendingIntent
            )
            .build()

        val manager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    fun cancel(context: Context) {
        val manager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
    }
}
