package com.ares.ewe_shop.data.remote.model

import com.google.gson.annotations.SerializedName

data class ShopProductDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("price") val price: Double,
    @SerializedName("imageUrls") val imageUrls: List<String> = emptyList(),
    @SerializedName("hasPromotion") val hasPromotion: Boolean = false,
    @SerializedName("discount") val discount: Int = 0,
    @SerializedName("rate") val rate: Float = 0f,
    @SerializedName("isActive") val isActive: Boolean = true,
    @SerializedName("category") val category: String = "miscelaneos",
    @SerializedName("shop") val shop: ShopNameRef? = null
)

data class ShopNameRef(
    @SerializedName("name") val name: String?
)

data class CreateShopProductRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("price") val price: Double,
    @SerializedName("imageUrls") val imageUrls: List<String>,
    @SerializedName("hasPromotion") val hasPromotion: Boolean,
    @SerializedName("discount") val discount: Int,
    @SerializedName("isActive") val isActive: Boolean,
    @SerializedName("category") val category: String,
)

data class UploadImageResponse(
    @SerializedName("url") val url: String
)
