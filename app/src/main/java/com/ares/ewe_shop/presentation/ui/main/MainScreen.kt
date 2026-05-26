package com.ares.ewe_shop.presentation.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.ewe_shop.core.theme.DobbyShopColors
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
 * Activity con [adjustNothing]: el teclado no redimensiona la ventana.
 * El área central usa insets del IME restando la altura medida de la barra inferior, para no
 * duplicar espacio ni quitar la barra del árbol (transición más fluida).
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
    var bottomBarHeightPx by remember { mutableIntStateOf(0) }
    val bottomBarInsets = WindowInsets(bottom = bottomBarHeightPx)
    val contentImeInsets = if (showCreateProduct) {
        WindowInsets.ime
    } else {
        WindowInsets.ime.exclude(bottomBarInsets)
    }

    Column(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .windowInsetsPadding(contentImeInsets),
            contentAlignment = Alignment.TopStart,
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
            DobbyShopBottomBar(
                modifier = Modifier.onSizeChanged { bottomBarHeightPx = it.height },
                tabs = tabs,
                selectedIndex = selectedTab,
                onTabSelected = { selectedTab = it },
            )
        }
    }
}

@Composable
private fun DobbyShopBottomBar(
    modifier: Modifier = Modifier,
    tabs: List<MainTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
) {
    Surface(
        modifier = modifier.navigationBarsPadding(),
        color = DobbyShopColors.Surface,
        shadowElevation = 12.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = selectedIndex == index
                DobbyShopBottomBarItem(
                    tab = tab,
                    selected = selected,
                    onClick = { onTabSelected(index) },
                )
            }
        }
    }
}

@Composable
private fun DobbyShopBottomBarItem(
    tab: MainTab,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val iconColor = if (selected) DobbyShopColors.Purple else DobbyShopColors.TextSecondary
    val textColor = if (selected) DobbyShopColors.Purple else DobbyShopColors.TextSecondary
    val pillColor = if (selected) DobbyShopColors.PurpleLight else Color.Transparent

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(pillColor)
                .padding(horizontal = 14.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.title,
                tint = iconColor,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = tab.title,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = textColor,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
