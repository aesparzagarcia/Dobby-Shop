package com.ares.ewe_shop.data.remote.model

import com.google.gson.annotations.SerializedName

data class GoogleDirectionsResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("error_message") val errorMessage: String? = null,
    @SerializedName("routes") val routes: List<DirectionsRoute>?
)

data class DirectionsRoute(
    @SerializedName("overview_polyline") val overviewPolyline: DirectionsPolyline?,
    @SerializedName("legs") val legs: List<DirectionsLeg>?
)

data class DirectionsLeg(
    @SerializedName("duration") val duration: DirectionsValueText?,
    @SerializedName("distance") val distance: DirectionsValueText?
)

/** Google usa la misma forma: duration en segundos, distance en metros. */
data class DirectionsValueText(
    @SerializedName("value") val value: Int?,
    @SerializedName("text") val text: String?
)

data class DirectionsPolyline(
    @SerializedName("points") val points: String?
)
