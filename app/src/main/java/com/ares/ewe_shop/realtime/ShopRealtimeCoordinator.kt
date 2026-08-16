package com.ares.ewe_shop.realtime

import com.ares.ewe_shop.data.local.datastore.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShopRealtimeCoordinator @Inject constructor(
    private val sessionManager: SessionManager,
    private val shopFirebaseAuth: ShopFirebaseAuth,
    private val pushTokenRegistrar: ShopPushTokenRegistrar,
    private val orderRealtimeListener: ShopOrderRealtimeListener,
    private val orderRealtimeBus: OrderRealtimeBus,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun onSessionReady() {
        scope.launch {
            if (!sessionManager.isLoggedIn.first()) return@launch
            shopFirebaseAuth.signInWithBackendToken()
            pushTokenRegistrar.registerCurrentToken()
            orderRealtimeListener.start()
        }
    }

    /**
     * After background, re-attach Firestore and wake UI collectors.
     * Needed because system tray FCM often skips [onMessageReceived], so the bus never fires.
     */
    fun resumeAfterBackground() {
        scope.launch {
            if (!sessionManager.isLoggedIn.first()) return@launch
            shopFirebaseAuth.signInWithBackendToken()
            orderRealtimeListener.resume()
            // Catch-up even if the first snapshot is delayed.
            orderRealtimeBus.notifyOrdersChanged()
        }
    }

    fun onLogout() {
        orderRealtimeListener.stop()
        shopFirebaseAuth.signOut()
        scope.launch {
            pushTokenRegistrar.unregisterOnServer()
        }
    }
}
