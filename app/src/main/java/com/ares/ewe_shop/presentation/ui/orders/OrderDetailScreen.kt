package com.ares.ewe_shop.presentation.ui.orders

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ares.ewe_shop.core.theme.DobbyShopColors
import com.ares.ewe_shop.presentation.ui.main.LocalMainBottomBarPadding
import com.ares.ewe_shop.core.util.OrderDateFormat
import com.ares.ewe_shop.core.util.absoluteUploadUrl
import com.ares.ewe_shop.data.remote.model.ShopOrderDto
import com.ares.ewe_shop.data.remote.model.ShopOrderItemDto
import com.ares.ewe_shop.data.remote.model.productsSubtotal
import com.ares.ewe_shop.presentation.viewmodel.orders.OrderDetailViewModel
import java.util.Locale

private data class DetailStatusVisual(
    val label: String,
    val background: Color,
    val foreground: Color,
    val icon: ImageVector,
)

private fun detailStatusVisual(status: String, isCarWash: Boolean): DetailStatusVisual {
    val visual = orderStatusVisual(status, isCarWash)
    return DetailStatusVisual(
        label = visual.label,
        background = visual.background,
        foreground = visual.foreground,
        icon = visual.icon,
    )
}

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

private fun formatDetailOrderNumber(orderId: String): String {
    val suffix = orderId.takeLast(4).uppercase(Locale.getDefault())
    return "#$suffix"
}

private fun formatCustomerLabel(order: ShopOrderDto): String? {
    val parts = listOfNotNull(
        order.customerName?.trim()?.takeIf { it.isNotBlank() },
        order.customerLastName?.trim()?.takeIf { it.isNotBlank() },
    )
    if (parts.isEmpty()) return null
    return "Cliente: ${parts.joinToString(" ")}"
}

private fun formatDetailOrderDate(createdAt: String): String = OrderDateFormat.formatDetail(createdAt)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EstimatedPrepTimePickerDialog(
    initialMinutes: Int?,
    isCarWash: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (totalMinutes: Int) -> Unit,
) {
    val base = initialMinutes ?: 30
    val state = rememberTimePickerState(
        initialHour = (base / 60).coerceIn(0, 23),
        initialMinute = (base % 60).coerceIn(0, 59),
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isCarWash) "Tiempo estimado para recoger el carro"
                else "Duración estimada de preparación",
            )
        },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(
                onClick = {
                    val total = state.hour * 60 + state.minute
                    onConfirm(total)
                },
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    onBack: () -> Unit,
    onAcceptSuccess: () -> Unit,
    onRejectSuccess: () -> Unit,
    onMarkPreparingSuccess: () -> Unit,
    onReadyForPickupSuccess: () -> Unit,
    onDetailingSuccess: () -> Unit,
    onOnDeliverySuccess: () -> Unit,
    onOpenDeliveryMap: () -> Unit,
    onOpenPickupMap: () -> Unit = onOpenDeliveryMap,
    onOpenCustomerRoutePreview: () -> Unit = onOpenDeliveryMap,
    viewModel: OrderDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val bottomBarPadding = LocalMainBottomBarPadding.current
    var showPrepTimePicker by rememberSaveable { mutableStateOf(false) }
    var estimatedPrepMinutes by rememberSaveable { mutableStateOf<Int?>(null) }
    var showPrepInstructionNotice by rememberSaveable { mutableStateOf(true) }
    var deliveryMapOpened by rememberSaveable { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    LaunchedEffect(Unit) {
        viewModel.loadOrder(null)
    }

    // Carwash en ruta (recolección o entrega): abrir mapa.
    LaunchedEffect(uiState.isCarWash, uiState.order?.status) {
        val status = uiState.order?.status
        if (uiState.isCarWash && status == "OUT_FOR_PICKUP" && !deliveryMapOpened) {
            deliveryMapOpened = true
            onOpenPickupMap()
        } else if (uiState.isCarWash && status == "PICKED_UP" && !deliveryMapOpened) {
            deliveryMapOpened = true
            onOpenPickupMap()
        } else if (uiState.isCarWash && status == "ON_DELIVERY" && !deliveryMapOpened) {
            deliveryMapOpened = true
            onOpenDeliveryMap()
        }
    }

    LaunchedEffect(uiState.actionSuccess) {
        if (uiState.actionSuccess) {
            snackbarHostState.showSnackbar("Pedido actualizado")
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

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
                .padding(padding),
        ) {
            OrderDetailTopBar(onBack = onBack)

            when {
                uiState.isLoading && uiState.order == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = DobbyShopColors.Purple)
                    }
                }
                uiState.order == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Pedido no encontrado",
                            style = MaterialTheme.typography.bodyLarge,
                            color = DobbyShopColors.TextSecondary,
                        )
                    }
                }
                else -> {
                    val order = uiState.order!!
                    val hasBottomActions = order.status in setOf("PENDING", "CONFIRMED", "PREPARING") ||
                        (uiState.isCarWash && order.status in setOf(
                            "OUT_FOR_PICKUP",
                            "PICKED_UP",
                            "READY_FOR_PICKUP",
                            "ASSIGNED",
                            "ON_DELIVERY",
                        ))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp)
                                .padding(bottom = if (hasBottomActions) 8.dp else 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            OrderInfoCard(
                                order = order,
                                isCarWash = uiState.isCarWash,
                                onOpenCustomerRoute = if (
                                    uiState.isCarWash && order.lat != null && order.lng != null
                                ) {
                                    onOpenCustomerRoutePreview
                                } else {
                                    null
                                },
                            )
                            ProductsSection(items = order.items)
                            OrderTotalCard(total = order.productsSubtotal())
                            if (order.status in setOf("ASSIGNED", "ON_DELIVERY", "DELIVERED")) {
                                order.deliveryMan?.let { dm ->
                                    DeliveryDriverCard(name = dm.name)
                                }
                            }
                            when (order.status) {
                                "ASSIGNED" -> {
                                    if (!order.pickupCode.isNullOrBlank()) {
                                        PickupHandoffCodeCard(
                                            orderId = order.id,
                                            pickupCode = order.pickupCode,
                                            customerLabel = formatCustomerLabel(order),
                                            courierName = order.deliveryMan?.name,
                                            handedOff = false,
                                        )
                                    }
                                }
                                "ON_DELIVERY" -> {
                                    // En carwash la tienda es quien entrega: no hay repartidor externo.
                                    if (!uiState.isCarWash) {
                                        PickupHandoffCodeCard(
                                            orderId = order.id,
                                            pickupCode = null,
                                            customerLabel = formatCustomerLabel(order),
                                            courierName = order.deliveryMan?.name,
                                            handedOff = true,
                                        )
                                    }
                                }
                            }
                            order.estimatedPreparationMinutes?.takeIf { it > 0 }?.let { mins ->
                                EstimatedPrepInfoCard(
                                    minutes = mins,
                                    isPickupEta = uiState.isCarWash &&
                                        order.status in setOf("CONFIRMED", "OUT_FOR_PICKUP"),
                                )
                            }
                        }
                    }

                    if (hasBottomActions) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (order.status == "PENDING") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.rejectOrder(onRejectSuccess) },
                                    modifier = Modifier.weight(1f),
                                    enabled = !uiState.isAccepting && !uiState.isRejecting,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = DobbyShopColors.RedDark,
                                    ),
                                ) {
                                    if (uiState.isRejecting) {
                                        ActionLoadingIndicator()
                                    } else {
                                        Text("Rechazar", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                Button(
                                    onClick = { viewModel.acceptOrder(onAcceptSuccess) },
                                    modifier = Modifier.weight(1f),
                                    enabled = !uiState.isAccepting && !uiState.isRejecting,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DobbyShopColors.Purple,
                                        contentColor = Color.White,
                                    ),
                                ) {
                                    if (uiState.isAccepting) {
                                        ActionLoadingIndicator(color = Color.White)
                                    } else {
                                        Text("Aceptar", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        if (order.status == "CONFIRMED") {
                            if (!uiState.isCarWash) {
                                LaunchedEffect(order.id) {
                                    showPrepInstructionNotice = true
                                    delay(6_000)
                                    showPrepInstructionNotice = false
                                }
                            }
                            if (showPrepTimePicker) {
                                EstimatedPrepTimePickerDialog(
                                    initialMinutes = estimatedPrepMinutes,
                                    isCarWash = uiState.isCarWash,
                                    onDismiss = { showPrepTimePicker = false },
                                    onConfirm = { total ->
                                        estimatedPrepMinutes = if (total in 1..1440) total else null
                                        showPrepTimePicker = false
                                    },
                                )
                            }
                            if (!uiState.isCarWash) {
                                AnimatedVisibility(
                                    visible = showPrepInstructionNotice,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically(),
                                ) {
                                    PrepInstructionNotice(
                                        onDismiss = { showPrepInstructionNotice = false },
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { showPrepTimePicker = true },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isPreparing,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = DobbyShopColors.Purple,
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    DobbyShopColors.Purple.copy(alpha = 0.5f),
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (estimatedPrepMinutes != null) {
                                        if (uiState.isCarWash) {
                                            "Tiempo estimado para recoger carro: ${formatPrepDurationMinutes(estimatedPrepMinutes!!)}"
                                        } else {
                                            "Tiempo estimado: ${formatPrepDurationMinutes(estimatedPrepMinutes!!)}"
                                        }
                                    } else {
                                        if (uiState.isCarWash) {
                                            "Seleccionar tiempo estimado para recoger carro"
                                        } else {
                                            "Seleccionar tiempo estimado"
                                        }
                                    },
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            val minutesValid = estimatedPrepMinutes != null &&
                                estimatedPrepMinutes!! in 1..1440
                            Button(
                                onClick = {
                                    estimatedPrepMinutes?.let { mins ->
                                        if (uiState.isCarWash) {
                                            viewModel.markOutForPickup(mins) {
                                                onOpenPickupMap()
                                            }
                                        } else {
                                            viewModel.markPreparing(mins, onMarkPreparingSuccess)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isPreparing && minutesValid,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DobbyShopColors.Purple,
                                    contentColor = Color.White,
                                ),
                            ) {
                                if (uiState.isPreparing) {
                                    ActionLoadingIndicator(color = Color.White)
                                } else {
                                    Icon(
                                        imageVector = if (uiState.isCarWash) {
                                            Icons.Default.DirectionsCar
                                        } else {
                                            Icons.Default.Restaurant
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (uiState.isCarWash) {
                                            "Ir a recoger el carro"
                                        } else {
                                            "Marcar en preparación"
                                        },
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }

                        if (order.status in setOf("OUT_FOR_PICKUP", "PICKED_UP") && uiState.isCarWash) {
                            Button(
                                onClick = onOpenPickupMap,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DobbyShopColors.Purple,
                                    contentColor = Color.White,
                                ),
                            ) {
                                Text(
                                    text = if (order.status == "PICKED_UP") {
                                        "Abrir mapa al autolavado"
                                    } else {
                                        "Abrir mapa de recolección"
                                    },
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }

                        if (order.status == "PREPARING") {
                            Button(
                                onClick = { viewModel.markReadyForPickup(onReadyForPickupSuccess) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isReadyForPickup,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DobbyShopColors.Purple,
                                    contentColor = Color.White,
                                ),
                            ) {
                                if (uiState.isReadyForPickup) {
                                    ActionLoadingIndicator(color = Color.White)
                                } else {
                                    Text(
                                        text = if (uiState.isCarWash) {
                                            "Marcar Secado y Aspirado"
                                        } else {
                                            "Listo para recoger"
                                        },
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }

                        if (order.status == "READY_FOR_PICKUP" && uiState.isCarWash) {
                            Button(
                                onClick = { viewModel.markDetailing(onDetailingSuccess) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isDetailing,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DobbyShopColors.Purple,
                                    contentColor = Color.White,
                                ),
                            ) {
                                if (uiState.isDetailing) {
                                    ActionLoadingIndicator(color = Color.White)
                                } else {
                                    Text(
                                        text = "Pasar a Detallado",
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }

                        if (order.status == "ON_DELIVERY" && uiState.isCarWash) {
                            Button(
                                onClick = {
                                    deliveryMapOpened = true
                                    onOpenDeliveryMap()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DobbyShopColors.Purple,
                                    contentColor = Color.White,
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Abrir mapa de entrega",
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }

                        if (order.status == "ASSIGNED" && uiState.isCarWash) {
                            Button(
                                onClick = {
                                    deliveryMapOpened = true
                                    viewModel.markOnDelivery(onOnDeliverySuccess)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isMarkingOnDelivery,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DobbyShopColors.Purple,
                                    contentColor = Color.White,
                                ),
                            ) {
                                if (uiState.isMarkingOnDelivery) {
                                    ActionLoadingIndicator(color = Color.White)
                                } else {
                                    Text(
                                        text = "Pasar a entregar",
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderDetailTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            onClick = onBack,
            shape = RoundedCornerShape(14.dp),
            color = DobbyShopColors.PurpleLight,
            modifier = Modifier.size(44.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = DobbyShopColors.Purple,
                )
            }
        }
        Text(
            text = "Detalle del pedido",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = DobbyShopColors.TextPrimary,
        )
    }
}

@Composable
private fun OrderInfoCard(
    order: ShopOrderDto,
    isCarWash: Boolean,
    onOpenCustomerRoute: (() -> Unit)? = null,
) {
    val visual = detailStatusVisual(order.status, isCarWash)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DobbyShopColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                    DetailIconBox(icon = Icons.Default.Event)
                    Text(
                        text = formatDetailOrderDate(order.createdAt),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = DobbyShopColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                DetailStatusBadge(visual = visual)
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 14.dp),
                color = DobbyShopColors.Border,
            )

            formatCustomerLabel(order)?.let { label ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DetailIconBox(icon = Icons.Default.Person)
                    Column {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = DobbyShopColors.TextPrimary,
                        )
                        order.customerEmail?.takeIf { it.isNotBlank() }?.let { email ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = DobbyShopColors.TextSecondary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = email,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DobbyShopColors.TextSecondary,
                                )
                            }
                        }
                    }
                }
            }

            order.deliveryAddress?.takeIf { it.isNotBlank() }?.let { addr ->
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 14.dp),
                    color = DobbyShopColors.Border,
                )
                val routeClickable = onOpenCustomerRoute != null
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (routeClickable) {
                                Modifier.clickable(onClick = onOpenCustomerRoute!!)
                            } else {
                                Modifier
                            },
                        ),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DetailIconBox(icon = Icons.Default.LocationOn)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isCarWash) "Dirección del cliente" else "Dirección de entrega",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = DobbyShopColors.TextPrimary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = addr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DobbyShopColors.TextSecondary,
                        )
                        if (routeClickable) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Ver ruta y tiempo estimado",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = DobbyShopColors.Purple,
                            )
                        }
                    }
                    if (routeClickable) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Ver ruta",
                            tint = DobbyShopColors.Purple,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductsSection(items: List<ShopOrderItemDto>) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DetailIconBox(icon = Icons.Default.ShoppingBag)
            Text(
                text = "Productos",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DobbyShopColors.TextPrimary,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items.forEach { item ->
                ProductLineCard(item = item)
            }
        }
    }
}

@Composable
private fun ProductLineCard(item: ShopOrderItemDto) {
    val name = item.productName ?: "Producto"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DobbyShopColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DobbyShopColors.PurpleLight),
                contentAlignment = Alignment.Center,
            ) {
                val imageUrl = item.imageUrl?.takeIf { it.isNotBlank() }
                if (imageUrl != null) {
                    AsyncImage(
                        model = absoluteUploadUrl(imageUrl),
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = name.take(1).uppercase(Locale.getDefault()),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = DobbyShopColors.Purple,
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = DobbyShopColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        append("Cantidad: ${item.quantity}")
                        append(" · $")
                        append(String.format(Locale.getDefault(), "%.2f", item.price))
                        append(" c/u")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = DobbyShopColors.TextSecondary,
                )
            }
            Text(
                text = "$${String.format(Locale.getDefault(), "%.2f", item.price * item.quantity)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = DobbyShopColors.TextPrimary,
            )
        }
    }
}

@Composable
private fun OrderTotalCard(total: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DobbyShopColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DetailIconBox(icon = Icons.Default.Receipt)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Total productos",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = DobbyShopColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$${String.format(Locale.getDefault(), "%.2f", total)}",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = DobbyShopColors.Purple,
            )
        }
    }
}

@Composable
private fun PrepInstructionNotice(onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = DobbyShopColors.PurpleLight,
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = DobbyShopColors.Purple,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Antes de pasar a preparación, elige cuánto tardará el pedido (duración).",
                style = MaterialTheme.typography.bodySmall,
                color = DobbyShopColors.PurpleDark,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar aviso",
                    tint = DobbyShopColors.Purple,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun EstimatedPrepInfoCard(minutes: Int, isPickupEta: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DobbyShopColors.PurpleLight),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                tint = DobbyShopColors.Purple,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = if (isPickupEta) {
                    "Tiempo estimado para recoger carro: ${formatPrepDurationMinutes(minutes)}"
                } else {
                    "Tiempo estimado: ${formatPrepDurationMinutes(minutes)}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = DobbyShopColors.PurpleDark,
            )
        }
    }
}

@Composable
private fun PickupHandoffCodeCard(
    orderId: String,
    pickupCode: String?,
    customerLabel: String?,
    courierName: String?,
    handedOff: Boolean,
) {
    val containerColor = if (handedOff) DobbyShopColors.GreenLight else DobbyShopColors.TealLight
    val accent = if (handedOff) DobbyShopColors.GreenDark else DobbyShopColors.TealDark
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = if (handedOff) "Pedido entregado al repartidor" else "Código para el repartidor",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
            if (!handedOff && pickupCode != null) {
                Text(
                    text = pickupCode,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 8.sp,
                    color = DobbyShopColors.TextPrimary,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
            Text(
                text = "Pedido ${formatDetailOrderNumber(orderId)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = DobbyShopColors.TextPrimary,
            )
            customerLabel?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = DobbyShopColors.TextSecondary,
                )
            }
            courierName?.let { name ->
                Text(
                    text = "Repartidor: $name",
                    style = MaterialTheme.typography.bodySmall,
                    color = DobbyShopColors.TextSecondary,
                )
            }
            Text(
                text = if (handedOff) {
                    "El repartidor confirmó la recogida con el código."
                } else {
                    "Entrega el pedido solo cuando el repartidor ingrese este código en su app."
                },
                style = MaterialTheme.typography.bodySmall,
                color = DobbyShopColors.TextSecondary,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun DeliveryDriverCard(name: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DobbyShopColors.Surface),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DetailIconBox(icon = Icons.Outlined.LocalShipping)
            Text(
                text = "Repartidor: ${name ?: "—"}",
                style = MaterialTheme.typography.bodyMedium,
                color = DobbyShopColors.TextPrimary,
            )
        }
    }
}

@Composable
private fun DetailIconBox(
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DobbyShopColors.PurpleLight),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DobbyShopColors.Purple,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun DetailStatusBadge(
    visual: DetailStatusVisual,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(visual.background)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = visual.icon,
            contentDescription = null,
            tint = visual.foreground,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = visual.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = visual.foreground,
            maxLines = 2,
            softWrap = true,
        )
    }
}

@Composable
private fun ActionLoadingIndicator(color: Color = DobbyShopColors.Purple) {
    CircularProgressIndicator(
        modifier = Modifier.size(20.dp),
        strokeWidth = 2.dp,
        color = color,
    )
}
