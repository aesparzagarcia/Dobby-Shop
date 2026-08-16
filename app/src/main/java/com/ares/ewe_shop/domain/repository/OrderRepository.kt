package com.ares.ewe_shop.domain.repository

import com.ares.ewe_shop.data.remote.model.ShopOrderDto

interface OrderRepository {

    suspend fun getOrders(status: String? = null): Result<List<ShopOrderDto>>

    suspend fun getOrderById(orderId: String): Result<ShopOrderDto>

    suspend fun acceptOrder(orderId: String): Result<Unit>

    suspend fun markOrderPreparing(orderId: String, estimatedPreparationMinutes: Int): Result<Unit>

    /** Carwash: sale a recoger el carro del cliente. */
    suspend fun markOrderOutForPickup(orderId: String, estimatedPreparationMinutes: Int): Result<Unit>

    /** Carwash: confirma recolección con PIN → PICKED_UP. */
    suspend fun markOrderConfirmPickup(orderId: String, deliveryCode: String): Result<Unit>

    /** Carwash: inicia lavado al llegar al local. */
    suspend fun markOrderStartWash(orderId: String): Result<Unit>

    suspend fun markOrderReadyForPickup(orderId: String): Result<Unit>

    suspend fun markOrderDetailing(orderId: String): Result<Unit>

    suspend fun markOrderOnDelivery(orderId: String): Result<Unit>

    suspend fun rejectOrder(orderId: String): Result<Unit>

    /** Carwash: la tienda llegó con el cliente; el backend genera el PIN de entrega. */
    suspend fun markArrivedAtCustomer(orderId: String): Result<Unit>

    /** Carwash: `true` cuando el PIN de 6 dígitos del cliente es correcto. */
    suspend fun verifyDeliveryCode(orderId: String, deliveryCode: String): Result<Boolean>

    /** Carwash: cierra la entrega con el PIN del cliente. */
    suspend fun markOrderDelivered(orderId: String, deliveryCode: String): Result<Unit>

    /** Carwash: publica el ETA en minutos mientras va en ruta. */
    suspend fun updateDeliveryEta(orderId: String, estimatedDeliveryMinutes: Int): Result<Unit>

    /** Carwash: publica la GPS del vehículo para el mapa del cliente. */
    suspend fun updateCourierLocation(orderId: String, lat: Double, lng: Double): Result<Unit>

    /** Carwash: calificación opcional del cliente tras entregar. */
    suspend fun rateCustomer(
        orderId: String,
        stars: Int?,
        punctual: Boolean?,
        paysWell: Boolean?,
        tipped: Boolean?,
        recommended: Boolean?,
    ): Result<Unit>
}
