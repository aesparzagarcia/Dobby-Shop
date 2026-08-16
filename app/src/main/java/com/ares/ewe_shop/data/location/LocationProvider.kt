package com.ares.ewe_shop.data.location

import com.google.android.gms.maps.model.LatLng

interface LocationProvider {
    suspend fun getCurrentLocation(): Result<LatLng>
}
