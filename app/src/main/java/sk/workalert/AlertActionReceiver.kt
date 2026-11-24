package sk.workalert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlertActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WorkAlertNotification.ACTION_SIGNAL -> {
                AlertTone.play(context)
            }

            WorkAlertNotification.ACTION_STOP -> {
                WorkAlertSession.stop(context)
                WorkAlertNotification.cancel(context)
            }
        }
    }
}
