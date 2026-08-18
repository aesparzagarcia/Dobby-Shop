package com.ares.ewe_shop.realtime

import android.util.Log
import com.ares.ewe_shop.data.local.datastore.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
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
    private var pollJob: Job? = null

    fun start() {
        scope.launch {
            if (!sessionManager.isLoggedIn.first()) {
                stop()
                return@launch
            }
            val shopId = sessionManager.shopId.first()?.trim().orEmpty()
            if (shopId.isEmpty()) return@launch

            val signedIn = shopFirebaseAuth.signInWithBackendToken()
            if (signedIn) {
                stopPolling()
                if (shopId == listeningShopId && registration != null) return@launch
                stopListener()
                listeningShopId = shopId
                registration = FirebaseFirestore.getInstance()
                    .collection("shops")
                    .document(shopId)
                    .collection("order_signals")
                    .addSnapshotListener { _, error ->
                        if (error != null) {
                            Log.e(TAG, "Firestore order_signals: ${error.message}")
                        }
                        // Any snapshot (incl. reconnect) should wake UI.
                        orderRealtimeBus.notifyOrdersChanged()
                    }
            } else {
                // Dev without Firebase Admin / token: poll so iOS ↔ Android still sync.
                stopListener()
                startPolling()
            }
        }
    }

    /** Force re-attach after background (parity with Dobby consumer / iOS resume). */
    fun resume() {
        stop()
        start()
    }

    fun stop() {
        stopListener()
        stopPolling()
    }

    private fun stopListener() {
        registration?.remove()
        registration = null
        listeningShopId = null
    }

    private fun startPolling() {
        if (pollJob?.isActive == true) return
        Log.w(TAG, "Firebase auth unavailable — polling orders every ${POLL_MS}ms")
        pollJob = scope.launch {
            while (isActive) {
                delay(POLL_MS)
                orderRealtimeBus.notifyOrdersChanged()
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private companion object {
        const val TAG = "ShopOrderRealtime"
        const val POLL_MS = 3_000L
    }
}
