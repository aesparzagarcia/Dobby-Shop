package com.ares.ewe_shop.realtime

import com.ares.ewe_shop.data.local.datastore.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShopOrderRealtimeListener @Inject constructor(
    private val sessionManager: SessionManager,
    private val shopFirebaseAuth: ShopFirebaseAuth,
    private val orderRealtimeBus: OrderRealtimeBus,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var registration: ListenerRegistration? = null
    private var listeningShopId: String? = null

    fun start() {
        scope.launch {
            if (!sessionManager.isLoggedIn.first()) {
                stop()
                return@launch
            }
            shopFirebaseAuth.signInWithBackendToken()
            val shopId = sessionManager.shopId.first()?.trim().orEmpty()
            if (shopId.isEmpty()) return@launch
            if (shopId == listeningShopId && registration != null) return@launch

            stop()
            listeningShopId = shopId
            registration = FirebaseFirestore.getInstance()
                .collection("shops")
                .document(shopId)
                .collection("order_signals")
                .addSnapshotListener { _, _ ->
                    orderRealtimeBus.notifyOrdersChanged()
                }
        }
    }

    fun stop() {
        registration?.remove()
        registration = null
        listeningShopId = null
    }
}
