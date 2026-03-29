package com.ares.ewe_shop.data.remote.model

import com.google.gson.annotations.SerializedName

data class AcceptRejectResponse(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("status") val status: String
)
