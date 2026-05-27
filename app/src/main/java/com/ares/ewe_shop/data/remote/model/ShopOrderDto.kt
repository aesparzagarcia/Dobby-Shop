package com.ares.ewe_shop.data.remote.model

import com.google.gson.annotations.SerializedName

data class DeliveryManDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String?
)

data class ShopOrderDto(
    @SerializedName("id") val id: String,
    @SerializedName("status") val status: String,
    @SerializedName("total") val total: Double,
    @SerializedName("deliveryAddress") val deliveryAddress: String?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("customerName") val customerName: String?,
    @SerializedName("customerLastName") val customerLastName: String? = null,
    @SerializedName("customerEmail") val customerEmail: String?,
    @SerializedName("estimatedPreparationMinutes") val estimatedPreparationMinutes: Int? = null,
    @SerializedName("items") val items: List<ShopOrderItemDto>,
    @SerializedName("deliveryMan") val deliveryMan: DeliveryManDto?,
    @SerializedName("pickup_code") val pickupCode: String? = null,
    @SerializedName("pickup_handoff_at") val pickupHandoffAt: String? = null,
)

/** Suma de líneas de producto (sin costo de envío). */
fun ShopOrderDto.productsSubtotal(): Double =
    items.sumOf { it.price * it.quantity }
