package com.ares.ewe_shop.presentation.ui.orders

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Schedule
import com.ares.ewe_shop.core.theme.DobbyShopColors

internal data class StatusFilterChip(
    val value: String?,
    val label: String,
    val dotColor: Color? = null,
)

internal data class OrderStatusVisual(
    val label: String,
    val background: Color,
    val foreground: Color,
    val icon: ImageVector,
)

internal fun orderStatusFilters(isCarWash: Boolean): List<StatusFilterChip> {
    if (!isCarWash) {
        return listOf(
            StatusFilterChip(null, "Todos"),
            StatusFilterChip("PENDING", "Pendientes", DobbyShopColors.Orange),
            StatusFilterChip("CONFIRMED", "Confirmados", DobbyShopColors.Blue),
            StatusFilterChip("PREPARING", "En preparación", DobbyShopColors.Purple),
            StatusFilterChip("READY_FOR_PICKUP", "Listo para recoger", DobbyShopColors.Teal),
            StatusFilterChip("ASSIGNED", "Asignados", DobbyShopColors.PurpleMuted),
            StatusFilterChip("ON_DELIVERY", "En camino", DobbyShopColors.Blue),
            StatusFilterChip("DELIVERED", "Entregados", DobbyShopColors.Green),
            StatusFilterChip("CANCELLED", "Cancelados", DobbyShopColors.Red),
        )
    }
    return listOf(
        StatusFilterChip(null, "Todos"),
        StatusFilterChip("PENDING", "Pendientes", DobbyShopColors.Orange),
        StatusFilterChip("CONFIRMED", "Confirmados", DobbyShopColors.Blue),
        StatusFilterChip("OUT_FOR_PICKUP", "En camino", DobbyShopColors.Blue),
        StatusFilterChip("PICKED_UP", "Recogido", DobbyShopColors.Blue),
        StatusFilterChip("PREPARING", "Lavando", DobbyShopColors.Purple),
        StatusFilterChip("READY_FOR_PICKUP", "Secado y Aspirado", DobbyShopColors.Teal),
        StatusFilterChip("ASSIGNED", "Detallado", DobbyShopColors.PurpleMuted),
        StatusFilterChip("ON_DELIVERY", "En entrega", DobbyShopColors.Blue),
        StatusFilterChip("DELIVERED", "Entregados", DobbyShopColors.Green),
        StatusFilterChip("CANCELLED", "Cancelados", DobbyShopColors.Red),
    )
}

internal fun orderStatusVisual(status: String, isCarWash: Boolean): OrderStatusVisual {
    val label = orderStatusLabel(status, isCarWash)
    return when (status) {
        "PENDING" -> OrderStatusVisual(
            label,
            DobbyShopColors.OrangeLight,
            DobbyShopColors.OrangeDark,
            Icons.Outlined.Schedule,
        )
        "CONFIRMED" -> OrderStatusVisual(
            label,
            DobbyShopColors.BlueLight,
            DobbyShopColors.BlueDark,
            Icons.Default.CheckCircle,
        )
        "OUT_FOR_PICKUP" -> OrderStatusVisual(
            label,
            DobbyShopColors.BlueLight,
            DobbyShopColors.BlueDark,
            Icons.Default.DirectionsCar,
        )
        "PICKED_UP" -> OrderStatusVisual(
            label,
            DobbyShopColors.BlueLight,
            DobbyShopColors.BlueDark,
            Icons.Default.DirectionsCar,
        )
        "PREPARING" -> OrderStatusVisual(
            label,
            DobbyShopColors.PurpleLight,
            DobbyShopColors.Purple,
            if (isCarWash) Icons.Default.DirectionsCar else Icons.Default.Inventory2,
        )
        "READY_FOR_PICKUP" -> OrderStatusVisual(
            label,
            DobbyShopColors.TealLight,
            DobbyShopColors.TealDark,
            if (isCarWash) Icons.Default.Air else Icons.Default.ShoppingBag,
        )
        "ASSIGNED" -> OrderStatusVisual(
            label,
            DobbyShopColors.PurpleLight,
            DobbyShopColors.PurpleDark,
            if (isCarWash) Icons.Default.AutoAwesome else Icons.Outlined.LocalShipping,
        )
        "ON_DELIVERY" -> OrderStatusVisual(
            label,
            DobbyShopColors.BlueLight,
            DobbyShopColors.BlueDark,
            if (isCarWash) Icons.Default.DirectionsCar else Icons.Outlined.LocalShipping,
        )
        "DELIVERED" -> OrderStatusVisual(
            label,
            DobbyShopColors.GreenLight,
            DobbyShopColors.GreenDark,
            Icons.Default.CheckCircle,
        )
        "CANCELLED" -> OrderStatusVisual(
            label,
            DobbyShopColors.RedLight,
            DobbyShopColors.RedDark,
            Icons.Outlined.Cancel,
        )
        else -> OrderStatusVisual(
            label,
            DobbyShopColors.PurpleLight,
            DobbyShopColors.Purple,
            Icons.Default.AccessTime,
        )
    }
}

/** Badge / detail label for a raw order status. */
internal fun orderStatusLabel(status: String, isCarWash: Boolean): String {
    if (!isCarWash) {
        return when (status) {
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
    }
    return when (status) {
        "PENDING" -> "Pendiente"
        "CONFIRMED" -> "Confirmado"
        "OUT_FOR_PICKUP" -> "En camino"
        "PICKED_UP" -> "Recogido"
        "PREPARING" -> "Lavando"
        "READY_FOR_PICKUP" -> "Secado y Aspirado"
        "ASSIGNED" -> "Detallado"
        "ON_DELIVERY" -> "En entrega"
        "DELIVERED" -> "Entregado"
        "CANCELLED" -> "Cancelado"
        else -> status
    }
}
