package sk.workalert

import com.google.firebase.messaging.FirebaseMessagingService

class WorkAlertFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FirebaseBootstrap.storeNewToken(this, token)
    }
}
