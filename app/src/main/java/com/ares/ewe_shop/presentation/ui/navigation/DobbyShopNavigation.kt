package com.ares.ewe_shop.presentation.ui.navigation

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ares.ewe_shop.di.SessionEventBusEntryPoint
import com.ares.ewe_shop.presentation.ui.auth.otp.OtpScreen
import com.ares.ewe_shop.presentation.ui.auth.phone.PhoneScreen
import com.ares.ewe_shop.presentation.ui.main.MainScreen
import com.ares.ewe_shop.presentation.ui.orders.SearchingDriverScreen
import com.ares.ewe_shop.presentation.ui.splash.SplashScreen
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.StateFlow

@Composable
fun DobbyShopNavigation(
    pendingOrderId: String? = null,
    onPendingOrderNavigated: () -> Unit = {},
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sessionEventBus = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            SessionEventBusEntryPoint::class.java
        ).sessionEventBus()
    }
    LaunchedEffect(sessionEventBus) {
        sessionEventBus.sessionExpired.collect {
            navController.navigate(DobbyShopScreens.Phone) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
    NavHost(
        navController = navController,
        startDestination = DobbyShopScreens.Splash,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable(DobbyShopScreens.Splash) {
            SplashScreen(
                onOpenAuth = {
                    navController.navigate(DobbyShopScreens.Phone) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onOpenHome = {
                    navController.navigate(DobbyShopScreens.Main) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(DobbyShopScreens.Phone) {
            val activity = LocalContext.current as? Activity
            BackHandler {
                activity?.finish()
            }
            PhoneScreen(
                onCodeSent = { phone ->
                    navController.navigate(DobbyShopScreens.otp(phone)) {
                        popUpTo(DobbyShopScreens.Phone) { inclusive = false }
                    }
                }
            )
        }
        composable(
            route = DobbyShopScreens.Otp,
            arguments = listOf(navArgument("phone") { type = NavType.StringType })
        ) {
            OtpScreen(
                onBack = { navController.popBackStack() },
                onVerified = {
                    navController.navigate(DobbyShopScreens.Main) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(DobbyShopScreens.Main) { mainBackStackEntry ->
            val activity = LocalContext.current as? Activity
            BackHandler {
                activity?.finish()
            }
            val ordersRefreshGeneration: StateFlow<Int> =
                mainBackStackEntry.savedStateHandle.getStateFlow(KEY_ORDERS_REFRESH_GEN, 0)
            MainScreen(
                rootNavController = navController,
                mainViewModelStoreOwner = mainBackStackEntry,
                onLogout = {
                    navController.navigate(DobbyShopScreens.Phone) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                ordersRefreshGeneration = ordersRefreshGeneration,
                pendingOrderId = pendingOrderId,
                onPendingOrderNavigated = onPendingOrderNavigated,
            )
        }
        composable(DobbyShopScreens.SearchingDriver) {
            SearchingDriverScreen(
                onNavigateHome = {
                    incrementMainOrdersRefreshGen(navController)
                    navController.popBackStack()
                },
            )
        }
    }
}
