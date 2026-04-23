package com.ares.ewe_shop.presentation.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.ares.ewe_shop.data.remote.model.ShopOrderDto
import com.ares.ewe_shop.data.remote.model.ShopProductDto
import com.ares.ewe_shop.presentation.ui.orders.OrdersScreen
import com.ares.ewe_shop.presentation.ui.product.CreateProductScreen
import com.ares.ewe_shop.presentation.ui.product.ShopProductsScreen
import com.ares.ewe_shop.presentation.ui.profile.ProfileScreen

sealed class MainTab(
    val title: String,
    val icon: ImageVector
) {
    data object Orders : MainTab("Pedidos", Icons.Default.List)
    data object Products : MainTab("Productos", Icons.Default.Inventory2)
    data object Profile : MainTab("Perfil", Icons.Default.Person)
}

/**
 * El teclado usa [adjustNothing] a nivel Activity para que la ventana no encoja y la barra inferior
 * no suba. Solo el área central lleva [imePadding] para desplazar el contenido sobre el IME.
 */
@Composable
fun MainScreen(
    onOrderClick: (ShopOrderDto) -> Unit,
    onLogout: () -> Unit,
    /** Contador en SavedStateHandle de la ruta Main; al volver del detalle sube y dispara refresh en pedidos. */
    ordersRefreshGeneration: StateFlow<Int>? = null,
) {
    val ordersRefreshFlow = ordersRefreshGeneration ?: remember { MutableStateFlow(0) }
    val ordersRefreshGen by ordersRefreshFlow.collectAsStateWithLifecycle(0)

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showCreateProduct by rememberSaveable { mutableStateOf(false) }
    var productCreatedMessage by remember { mutableStateOf<String?>(null) }
    var productBeingEdited by remember { mutableStateOf<ShopProductDto?>(null) }
    val tabs = listOf(MainTab.Orders, MainTab.Products, MainTab.Profile)

    Column(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .imePadding()
        ) {
            if (showCreateProduct) {
                CreateProductScreen(
                    onBack = {
                        showCreateProduct = false
                        productBeingEdited = null
                    },
                    productToEdit = productBeingEdited,
                    onCreateSuccess = { msg ->
                        showCreateProduct = false
                        productBeingEdited = null
                        selectedTab = 1
                        productCreatedMessage = msg
                    }
                )
            } else {
                when (selectedTab) {
                    0 -> OrdersScreen(
                        onOrderClick = onOrderClick,
                        ordersRefreshGeneration = ordersRefreshGen,
                    )
                    1 -> ShopProductsScreen(
                        onNuevoClick = {
                            productBeingEdited = null
                            showCreateProduct = true
                        },
                        onProductClick = { product ->
                            productBeingEdited = product
                            showCreateProduct = true
                        },
                        postCreateMessage = productCreatedMessage,
                        onPostCreateMessageConsumed = { productCreatedMessage = null }
                    )
                    2 -> ProfileScreen(onLogout = onLogout)
                }
            }
        }
        if (!showCreateProduct) {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}
