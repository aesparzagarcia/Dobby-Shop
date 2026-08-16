package com.ares.ewe_shop.data.repository

import com.ares.ewe_shop.data.remote.api.DobbyShopApi
import com.ares.ewe_shop.data.remote.model.ErrorResponse
import com.ares.ewe_shop.data.remote.model.MarkDeliveredRequest
import com.ares.ewe_shop.data.remote.model.MarkPreparingRequest
import com.ares.ewe_shop.data.remote.model.ShopOrderDto
import com.ares.ewe_shop.data.remote.model.RateCustomerRequest
import com.ares.ewe_shop.data.remote.model.UpdateCourierLocationRequest
import com.ares.ewe_shop.data.remote.model.UpdateDeliveryEtaRequest
import com.ares.ewe_shop.data.remote.model.VerifyDeliveryCodeRequest
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

    override suspend fun getOrderById(orderId: String): Result<ShopOrderDto> {
        return getOrders(null).mapCatching { list ->
            list.firstOrNull { it.id == orderId }
                ?: throw Exception("Pedido no encontrado")
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

    override suspend fun markOrderOutForPickup(orderId: String, estimatedPreparationMinutes: Int): Result<Unit> {
        return try {
            api.markOrderOutForPickup(orderId, MarkPreparingRequest(estimatedPreparationMinutes))
            Result.success(Unit)
        } catch (e: HttpException) {
            val message = parseErrorBody(e) ?: "Error al salir a recoger el carro"
            Result.failure(Exception(message))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markOrderConfirmPickup(orderId: String, deliveryCode: String): Result<Unit> {
        return try {
            api.markOrderConfirmPickup(orderId, MarkDeliveredRequest(deliveryCode))
            Result.success(Unit)
        } catch (e: HttpException) {
            val message = parseErrorBody(e) ?: "Error al confirmar la recolección"
            Result.failure(Exception(message))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markOrderStartWash(orderId: String): Result<Unit> {
        return try {
            api.markOrderStartWash(orderId)
            Result.success(Unit)
        } catch (e: HttpException) {
            val message = parseErrorBody(e) ?: "Error al iniciar el lavado"
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

    override suspend fun markOrderDetailing(orderId: String): Result<Unit> {
        return try {
            api.markOrderDetailing(orderId)
            Result.success(Unit)
        } catch (e: HttpException) {
            val message = parseErrorBody(e) ?: "Error al marcar Detallado"
            Result.failure(Exception(message))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markOrderOnDelivery(orderId: String): Result<Unit> {
        return try {
            api.markOrderOnDelivery(orderId)
            Result.success(Unit)
        } catch (e: HttpException) {
            val message = parseErrorBody(e) ?: "Error al marcar En camino"
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

    override suspend fun markArrivedAtCustomer(orderId: String): Result<Unit> {
        return try {
            api.markArrivedAtCustomer(orderId)
            Result.success(Unit)
        } catch (e: HttpException) {
            val message = parseErrorBody(e) ?: "Error al registrar la llegada"
            Result.failure(Exception(message))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verifyDeliveryCode(orderId: String, deliveryCode: String): Result<Boolean> {
        return try {
            val response = api.verifyDeliveryCode(orderId, VerifyDeliveryCodeRequest(deliveryCode))
            Result.success(response.valid == true)
        } catch (e: HttpException) {
            // El backend responde 400 con { valid: false } cuando el PIN no coincide.
            if (e.code() == 400) {
                Result.success(false)
            } else {
                Result.failure(Exception(parseErrorBody(e) ?: "Error al verificar el código"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markOrderDelivered(orderId: String, deliveryCode: String): Result<Unit> {
        return try {
            api.markOrderDelivered(orderId, MarkDeliveredRequest(deliveryCode))
            Result.success(Unit)
        } catch (e: HttpException) {
            val message = parseErrorBody(e) ?: "Error al marcar como entregado"
            Result.failure(Exception(message))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateDeliveryEta(orderId: String, estimatedDeliveryMinutes: Int): Result<Unit> {
        return try {
            api.updateDeliveryEta(orderId, UpdateDeliveryEtaRequest(estimatedDeliveryMinutes))
            Result.success(Unit)
        } catch (e: HttpException) {
            val message = parseErrorBody(e) ?: "Error al actualizar el tiempo de entrega"
            Result.failure(Exception(message))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateCourierLocation(orderId: String, lat: Double, lng: Double): Result<Unit> {
        return try {
            api.updateCourierLocation(orderId, UpdateCourierLocationRequest(lat = lat, lng = lng))
            Result.success(Unit)
        } catch (e: HttpException) {
            val message = parseErrorBody(e) ?: "Error al actualizar la ubicación"
            Result.failure(Exception(message))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rateCustomer(
        orderId: String,
        stars: Int?,
        punctual: Boolean?,
        paysWell: Boolean?,
        tipped: Boolean?,
        recommended: Boolean?,
    ): Result<Unit> {
        return try {
            api.rateCustomer(
                orderId,
                RateCustomerRequest(
                    stars = stars,
                    punctual = punctual,
                    paysWell = paysWell,
                    tipped = tipped,
                    recommended = recommended,
                ),
            )
            Result.success(Unit)
        } catch (e: HttpException) {
            val message = parseErrorBody(e) ?: "Error al guardar la calificación"
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
