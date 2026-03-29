package com.ares.ewe_shop.data.remote.model

import com.google.gson.annotations.SerializedName

data class ShopOrderItemDto(
    @SerializedName("productId") val productId: String,
    @SerializedName("productName") val productName: String?,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("price") val price: Double
)
