package com.ares.ewe_shop.domain.repository

import com.ares.ewe_shop.data.remote.model.ShopProfileDto

interface ShopProfileRepository {
    suspend fun getProfile(): Result<ShopProfileDto>
}
