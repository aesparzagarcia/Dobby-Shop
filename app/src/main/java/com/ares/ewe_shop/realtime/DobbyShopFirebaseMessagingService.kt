package com.ares.ewe_shop.realtime

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DobbyShopFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var pushTokenRegistrar: ShopPushTokenRegistrar

    @Inject
    lateinit var orderRealtimeBus: OrderRealtimeBus

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch {
            pushTokenRegistrar.registerToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val type = message.data["type"] ?: return
        when (type) {
            "shop_new_order",
            "shop_courier_assigned",
            "order_status" -> orderRealtimeBus.notifyOrdersChanged()
        }
    }
}
