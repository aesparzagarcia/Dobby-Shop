package com.ares.ewe_shop.data.remote.model

import com.google.gson.annotations.SerializedName

data class ShopRequestOtpResponse(
    @SerializedName("sent") val sent: Boolean
)
