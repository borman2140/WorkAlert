package sk.workalert

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.RemoteViews

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
                description = "WorkAlert notification controls"
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

        val compactView = RemoteViews(
            context.packageName,
            R.layout.notification_workalert
        ).apply {
            setOnClickPendingIntent(R.id.action_stop, stopPendingIntent)
            setOnClickPendingIntent(R.id.action_signal, signalPendingIntent)
        }

        val expandedView = RemoteViews(
            context.packageName,
            R.layout.notification_workalert_expanded
        ).apply {
            setOnClickPendingIntent(R.id.action_stop, stopPendingIntent)
            setOnClickPendingIntent(R.id.action_signal, signalPendingIntent)
        }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }

        val notification = builder
            .setSmallIcon(R.drawable.workalert_notification_small)
            .setContentTitle("WorkAlert")
            .setContentText("WorkAlert active")
            .setCustomContentView(compactView)
            .setCustomBigContentView(expandedView)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(openPendingIntent)
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
