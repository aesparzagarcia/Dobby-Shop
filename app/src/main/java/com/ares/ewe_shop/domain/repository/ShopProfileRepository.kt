package com.ares.ewe_shop.domain.repository

import com.ares.ewe_shop.data.remote.model.ShopEarningsDto
import com.ares.ewe_shop.data.remote.model.ShopProfileDto

interface ShopProfileRepository {
    suspend fun getProfile(): Result<ShopProfileDto>
    suspend fun getEarnings(from: String, to: String): Result<ShopEarningsDto>
    suspend fun updateStatus(status: String): Result<String>
}
