package com.ares.ewe_shop.presentation.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.ares.ewe_shop.R
import com.ares.ewe_shop.core.theme.DobbyShopTheme
import com.ares.ewe_shop.presentation.ui.navigation.DobbyShopNavigation
import com.ares.ewe_shop.realtime.OrderRealtimeBus
import com.ares.ewe_shop.realtime.ShopOrderNotificationHelper
import com.ares.ewe_shop.realtime.ShopPushTokenRegistrar
import com.ares.ewe_shop.realtime.ShopRealtimeCoordinator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var pushTokenRegistrar: ShopPushTokenRegistrar

    @Inject
    lateinit var shopRealtimeCoordinator: ShopRealtimeCoordinator

    @Inject
    lateinit var orderRealtimeBus: OrderRealtimeBus

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** True tras [onStop] (app en background u otra activity encima). */
    private var wasStopped = false

    private var pendingOrderId by mutableStateOf<String?>(null)

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) syncPushAndRealtime()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_DobbyShop)
        super.onCreate(savedInstanceState)
        consumeOrderIdFromIntent(intent)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        requestNotifPermissionIfNeeded()
        setContent {
            DobbyShopTheme {
                DobbyShopApp(
                    pendingOrderId = pendingOrderId,
                    onPendingOrderNavigated = { pendingOrderId = null },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ShopOrderNotificationHelper.clearAllOrderNotifications(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeOrderIdFromIntent(intent)
    }

    override fun onStop() {
        wasStopped = true
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        if (wasStopped) {
            wasStopped = false
            // Background FCM often never hits the realtime bus; re-attach Firestore + force refresh.
            shopRealtimeCoordinator.resumeAfterBackground()
            orderRealtimeBus.notifyOrdersChanged()
        }
    }

    private fun consumeOrderIdFromIntent(intent: Intent?) {
        if (intent == null) return
        val extras = intent.extras

        fun extra(key: String): String? =
            intent.getStringExtra(key)?.trim()?.takeIf { it.isNotEmpty() }
                ?: extras?.getString(key)?.trim()?.takeIf { it.isNotEmpty() }

        val orderId = extra(ShopOrderNotificationHelper.EXTRA_ORDER_ID) ?: extra("order_id")
        if (orderId == null) return

        ShopOrderNotificationHelper.clearOrderNotifications(this, orderId)
        pendingOrderId = orderId
        intent.removeExtra(ShopOrderNotificationHelper.EXTRA_ORDER_ID)
        extras?.remove("order_id")
    }

    private fun requestNotifPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            syncPushAndRealtime()
            return
        }
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED -> syncPushAndRealtime()
            else -> notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun syncPushAndRealtime() {
        scope.launch {
            shopRealtimeCoordinator.onSessionReady()
        }
    }
}

@Composable
fun DobbyShopApp(
    pendingOrderId: String? = null,
    onPendingOrderNavigated: () -> Unit = {},
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        DobbyShopNavigation(
            pendingOrderId = pendingOrderId,
            onPendingOrderNavigated = onPendingOrderNavigated,
        )
    }
}
