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

    fun onLogout() {
        orderRealtimeListener.stop()
        shopFirebaseAuth.signOut()
        scope.launch {
            pushTokenRegistrar.unregisterOnServer()
        }
    }
}
