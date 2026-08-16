package com.ares.ewe_shop.presentation.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ares.ewe_shop.presentation.ui.deliverymap.CarWashDeliveryMapScreen
import com.ares.ewe_shop.presentation.ui.orders.OrderDetailScreen
import com.ares.ewe_shop.presentation.ui.orders.OrdersScreen
import com.ares.ewe_shop.presentation.viewmodel.orders.OrdersViewModel
import com.ares.ewe_shop.realtime.ShopOrderNotificationHelper

private object OrdersRoutes {
    const val List = "orders_list"
    const val Detail = "orders_detail/{orderId}"
    const val CarWashDelivery = "carwash_delivery/{orderId}"

    fun detail(orderId: String) = "orders_detail/$orderId"

    fun carWashDelivery(orderId: String) = "carwash_delivery/$orderId"
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
 * El mapa de entrega de carwash pide ocultar esa barra para ir a pantalla completa.
 */
@Composable
fun OrdersNavHost(
    rootNavController: NavController,
    mainViewModelStoreOwner: ViewModelStoreOwner,
    ordersRefreshGeneration: Int,
    pendingOrderId: String? = null,
    onPendingOrderNavigated: () -> Unit = {},
    onDeliveryMapVisibleChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val ordersNavController = rememberNavController()
    val context = LocalContext.current
    val currentRoute = ordersNavController.currentBackStackEntryAsState().value?.destination?.route
    val onDeliveryMap = currentRoute?.startsWith("carwash_delivery") == true

    LaunchedEffect(onDeliveryMap) {
        onDeliveryMapVisibleChange(onDeliveryMap)
    }

    DisposableEffect(Unit) {
        onDispose { onDeliveryMapVisibleChange(false) }
    }

    LaunchedEffect(pendingOrderId) {
        val orderId = pendingOrderId?.trim()?.takeIf { it.isNotEmpty() } ?: return@LaunchedEffect
        ShopOrderNotificationHelper.clearOrderNotifications(context.applicationContext, orderId)
        ordersNavController.navigate(OrdersRoutes.detail(orderId)) {
            launchSingleTop = true
        }
        onPendingOrderNavigated()
    }

    NavHost(
        navController = ordersNavController,
        startDestination = OrdersRoutes.List,
        modifier = modifier.fillMaxSize(),
    ) {
        composable(OrdersRoutes.List) {
            val ordersViewModel: OrdersViewModel = hiltViewModel(mainViewModelStoreOwner)
            val ordersState by ordersViewModel.uiState.collectAsState()
            OrdersScreen(
                onOrderClick = { order ->
                    // Carwash en ruta: el local es el repartidor, así que se abre el mapa de entrega.
                    val route = if (ordersState.isCarWash &&
                        order.status in setOf("OUT_FOR_PICKUP", "PICKED_UP", "ON_DELIVERY")
                    ) {
                        OrdersRoutes.carWashDelivery(order.id)
                    } else {
                        OrdersRoutes.detail(order.id)
                    }
                    ordersNavController.navigate(route) {
                        launchSingleTop = true
                    }
                },
                ordersRefreshGeneration = ordersRefreshGeneration,
                viewModel = ordersViewModel,
            )
        }
        composable(
            route = OrdersRoutes.Detail,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId")
            LaunchedEffect(orderId) {
                orderId?.let {
                    ShopOrderNotificationHelper.clearOrderNotifications(
                        context.applicationContext,
                        it,
                    )
                }
            }
            val refreshOrdersList: () -> Unit = {
                incrementMainOrdersRefreshGen(rootNavController)
            }
            val popDetailAndRefreshOrders: () -> Unit = {
                refreshOrdersList()
                ordersNavController.popBackStack()
            }
            val openCarWashDeliveryMap: () -> Unit = {
                orderId?.let { id ->
                    refreshOrdersList()
                    ordersNavController.navigate(OrdersRoutes.carWashDelivery(id)) {
                        launchSingleTop = true
                    }
                }
            }
            OrderDetailScreen(
                onBack = popDetailAndRefreshOrders,
                onAcceptSuccess = refreshOrdersList,
                onRejectSuccess = popDetailAndRefreshOrders,
                onMarkPreparingSuccess = popDetailAndRefreshOrders,
                onReadyForPickupSuccess = popDetailAndRefreshOrders,
                onDetailingSuccess = popDetailAndRefreshOrders,
                onOnDeliverySuccess = openCarWashDeliveryMap,
                onOpenDeliveryMap = openCarWashDeliveryMap,
                onOpenPickupMap = openCarWashDeliveryMap,
            )
        }
        composable(
            route = OrdersRoutes.CarWashDelivery,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
        ) {
            val popMapAndRefreshOrders: () -> Unit = {
                incrementMainOrdersRefreshGen(rootNavController)
                ordersNavController.popBackStack(OrdersRoutes.List, inclusive = false)
            }
            CarWashDeliveryMapScreen(
                onBack = popMapAndRefreshOrders,
                onDeliveredSuccess = popMapAndRefreshOrders,
            )
        }
    }
}
