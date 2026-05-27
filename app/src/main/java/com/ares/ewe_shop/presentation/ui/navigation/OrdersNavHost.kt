package com.ares.ewe_shop.presentation.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ares.ewe_shop.presentation.ui.orders.OrderDetailScreen
import com.ares.ewe_shop.presentation.ui.orders.OrdersScreen

private object OrdersRoutes {
    const val List = "orders_list"
    const val Detail = "orders_detail/{orderId}"

    fun detail(orderId: String) = "orders_detail/$orderId"
}

/** Se incrementa al volver del detalle para que [OrdersScreen] recargue la lista. */
internal const val KEY_ORDERS_REFRESH_GEN = "orders_list_refresh_gen"

internal fun incrementMainOrdersRefreshGen(rootNavController: NavController) {
    runCatching {
        rootNavController.getBackStackEntry(DobbyShopScreens.Main).savedStateHandle
    }.getOrNull()?.let { handle ->
        val next = (handle.get<Int>(KEY_ORDERS_REFRESH_GEN) ?: 0) + 1
        handle[KEY_ORDERS_REFRESH_GEN] = next
    }
}

/**
 * Lista + detalle de pedidos dentro del área sobre la barra inferior de [MainScreen],
 * para que el contenido no quede tapado por el menú de tabs.
 */
@Composable
fun OrdersNavHost(
    rootNavController: NavController,
    mainViewModelStoreOwner: ViewModelStoreOwner,
    ordersRefreshGeneration: Int,
    modifier: Modifier = Modifier,
) {
    val ordersNavController = rememberNavController()

    NavHost(
        navController = ordersNavController,
        startDestination = OrdersRoutes.List,
        modifier = modifier.fillMaxSize(),
    ) {
        composable(OrdersRoutes.List) {
            OrdersScreen(
                onOrderClick = { order ->
                    ordersNavController.navigate(OrdersRoutes.detail(order.id)) {
                        launchSingleTop = true
                    }
                },
                ordersRefreshGeneration = ordersRefreshGeneration,
                viewModel = hiltViewModel(mainViewModelStoreOwner),
            )
        }
        composable(
            route = OrdersRoutes.Detail,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
        ) {
            val popDetailAndRefreshOrders: () -> Unit = {
                incrementMainOrdersRefreshGen(rootNavController)
                ordersNavController.popBackStack()
            }
            OrderDetailScreen(
                onBack = popDetailAndRefreshOrders,
                onAcceptOrRejectSuccess = popDetailAndRefreshOrders,
                onReadyForPickupSuccess = {
                    incrementMainOrdersRefreshGen(rootNavController)
                    rootNavController.navigate(DobbyShopScreens.SearchingDriver) {
                        popUpTo(DobbyShopScreens.Main) { inclusive = false }
                    }
                },
            )
        }
    }
}
