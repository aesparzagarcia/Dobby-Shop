package com.ares.ewe_shop.data.location

import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FusedLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationProvider {

    private val fusedClient by lazy { LocationServices.getFusedLocationProviderClient(context) }

    override suspend fun getCurrentLocation(): Result<LatLng> = runCatching {
        val cancellationToken = CancellationTokenSource()
        val location = fusedClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationToken.token
        ).await()
            ?: fusedClient.lastLocation.await()
            ?: throw IllegalStateException("Ubicación no disponible. Activa GPS o ubicación por red.")
        LatLng(location.latitude, location.longitude)
    }
}
