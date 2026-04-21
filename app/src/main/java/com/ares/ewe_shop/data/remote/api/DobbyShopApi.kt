package com.ares.ewe_shop.data.remote.api

import com.ares.ewe_shop.data.remote.model.AcceptRejectResponse
import com.ares.ewe_shop.data.remote.model.MarkPreparingRequest
import com.ares.ewe_shop.data.remote.model.ShopRequestOtpRequest
import com.ares.ewe_shop.data.remote.model.ShopRequestOtpResponse
import com.ares.ewe_shop.data.remote.model.ShopOrderDto
import com.ares.ewe_shop.data.remote.model.CreateShopProductRequest
import com.ares.ewe_shop.data.remote.model.ShopProductDto
import com.ares.ewe_shop.data.remote.model.ShopProfileDto
import com.ares.ewe_shop.data.remote.model.UploadImageResponse
import com.ares.ewe_shop.data.remote.model.VerifyOtpRequest
import com.ares.ewe_shop.data.remote.model.VerifyOtpResponse
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.Part
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface DobbyShopApi {

    @POST("auth/shop/request-otp")
    suspend fun requestOtp(@Body body: ShopRequestOtpRequest): ShopRequestOtpResponse

    /** Shop login: verify OTP and get token for the Shop (not the generic auth/verify-otp). */
    @POST("auth/shop/verify-otp")
    suspend fun verifyOtpShop(@Body body: VerifyOtpRequest): VerifyOtpResponse

    @GET("shop/profile")
    suspend fun getShopProfile(): ShopProfileDto

    @GET("shop/orders")
    suspend fun getOrders(@Query("status") status: String? = null): List<ShopOrderDto>

    @PATCH("shop/orders/{id}/accept")
    suspend fun acceptOrder(@Path("id") orderId: String): AcceptRejectResponse

    @PATCH("shop/orders/{id}/preparing")
    suspend fun markOrderPreparing(
        @Path("id") orderId: String,
        @Body body: MarkPreparingRequest
    ): AcceptRejectResponse

    @PATCH("shop/orders/{id}/ready-for-pickup")
    suspend fun markOrderReadyForPickup(@Path("id") orderId: String): AcceptRejectResponse

    @PATCH("shop/orders/{id}/reject")
    suspend fun rejectOrder(@Path("id") orderId: String): AcceptRejectResponse

    @GET("shop/products")
    suspend fun getShopProducts(): List<ShopProductDto>

    /** Crea un producto para la tienda del JWT (misma lógica que el panel, sin elegir otra tienda). */
    @POST("shop/products")
    suspend fun createShopProduct(@Body body: CreateShopProductRequest): ShopProductDto

    @PUT("shop/products/{id}")
    suspend fun updateShopProduct(
        @Path("id") productId: String,
        @Body body: CreateShopProductRequest
    ): ShopProductDto

    @Multipart
    @POST("upload/product-image")
    suspend fun uploadProductImage(@Part file: MultipartBody.Part): UploadImageResponse
}
