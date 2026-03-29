package com.ares.ewe_shop.data.remote.model

import com.google.gson.annotations.SerializedName

data class MarkPreparingRequest(
    @SerializedName("estimatedPreparationMinutes") val estimatedPreparationMinutes: Int
)
