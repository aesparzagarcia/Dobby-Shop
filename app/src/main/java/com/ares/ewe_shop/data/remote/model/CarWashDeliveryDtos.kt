package com.ares.ewe_shop.data.remote.model

import com.google.gson.annotations.SerializedName

data class VerifyDeliveryCodeRequest(
    @SerializedName("delivery_code") val deliveryCode: String,
)

data class MarkDeliveredRequest(
    @SerializedName("delivery_code") val deliveryCode: String,
)

data class UpdateDeliveryEtaRequest(
    @SerializedName("estimatedDeliveryMinutes") val estimatedDeliveryMinutes: Int,
)

data class UpdateCourierLocationRequest(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
)

data class RateCustomerRequest(
    @SerializedName("stars") val stars: Int? = null,
    @SerializedName("punctual") val punctual: Boolean? = null,
    @SerializedName("pays_well") val paysWell: Boolean? = null,
    @SerializedName("tipped") val tipped: Boolean? = null,
    @SerializedName("recommended") val recommended: Boolean? = null,
)

data class MarkArrivedResponse(
    @SerializedName("ok") val ok: Boolean? = null,
    @SerializedName("arrived_at_customer_at") val arrivedAtCustomerAt: String? = null,
    @SerializedName("delivery_code_required") val deliveryCodeRequired: Boolean? = null,
)

data class VerifyDeliveryCodeResponse(
    @SerializedName("valid") val valid: Boolean? = null,
)

data class UpdateDeliveryEtaResponse(
    @SerializedName("ok") val ok: Boolean? = null,
    @SerializedName("estimatedDeliveryMinutes") val estimatedDeliveryMinutes: Int? = null,
)
