package com.ares.ewe_shop.data.remote.api

import com.ares.ewe_shop.data.remote.model.AcceptRejectResponse
import com.ares.ewe_shop.data.remote.model.MarkPreparingRequest
import com.ares.ewe_shop.data.remote.model.ShopRequestOtpRequest
import com.ares.ewe_shop.data.remote.model.ShopRequestOtpResponse
import com.ares.ewe_shop.data.remote.model.ShopOrderDto
import com.ares.ewe_shop.data.remote.model.VerifyOtpRequest
import com.ares.ewe_shop.data.remote.model.VerifyOtpResponse
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface DobbyShopApi {

    @POST("auth/shop/request-otp")
    suspend fun requestOtp(@Body body: ShopRequestOtpRequest): ShopRequestOtpResponse

    /** Shop login: verify OTP and get token for the Shop (not the generic auth/verify-otp). */
    @POST("auth/shop/verify-otp")
    suspend fun verifyOtpShop(@Body body: VerifyOtpRequest): VerifyOtpResponse

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
}
