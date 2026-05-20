package com.ares.ewe_shop.presentation.ui.orders

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ares.ewe_shop.data.remote.model.ShopOrderDto
import com.ares.ewe_shop.presentation.viewmodel.orders.OrdersViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val statusFilters = listOf(
    null to "Todos",
    "PENDING" to "Pendientes",
    "CONFIRMED" to "Confirmados",
    "PREPARING" to "En preparación",
    "READY_FOR_PICKUP" to "Listo para recoger",
    "ASSIGNED" to "Asignados",
    "ON_DELIVERY" to "En camino",
    "DELIVERED" to "Entregados",
    "CANCELLED" to "Cancelados"
)

private fun formatOrderDate(createdAt: String): String {
    return try {
        val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        val date = iso.parse(createdAt) ?: return createdAt
        SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(date)
    } catch (_: Exception) {
        createdAt
    }
}

private fun statusLabel(status: String): String = when (status) {
    "PENDING" -> "Pendiente"
    "CONFIRMED" -> "Confirmado"
    "PREPARING" -> "En preparación"
    "READY_FOR_PICKUP" -> "Listo para recoger"
    "ASSIGNED" -> "Asignado"
    "ON_DELIVERY" -> "En camino"
    "DELIVERED" -> "Entregado"
    "CANCELLED" -> "Cancelado"
    else -> status
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    onOrderClick: (ShopOrderDto) -> Unit,
    /** Copiado desde el StateFlow de Main vía collectAsStateWithLifecycle; al cambiar se recarga la lista. */
    ordersRefreshGeneration: Int = 0,
    viewModel: OrdersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(ordersRefreshGeneration) {
        if (ordersRefreshGeneration > 0) viewModel.loadOrders()
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    val shopLabel = uiState.shopDisplayName?.takeIf { it.isNotBlank() } ?: "Tu tienda"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = shopLabel,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Pedidos",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh() },
                        enabled = !uiState.isRefreshing
                    ) {
                        if (uiState.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(8.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(statusFilters, key = { (_, label) -> label }) { (value, label) ->
                    FilterChip(
                        selected = uiState.selectedStatusFilter == value,
                        onClick = { viewModel.setStatusFilter(value) },
                        label = { Text(label) }
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.isLoading && uiState.orders.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (uiState.orders.isEmpty()) {
                    item {
                        Text(
                            text = "No hay pedidos",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                        )
                    }
                } else {
                    items(uiState.orders, key = { it.id }) { order ->
                        OrderCard(
                            order = order,
                            onClick = { onOrderClick(order) }
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun OrderCard(
    order: ShopOrderDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),

        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatOrderDate(order.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = statusLabel(order.status),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$${String.format("%.2f", order.total)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            order.deliveryAddress?.let { addr ->
                if (addr.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = addr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
