package com.ares.ewe_shop.data.remote.model

import com.google.gson.annotations.SerializedName

data class VerifyOtpResponse(
    @SerializedName("token") val token: String?,
    @SerializedName("refreshToken") val refreshToken: String?,
    @SerializedName("shop") val shop: VerifyOtpShop?
)

data class VerifyOtpShop(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String,
    @SerializedName("phone") val phone: String?
)
