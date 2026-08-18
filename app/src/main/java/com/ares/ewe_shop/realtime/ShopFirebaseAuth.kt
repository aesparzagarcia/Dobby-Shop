package com.ares.ewe_shop.realtime

import android.util.Log
import com.ares.ewe_shop.data.remote.api.DobbyShopApi
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShopFirebaseAuth @Inject constructor(
    private val api: DobbyShopApi,
) {
    /**
     * @return true when Firebase Auth signed in with a backend custom token.
     */
    suspend fun signInWithBackendToken(): Boolean {
        val customToken = try {
            api.getFirebaseCustomToken().token
        } catch (e: Exception) {
            Log.w(TAG, "shop/firebase-token failed: ${e.message}")
            return false
        }
        if (customToken.isBlank()) {
            Log.w(TAG, "shop/firebase-token returned empty token")
            return false
        }
        return try {
            FirebaseAuth.getInstance().signInWithCustomToken(customToken).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Auth signIn failed: ${e.message}")
            false
        }
    }

    fun signOut() {
        FirebaseAuth.getInstance().signOut()
    }

    private companion object {
        const val TAG = "ShopFirebaseAuth"
    }
}
