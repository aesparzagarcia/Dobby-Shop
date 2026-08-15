package com.ares.ewe_shop.presentation.ui.orders

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.ares.ewe_shop.core.theme.DobbyShopColors
import com.ares.ewe_shop.presentation.ui.main.LocalMainBottomBarPadding
import com.ares.ewe_shop.data.remote.model.ShopOrderDto
import com.ares.ewe_shop.data.remote.model.productsSubtotal
import com.ares.ewe_shop.presentation.viewmodel.orders.OrderStats
import com.ares.ewe_shop.presentation.viewmodel.orders.OrdersViewModel
import com.ares.ewe_shop.core.util.OrderDateFormat
import java.util.Locale


private fun formatOrderDate(createdAt: String): String = OrderDateFormat.formatList(createdAt)

private fun formatOrderId(id: String): String {
    val compact = id.replace("-", "").uppercase(Locale.getDefault())
    val suffix = compact.takeLast(4).ifEmpty { compact }
    return "#$suffix"
}

private fun productCountLabel(order: ShopOrderDto): String {
    val count = order.items.sumOf { it.quantity }
    return if (count == 1) "1 producto" else "$count productos"
}

@Composable
fun OrdersScreen(
    onOrderClick: (ShopOrderDto) -> Unit,
    ordersRefreshGeneration: Int = 0,
    viewModel: OrdersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val bottomBarPadding = LocalMainBottomBarPadding.current
    val context = LocalContext.current
    var notificationsEnabled by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> notificationsEnabled = granted }

    LaunchedEffect(ordersRefreshGeneration) {
        if (ordersRefreshGeneration > 0) viewModel.refresh()
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    val shopLabel = uiState.shopDisplayName?.takeIf { it.isNotBlank() } ?: "Tu tienda"

    // Sin insets inferiores: la NavigationBar vive en MainScreen; el default del Scaffold
    // reservaría otra franja y dejaría hueco encima de la barra.
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        containerColor = DobbyShopColors.Background,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = bottomBarPadding + 8.dp),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OrdersHeader(
                shopName = shopLabel,
                sectionLabel = uiState.ordersLabel,
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refresh() },
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(orderStatusFilters(uiState.isCarWash), key = { it.label }) { chip ->
                    StatusFilterPill(
                        label = chip.label,
                        dotColor = chip.dotColor,
                        selected = uiState.selectedStatusFilter == chip.value,
                        onClick = { viewModel.setStatusFilter(chip.value) },
                    )
                }
            }
            when {
                uiState.isLoading && uiState.orders.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = DobbyShopColors.Purple)
                    }
                }
                uiState.orders.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OrderStatsCard(
                            stats = uiState.orderStats,
                            totalLabel = if (uiState.isCarWash) "Total lavadas" else "Total pedidos",
                            preparingLabel = if (uiState.isCarWash) "Lavando" else "Preparando",
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (uiState.isCarWash) "No hay servicios" else "No hay pedidos",
                                style = MaterialTheme.typography.bodyLarge,
                                color = DobbyShopColors.TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (!notificationsEnabled) {
                            NotificationBanner(
                                onActivate = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            OrderStatsCard(
                                stats = uiState.orderStats,
                                totalLabel = if (uiState.isCarWash) "Total lavadas" else "Total pedidos",
                                preparingLabel = if (uiState.isCarWash) "Lavando" else "Preparando",
                            )
                        }
                        items(uiState.orders, key = { it.id }) { order ->
                            OrderCard(
                                order = order,
                                isCarWash = uiState.isCarWash,
                                onClick = { onOrderClick(order) },
                            )
                        }
                        if (!notificationsEnabled) {
                            item {
                                NotificationBanner(
                                    onActivate = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrdersHeader(
    shopName: String,
    sectionLabel: String,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = shopName,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = DobbyShopColors.PurpleDark,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = sectionLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = DobbyShopColors.TextSecondary,
            )
        }
        Surface(
            onClick = onRefresh,
            enabled = !isRefreshing,
            shape = RoundedCornerShape(14.dp),
            color = DobbyShopColors.PurpleLight,
            modifier = Modifier.size(48.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = DobbyShopColors.Purple,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refrescar",
                        tint = DobbyShopColors.Purple,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusFilterPill(
    label: String,
    dotColor: Color?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    val background = if (selected) DobbyShopColors.Purple else DobbyShopColors.Surface
    val textColor = if (selected) Color.White else DobbyShopColors.TextSecondary
    val borderModifier = if (selected) {
        Modifier
    } else {
        Modifier.border(1.dp, DobbyShopColors.Border, shape)
    }

    Row(
        modifier = Modifier
            .clip(shape)
            .then(borderModifier)
            .background(background, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        dotColor?.let { color ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = textColor,
        )
    }
}

@Composable
private fun OrderStatsCard(
    stats: OrderStats,
    totalLabel: String,
    preparingLabel: String,
) {
    val columns = listOf(
        Triple(Icons.Default.ShoppingBag, DobbyShopColors.Purple, DobbyShopColors.PurpleLight) to
            (stats.total.toString() to totalLabel),
        Triple(Icons.Default.AccessTime, DobbyShopColors.Orange, DobbyShopColors.OrangeLight) to
            (stats.pending.toString() to "Pendientes"),
        Triple(Icons.Default.Inventory2, DobbyShopColors.Purple, DobbyShopColors.PurpleLight) to
            (stats.preparing.toString() to preparingLabel),
        Triple(Icons.Default.CheckCircle, DobbyShopColors.Green, DobbyShopColors.GreenLight) to
            (stats.delivered.toString() to "Entregados"),
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DobbyShopColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
        ) {
            columns.forEachIndexed { index, (icons, data) ->
                val (icon, iconTint, iconBackground) = icons
                val (value, label) = data
                StatColumn(
                    modifier = Modifier.weight(1f),
                    icon = icon,
                    iconTint = iconTint,
                    iconBackground = iconBackground,
                    value = value,
                    label = label,
                )
                if (index < columns.lastIndex) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(72.dp)
                            .background(DobbyShopColors.Border),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatColumn(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    value: String,
    label: String,
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = DobbyShopColors.TextPrimary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = DobbyShopColors.TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

@Composable
private fun OrderCard(
    order: ShopOrderDto,
    isCarWash: Boolean,
    onClick: () -> Unit,
) {
    val visual = orderStatusVisual(order.status, isCarWash)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DobbyShopColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = formatOrderId(order.id),
                        fontWeight = FontWeight.Bold,
                        color = DobbyShopColors.Purple,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = formatOrderDate(order.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = DobbyShopColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                StatusBadge(visual = visual)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "$${String.format(Locale.getDefault(), "%.2f", order.productsSubtotal())}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = DobbyShopColors.TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = DobbyShopColors.TextSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = productCountLabel(order),
                            style = MaterialTheme.typography.bodySmall,
                            color = DobbyShopColors.TextSecondary,
                        )
                    }
                    order.deliveryAddress?.takeIf { it.isNotBlank() }?.let { addr ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = DobbyShopColors.TextSecondary,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = addr,
                                style = MaterialTheme.typography.bodySmall,
                                color = DobbyShopColors.TextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DobbyShopColors.PurpleLight,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Ver detalle",
                            tint = DobbyShopColors.Purple,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    visual: OrderStatusVisual,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(visual.background)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = visual.icon,
            contentDescription = null,
            tint = visual.foreground,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = visual.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = visual.foreground,
            maxLines = 2,
            softWrap = true,
        )
    }
}

@Composable
private fun NotificationBanner(onActivate: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DobbyShopColors.PurpleLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DobbyShopColors.Surface,
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = DobbyShopColors.Purple,
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Mantente al tanto de tus pedidos",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = DobbyShopColors.PurpleDark,
                )
                Text(
                    text = "Activa las notificaciones para no perderte ninguna actualización.",
                    style = MaterialTheme.typography.bodySmall,
                    color = DobbyShopColors.TextSecondary,
                )
            }
            TextButton(onClick = onActivate) {
                Text(
                    text = "Activar >",
                    fontWeight = FontWeight.Bold,
                    color = DobbyShopColors.Purple,
                )
            }
        }
    }
}
