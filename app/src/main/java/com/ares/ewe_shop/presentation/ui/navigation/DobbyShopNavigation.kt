package com.ares.ewe_shop.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ares.ewe_shop.di.SessionEventBusEntryPoint
import com.ares.ewe_shop.presentation.ui.auth.otp.OtpScreen
import com.ares.ewe_shop.presentation.ui.auth.phone.PhoneScreen
import com.ares.ewe_shop.presentation.ui.main.MainScreen
import com.ares.ewe_shop.presentation.ui.orders.OrderDetailScreen
import com.ares.ewe_shop.presentation.ui.splash.SplashScreen
import dagger.hilt.android.EntryPointAccessors

@Composable
fun DobbyShopNavigation() {
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
        startDestination = DobbyShopScreens.Splash
    ) {
        composable(DobbyShopScreens.Splash) {
            SplashScreen(
                onOpenAuth = {
                    navController.navigate(DobbyShopScreens.Phone) {
                        popUpTo(DobbyShopScreens.Splash) { inclusive = true }
                    }
                },
                onOpenHome = {
                    navController.navigate(DobbyShopScreens.Main) {
                        popUpTo(DobbyShopScreens.Splash) { inclusive = true }
                    }
                }
            )
        }
        composable(DobbyShopScreens.Phone) {
            PhoneScreen(
                onCodeSent = { phone ->
                    navController.navigate(DobbyShopScreens.otp(phone)) {
                        popUpTo(DobbyShopScreens.Phone) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = DobbyShopScreens.Otp,
            arguments = listOf(navArgument("phone") { type = NavType.StringType })
        ) { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            OtpScreen(
                phone = phone,
                onVerified = {
                    navController.navigate(DobbyShopScreens.Main) {
                        popUpTo(DobbyShopScreens.Otp) { inclusive = true }
                    }
                }
            )
        }
        composable(DobbyShopScreens.Main) {
            MainScreen(
                onOrderClick = { order ->
                    navController.navigate(DobbyShopScreens.orderDetail(order.id))
                },
                onLogout = {
                    navController.navigate(DobbyShopScreens.Phone) {
                        popUpTo(DobbyShopScreens.Main) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = DobbyShopScreens.OrderDetail,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) {
            OrderDetailScreen(
                onBack = { navController.popBackStack() },
                onAcceptOrRejectSuccess = { navController.popBackStack() }
            )
        }
    }
}
