package com.ares.ewe_shop.data.remote.model

import com.google.gson.annotations.SerializedName

data class ShopRefreshRequest(
    @SerializedName("refreshToken") val refreshToken: String
)

data class ShopRefreshResponse(
    @SerializedName("token") val token: String?,
    @SerializedName("refreshToken") val refreshToken: String?
)
