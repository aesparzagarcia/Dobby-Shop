package com.ares.ewe_shop.domain.repository

import com.ares.ewe_shop.data.remote.model.ShopOrderDto

interface OrderRepository {

    suspend fun getOrders(status: String? = null): Result<List<ShopOrderDto>>

    suspend fun acceptOrder(orderId: String): Result<Unit>

    suspend fun markOrderPreparing(orderId: String, estimatedPreparationMinutes: Int): Result<Unit>

    suspend fun markOrderReadyForPickup(orderId: String): Result<Unit>

    suspend fun rejectOrder(orderId: String): Result<Unit>
}
