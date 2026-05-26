package com.ares.ewe_shop.data.repository

import com.ares.ewe_shop.data.remote.api.DobbyShopApi
import com.ares.ewe_shop.data.remote.model.ErrorResponse
import com.ares.ewe_shop.data.remote.model.ShopProfileDto
import com.ares.ewe_shop.domain.repository.ShopProfileRepository
import com.google.gson.Gson
import retrofit2.HttpException
import javax.inject.Inject

class ShopProfileRepositoryImpl @Inject constructor(
    private val api: DobbyShopApi,
) : ShopProfileRepository {

    override suspend fun getProfile(): Result<ShopProfileDto> {
        return try {
            Result.success(api.getShopProfile())
        } catch (e: HttpException) {
            val message = parseErrorBody(e) ?: "Error al cargar el perfil"
            Result.failure(Exception(message))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseErrorBody(e: HttpException): String? {
        return try {
            e.response()?.errorBody()?.string()?.let { body ->
                Gson().fromJson(body, ErrorResponse::class.java)?.error
            }
        } catch (_: Exception) {
            null
        }
    }
}
