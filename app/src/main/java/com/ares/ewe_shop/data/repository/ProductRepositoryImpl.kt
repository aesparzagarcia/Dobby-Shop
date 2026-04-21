package com.ares.ewe_shop.data.repository

import android.content.Context
import android.net.Uri
import com.ares.ewe_shop.data.remote.api.DobbyShopApi
import com.ares.ewe_shop.data.remote.model.CreateShopProductRequest
import com.ares.ewe_shop.data.remote.model.ErrorResponse
import com.ares.ewe_shop.data.remote.model.ShopProductDto
import com.ares.ewe_shop.domain.repository.ProductRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

class ProductRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: DobbyShopApi
) : ProductRepository {

    override suspend fun getShopProducts(): Result<List<ShopProductDto>> {
        return try {
            Result.success(api.getShopProducts())
        } catch (e: HttpException) {
            Result.failure(Exception(parseErrorBody(e) ?: "Error al cargar los productos"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createProduct(body: CreateShopProductRequest): Result<ShopProductDto> {
        return try {
            Result.success(api.createShopProduct(body))
        } catch (e: HttpException) {
            Result.failure(Exception(parseErrorBody(e) ?: "Error al crear el producto"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProduct(
        productId: String,
        body: CreateShopProductRequest
    ): Result<ShopProductDto> {
        return try {
            Result.success(api.updateShopProduct(productId, body))
        } catch (e: HttpException) {
            Result.failure(Exception(parseErrorBody(e) ?: "Error al guardar el producto"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadProductImage(fileUri: Uri): Result<String> {
        return try {
            val mime = context.contentResolver.getType(fileUri) ?: "image/jpeg"
            val bytes = context.contentResolver.openInputStream(fileUri)?.use { it.readBytes() }
                ?: return Result.failure(Exception("No se pudo leer la imagen"))
            val body = bytes.toRequestBody(mime.toMediaType())
            val part = MultipartBody.Part.createFormData("file", "product.jpg", body)
            val resp = api.uploadProductImage(part)
            Result.success(resp.url)
        } catch (e: HttpException) {
            Result.failure(Exception(parseErrorBody(e) ?: "Error al subir la imagen"))
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
