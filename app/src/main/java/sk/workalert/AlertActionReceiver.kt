package sk.workalert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlertActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WorkAlertNotification.ACTION_SIGNAL -> {
                // The sender gets vibration only; the paired phone plays the audible alert.
                AlertTone.vibrateOnly(context)
                val pendingResult = goAsync()
                WorkAlertBackend.sendSignal { result ->
                    result.onFailure { Log.e("WorkAlert", "Signal delivery failed", it) }
                    pendingResult.finish()
                }
            }

            WorkAlertNotification.ACTION_STOP -> {
                WorkAlertSession.stop(context)
                WorkAlertNotification.cancel(context)
            }
        }
    }
}
