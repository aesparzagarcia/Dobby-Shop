package com.ares.ewe_shop.presentation.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.ares.ewe_shop.data.remote.model.ShopOrderDto
import com.ares.ewe_shop.presentation.ui.orders.OrdersScreen
import com.ares.ewe_shop.presentation.ui.product.CreateProductScreen
import com.ares.ewe_shop.presentation.ui.profile.ProfileScreen

sealed class MainTab(
    val title: String,
    val icon: ImageVector
) {
    data object Orders : MainTab("Pedidos", Icons.Default.List)
    data object CreateProduct : MainTab("Nuevo producto", Icons.Default.Add)
    data object Profile : MainTab("Perfil", Icons.Default.Person)
}

@Composable
fun MainScreen(
    onOrderClick: (ShopOrderDto) -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(MainTab.Orders, MainTab.CreateProduct, MainTab.Profile)

    Scaffold(
        bottomBar = {
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
    ) {
        when (selectedTab) {
            0 -> OrdersScreen(onOrderClick = onOrderClick)
            1 -> CreateProductScreen()
            2 -> ProfileScreen(onLogout = onLogout)
        }
    }
}
