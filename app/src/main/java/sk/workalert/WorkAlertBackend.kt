package sk.workalert

import com.google.firebase.functions.FirebaseFunctions

/**
 * All privileged database operations are performed by callable Cloud Functions.
 * The Android client never accesses Firestore directly.
 */
object WorkAlertBackend {
    private val functions: FirebaseFunctions by lazy {
        FirebaseFunctions.getInstance("europe-west1")
    }

    fun registerDevice(token: String, onComplete: (Result<Unit>) -> Unit = {}) {
        functions.getHttpsCallable("registerDevice")
            .call(mapOf("fcmToken" to token))
            .addOnSuccessListener { onComplete(Result.success(Unit)) }
            .addOnFailureListener { onComplete(Result.failure(it)) }
    }

    fun createPairCode(onComplete: (Result<Pair<String, Int>>) -> Unit) {
        functions.getHttpsCallable("createPairCode")
            .call()
            .addOnSuccessListener { result ->
                val data = result.getData() as? Map<*, *>
                val code = data?.get("code") as? String
                val seconds = (data?.get("expiresInSeconds") as? Number)?.toInt() ?: 300
                if (code.isNullOrBlank()) {
                    onComplete(Result.failure(IllegalStateException("Server returned no pairing code")))
                } else {
                    onComplete(Result.success(code to seconds))
                }
            }
            .addOnFailureListener { onComplete(Result.failure(it)) }
    }

    fun joinWithCode(code: String, onComplete: (Result<Unit>) -> Unit) {
        functions.getHttpsCallable("joinWithCode")
            .call(mapOf("code" to code))
            .addOnSuccessListener { onComplete(Result.success(Unit)) }
            .addOnFailureListener { onComplete(Result.failure(it)) }
    }

    fun sendSignal(onComplete: (Result<Unit>) -> Unit = {}) {
        functions.getHttpsCallable("sendSignal")
            .call()
            .addOnSuccessListener { onComplete(Result.success(Unit)) }
            .addOnFailureListener { onComplete(Result.failure(it)) }
    }
}
