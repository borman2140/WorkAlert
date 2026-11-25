package sk.workalert

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging

/** Initializes a private anonymous Firebase identity and obtains this installation's FCM token. */
object FirebaseBootstrap {
    private const val TAG = "WorkAlertFirebase"
    private const val PREFS = "workalert_firebase"
    private const val KEY_UID = "firebase_uid"
    private const val KEY_FCM_TOKEN = "fcm_token"

    fun start(context: Context) {
        val appContext = context.applicationContext
        val auth = FirebaseAuth.getInstance()

        fun fetchTokenAndStore() {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .edit()
                        .putString(KEY_FCM_TOKEN, token)
                        .apply()
                    Log.i(TAG, "FCM token obtained and stored locally")
                }
                .addOnFailureListener { error ->
                    Log.e(TAG, "Unable to obtain FCM token", error)
                }
        }

        val currentUser = auth.currentUser
        if (currentUser != null) {
            appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_UID, currentUser.uid).apply()
            fetchTokenAndStore()
            return
        }

        auth.signInAnonymously()
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid != null) {
                    appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .edit().putString(KEY_UID, uid).apply()
                    fetchTokenAndStore()
                    Log.i(TAG, "Anonymous Firebase sign-in completed")
                } else {
                    Log.e(TAG, "Anonymous sign-in returned no user")
                }
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Anonymous Firebase sign-in failed", error)
            }
    }

    internal fun storeNewToken(context: Context, token: String) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FCM_TOKEN, token)
            .apply()
        Log.i(TAG, "FCM token refreshed and stored locally")
    }
}
