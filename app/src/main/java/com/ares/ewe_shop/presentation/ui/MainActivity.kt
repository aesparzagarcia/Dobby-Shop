package com.ares.ewe_shop.presentation.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.ares.ewe_shop.core.theme.DobbyShopTheme
import com.ares.ewe_shop.presentation.ui.navigation.DobbyShopNavigation
import com.ares.ewe_shop.realtime.OrderRealtimeBus
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

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) syncPushAndRealtime()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        requestNotifPermissionIfNeeded()
        setContent {
            DobbyShopTheme {
                DobbyShopApp()
            }
        }
    }

    override fun onStop() {
        wasStopped = true
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        if (wasStopped) {
            wasStopped = false
            orderRealtimeBus.notifyOrdersChanged()
        }
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
fun DobbyShopApp() {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DobbyShopNavigation()
        }
    }
}
