package com.ares.ewe_shop.presentation.ui.orders

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.Alignment
import com.ares.ewe_shop.data.remote.model.ShopOrderDto
import com.ares.ewe_shop.presentation.viewmodel.orders.OrderDetailViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Locale

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

/** Formats duration minutes as used for prep time (e.g. 90 → "1 h 30 min"). */
private fun formatPrepDurationMinutes(totalMinutes: Int): String {
    if (totalMinutes <= 0) return "—"
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return when {
        h == 0 -> "$m min"
        m == 0 -> "${h} h"
        else -> "${h} h ${m} min"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EstimatedPrepTimePickerDialog(
    initialMinutes: Int?,
    onDismiss: () -> Unit,
    onConfirm: (totalMinutes: Int) -> Unit
) {
    val base = initialMinutes ?: 30
    val state = rememberTimePickerState(
        initialHour = (base / 60).coerceIn(0, 23),
        initialMinute = (base % 60).coerceIn(0, 59),
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Duración estimada de preparación") },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(
                onClick = {
                    val total = state.hour * 60 + state.minute
                    onConfirm(total)
                }
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

private fun formatOrderDate(createdAt: String): String {
    return try {
        val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        val date = iso.parse(createdAt) ?: return createdAt
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(date)
    } catch (_: Exception) {
        createdAt
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    onBack: () -> Unit,
    onAcceptOrRejectSuccess: () -> Unit,
    onReadyForPickupSuccess: () -> Unit,
    viewModel: OrderDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showPrepTimePicker by rememberSaveable { mutableStateOf(false) }
    /** Total prep duration in minutes (from TimePicker as hours + minutes of duration). */
    var estimatedPrepMinutes by rememberSaveable { mutableStateOf<Int?>(null) }

    // El gesto / botón atrás del sistema debe usar el mismo onBack que la flecha (refrescar lista en Nav).
    BackHandler(onBack = onBack)

    LaunchedEffect(Unit) {
        viewModel.loadOrder(null)
    }

    LaunchedEffect(uiState.actionSuccess) {
        if (uiState.actionSuccess) {
            snackbarHostState.showSnackbar("Pedido actualizado")
            // Pop is handled by accept/reject callbacks only; preparing just updates state
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del pedido") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading && uiState.order == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        } else {
            val order = uiState.order
            if (order == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(padding)
                        .padding(32.dp)
                ) {
                    Text(
                        text = "Pedido no encontrado",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Total: $${String.format("%.2f", order.total)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            order.customerName?.let { name ->
                                if (name.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Cliente: $name", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            order.customerEmail?.let { email ->
                                if (email.isNotBlank()) {
                                    Text("Email: $email", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            order.deliveryAddress?.let { addr ->
                                if (addr.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Dirección de entrega",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Text(text = addr, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            order.estimatedPreparationMinutes?.let { mins ->
                                if (mins > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Tiempo estimado de preparación: $mins min",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Productos",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    order.items.forEach { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.productName ?: "Producto",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Cantidad: ${item.quantity}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "$${String.format("%.2f", item.price * item.quantity)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    if (order.status == "PENDING") {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.rejectOrder(onAcceptOrRejectSuccess) },
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isAccepting && !uiState.isRejecting
                            ) {
                                if (uiState.isRejecting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.height(20.dp).padding(4.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Rechazar")
                                }
                            }
                            Button(
                                onClick = { viewModel.acceptOrder(onAcceptOrRejectSuccess) },
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isAccepting && !uiState.isRejecting
                            ) {
                                if (uiState.isAccepting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.height(20.dp).padding(4.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text("Aceptar")
                                }
                            }
                        }
                    }

                    if (order.status == "CONFIRMED") {
                        Spacer(modifier = Modifier.height(24.dp))
                        if (showPrepTimePicker) {
                            EstimatedPrepTimePickerDialog(
                                initialMinutes = estimatedPrepMinutes,
                                onDismiss = { showPrepTimePicker = false },
                                onConfirm = { total ->
                                    estimatedPrepMinutes = if (total in 1..1440) total else null
                                    showPrepTimePicker = false
                                }
                            )
                        }
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Antes de pasar a preparación, elige cuánto tardará el pedido (duración).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { showPrepTimePicker = true },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isPreparing
                            ) {
                                Text(
                                    if (estimatedPrepMinutes != null) {
                                        "Tiempo estimado: ${formatPrepDurationMinutes(estimatedPrepMinutes!!)}"
                                    } else {
                                        "Seleccionar tiempo estimado"
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            val minutesValid = estimatedPrepMinutes != null &&
                                estimatedPrepMinutes!! in 1..1440
                            Button(
                                onClick = {
                                    estimatedPrepMinutes?.let { viewModel.markPreparing(it) }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isPreparing && minutesValid
                            ) {
                                if (uiState.isPreparing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.height(20.dp).padding(4.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text("Marcar en preparación")
                                }
                            }
                        }
                    }

                    if (order.status == "PREPARING") {
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.markReadyForPickup(onReadyForPickupSuccess) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isReadyForPickup
                        ) {
                            if (uiState.isReadyForPickup) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(20.dp).padding(4.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Listo para recoger")
                            }
                        }
                    }

                    Log.d("ArmandoLog","Status: ${order.status}")

                    if (order.status in setOf("ASSIGNED", "ON_DELIVERY", "DELIVERED")) {
                        order.deliveryMan?.let { dm ->
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Repartidor: ${dm.name ?: "—"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
