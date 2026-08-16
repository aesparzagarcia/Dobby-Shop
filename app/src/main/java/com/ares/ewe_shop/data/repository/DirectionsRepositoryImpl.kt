package com.ares.ewe_shop.data.repository

import android.util.Log
import com.ares.ewe_shop.BuildConfig
import com.ares.ewe_shop.data.remote.api.GoogleDirectionsApi
import com.ares.ewe_shop.data.remote.model.GoogleDirectionsResponse
import com.ares.ewe_shop.data.util.PolylineDecoder
import com.ares.ewe_shop.domain.repository.DirectionsRepository
import com.ares.ewe_shop.domain.repository.DrivingRouteInfo
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import javax.inject.Inject

private const val TAG = "DirectionsRepo"

class DirectionsRepositoryImpl @Inject constructor(
    private val api: GoogleDirectionsApi
) : DirectionsRepository {

    override suspend fun getDrivingRoute(origin: LatLng, destination: LatLng): Result<DrivingRouteInfo> {
        return try {
            val key = BuildConfig.DIRECTIONS_API_KEY
            if (key.isBlank()) {
                Log.e(TAG, "DIRECTIONS_API_KEY is empty")
                return Result.failure(
                    IllegalStateException("DIRECTIONS_API_KEY no está configurada en gradle.properties.")
                )
            }
            val response = api.getDirections(
                origin = "${origin.latitude},${origin.longitude}",
                destination = "${destination.latitude},${destination.longitude}",
                key = key
            )
            val body = response.body()
            if (!response.isSuccessful || body == null) {
                val errorBody = response.errorBody()?.string()
                val parsed = errorBody?.let { Gson().fromJson(it, GoogleDirectionsResponse::class.java) }
                val status = parsed?.status ?: "HTTP ${response.code()}"
                val message = parsed?.errorMessage ?: errorBody ?: status
                Log.e(TAG, "Directions API failed: status=$status, message=$message")
                return Result.failure(IllegalStateException("Directions API: $status. $message"))
            }
            if (body.status != "OK") {
                val message = body.errorMessage ?: "Directions API: ${body.status}"
                Log.e(TAG, "Directions API status not OK: ${body.status}, $message")
                return Result.failure(IllegalStateException(message))
            }
            val route = body.routes?.firstOrNull()
            val encoded = route?.overviewPolyline?.points
            val points = if (!encoded.isNullOrBlank()) PolylineDecoder.decode(encoded) else emptyList()
            val leg = route?.legs?.firstOrNull()
            Result.success(
                DrivingRouteInfo(
                    points = points,
                    durationSeconds = leg?.duration?.value,
                    durationText = leg?.duration?.text,
                    distanceMeters = leg?.distance?.value,
                    distanceText = leg?.distance?.text
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Directions request failed", e)
            Result.failure(e)
        }
    }
}
