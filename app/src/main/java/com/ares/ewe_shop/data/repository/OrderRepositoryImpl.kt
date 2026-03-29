package com.ares.ewe_shop.data.repository

import com.ares.ewe_shop.data.remote.api.DobbyShopApi
import com.ares.ewe_shop.data.remote.model.ErrorResponse
import com.ares.ewe_shop.data.remote.model.MarkPreparingRequest
import com.ares.ewe_shop.data.remote.model.ShopOrderDto
import com.ares.ewe_shop.domain.repository.OrderRepository
import retrofit2.HttpException
import javax.inject.Inject

class OrderRepositoryImpl @Inject constructor(
    private val api: DobbyShopApi
) : OrderRepository {

    override suspend fun getOrders(status: String?): Result<List<ShopOrderDto>> {
        return try {
            val list = api.getOrders(status)
            Result.success(list)
        } catch (e: HttpException) {
            val message = parseErrorBody(e) ?: "Error al cargar los pedidos"
            Result.failure(Exception(message))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun acceptOrder(orderId: String): Result<Unit> {
        return try {
            api.acceptOrder(orderId)
            Result.success(Unit)
        } catch (e: HttpException) {
            val message = parseErrorBody(e) ?: "Error al aceptar el pedido"
            Result.failure(Exception(message))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markOrderPreparing(orderId: String, estimatedPreparationMinutes: Int): Result<Unit> {
        return try {
            api.markOrderPreparing(orderId, MarkPreparingRequest(estimatedPreparationMinutes))
            Result.success(Unit)
        } catch (e: HttpException) {
            val message = parseErrorBody(e) ?: "Error al marcar en preparación"
            Result.failure(Exception(message))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markOrderReadyForPickup(orderId: String): Result<Unit> {
        return try {
            api.markOrderReadyForPickup(orderId)
            Result.success(Unit)
        } catch (e: HttpException) {
            val message = parseErrorBody(e) ?: "Error al marcar listo para recoger"
            Result.failure(Exception(message))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rejectOrder(orderId: String): Result<Unit> {
        return try {
            api.rejectOrder(orderId)
            Result.success(Unit)
        } catch (e: HttpException) {
            val message = parseErrorBody(e) ?: "Error al rechazar el pedido"
            Result.failure(Exception(message))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseErrorBody(e: HttpException): String? {
        return try {
            e.response()?.errorBody()?.string()?.let { body ->
                com.google.gson.Gson().fromJson(body, ErrorResponse::class.java)?.error
            }
        } catch (_: Exception) {
            null
        }
    }
}
