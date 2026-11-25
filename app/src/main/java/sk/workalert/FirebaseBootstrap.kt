package sk.workalert

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging

/** Creates a stable anonymous identity and registers this installation's FCM token. */
object FirebaseBootstrap {
    private const val TAG = "WorkAlertFirebase"
    private const val PREFS = "workalert_firebase"
    private const val KEY_UID = "firebase_uid"
    private const val KEY_FCM_TOKEN = "fcm_token"

    fun start(context: Context) {
        val appContext = context.applicationContext
        val auth = FirebaseAuth.getInstance()

        fun fetchTokenAndRegister() {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .edit().putString(KEY_FCM_TOKEN, token).apply()
                    WorkAlertBackend.registerDevice(token) { result ->
                        result.onSuccess { Log.i(TAG, "Device registered with WorkAlert backend") }
                            .onFailure { Log.e(TAG, "Device registration failed", it) }
                    }
                }
                .addOnFailureListener { Log.e(TAG, "Unable to obtain FCM token", it) }
        }

        val currentUser = auth.currentUser
        if (currentUser != null) {
            appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_UID, currentUser.uid).apply()
            fetchTokenAndRegister()
            return
        }

        auth.signInAnonymously()
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid != null) {
                    appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .edit().putString(KEY_UID, uid).apply()
                    fetchTokenAndRegister()
                    Log.i(TAG, "Anonymous Firebase sign-in completed")
                } else {
                    Log.e(TAG, "Anonymous sign-in returned no user")
                }
            }
            .addOnFailureListener { Log.e(TAG, "Anonymous sign-in failed", it) }
    }

    internal fun storeNewToken(context: Context, token: String) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_FCM_TOKEN, token).apply()

        if (FirebaseAuth.getInstance().currentUser != null) {
            WorkAlertBackend.registerDevice(token) { result ->
                result.onFailure { Log.e(TAG, "Refreshed token registration failed", it) }
            }
        }
    }
}
