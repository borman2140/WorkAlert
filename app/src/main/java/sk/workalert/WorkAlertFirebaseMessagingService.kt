package sk.workalert

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class WorkAlertFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FirebaseBootstrap.storeNewToken(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        if (message.data["type"] == "WORKALERT_SIGNAL") {
            Log.i("WorkAlertFCM", "Incoming WorkAlert signal received")
            AlertTone.play(applicationContext)
        }
    }
}
