package com.ares.ewe_shop.data.remote.model

import com.google.gson.annotations.SerializedName

data class ShopProductVariantDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String = "",
    @SerializedName("price") val price: Double = 0.0,
)

data class ShopProductDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("price") val price: Double,
    @SerializedName("variants") val variants: List<ShopProductVariantDto>? = emptyList(),
    @SerializedName("imageUrls") val imageUrls: List<String> = emptyList(),
    @SerializedName("hasPromotion") val hasPromotion: Boolean = false,
    @SerializedName("discount") val discount: Int = 0,
    @SerializedName("rate") val rate: Float = 0f,
    @SerializedName("isActive") val isActive: Boolean = true,
    @SerializedName("category") val category: String = "miscelaneos",
    @SerializedName("shop") val shop: ShopNameRef? = null,
    @SerializedName("moderationStatus") val moderationStatus: String? = null,
    @SerializedName("canActivate") val canActivate: Boolean = true,
    @SerializedName("quantitySold") val quantitySold: Int = 0,
) {
    val resolvedVariants: List<ShopProductVariantDto>
        get() = variants.orEmpty()

    val hasMultipleVariants: Boolean
        get() = resolvedVariants.size > 1

    val listPrice: Double
        get() = if (hasMultipleVariants) resolvedVariants.minOf { it.price } else price

    val isPendingModeration: Boolean
        get() {
            val status = moderationStatus?.uppercase()
            return status == "PENDING" || status == "REVIEWED"
        }

    val wasRejected: Boolean
        get() = moderationStatus?.uppercase() == "DISMISSED"

    val availabilityBadge: String?
        get() = when {
            isPendingModeration -> "En revisión"
            wasRejected -> "Rechazado"
            !isActive -> "Pausado"
            else -> null
        }
}

data class ShopNameRef(
    @SerializedName("name") val name: String?
)

data class ProductVariantInput(
    @SerializedName("name") val name: String,
    @SerializedName("price") val price: Double,
)

data class CreateShopProductRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("price") val price: Double,
    @SerializedName("variants") val variants: List<ProductVariantInput> = emptyList(),
    @SerializedName("imageUrls") val imageUrls: List<String>,
    @SerializedName("hasPromotion") val hasPromotion: Boolean,
    @SerializedName("discount") val discount: Int,
    @SerializedName("isActive") val isActive: Boolean,
    @SerializedName("category") val category: String,
)

data class UploadImageResponse(
    @SerializedName("url") val url: String
)
