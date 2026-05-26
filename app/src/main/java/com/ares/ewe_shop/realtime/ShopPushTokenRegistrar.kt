package com.ares.ewe_shop.realtime

import com.ares.ewe_shop.data.local.datastore.SessionManager
import com.ares.ewe_shop.data.remote.api.DobbyShopApi
import com.ares.ewe_shop.data.remote.model.RegisterPushDeviceRequest
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShopPushTokenRegistrar @Inject constructor(
    private val api: DobbyShopApi,
    private val sessionManager: SessionManager,
) {
    suspend fun registerCurrentToken() {
        if (!sessionManager.isLoggedIn.first()) return
        val token = try {
            FirebaseMessaging.getInstance().token.await()
        } catch (_: Exception) {
            return
        }
        registerToken(token)
    }

    suspend fun registerToken(fcmToken: String) {
        if (!sessionManager.isLoggedIn.first()) return
        try {
            api.registerPushDevice(RegisterPushDeviceRequest(fcmToken = fcmToken))
        } catch (_: Exception) {
        }
    }

    suspend fun unregisterOnServer() {
        try {
            api.unregisterPushDevice()
        } catch (_: Exception) {
        }
    }
}
