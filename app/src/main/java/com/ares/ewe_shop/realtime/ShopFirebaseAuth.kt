package com.ares.ewe_shop.realtime

import com.ares.ewe_shop.data.remote.api.DobbyShopApi
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShopFirebaseAuth @Inject constructor(
    private val api: DobbyShopApi,
) {
    suspend fun signInWithBackendToken() {
        val customToken = try {
            api.getFirebaseCustomToken().token
        } catch (_: Exception) {
            return
        }
        if (customToken.isBlank()) return
        try {
            FirebaseAuth.getInstance().signInWithCustomToken(customToken).await()
        } catch (_: Exception) {
        }
    }

    fun signOut() {
        FirebaseAuth.getInstance().signOut()
    }
}
