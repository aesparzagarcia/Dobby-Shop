package com.ares.ewe_shop.data.remote.api

import com.ares.ewe_shop.data.remote.model.AcceptRejectResponse
import com.ares.ewe_shop.data.remote.model.FirebaseTokenResponse
import com.ares.ewe_shop.data.remote.model.MarkArrivedResponse
import com.ares.ewe_shop.data.remote.model.MarkDeliveredRequest
import com.ares.ewe_shop.data.remote.model.RegisterPushDeviceRequest
import com.ares.ewe_shop.data.remote.model.MarkPreparingRequest
import com.ares.ewe_shop.data.remote.model.RateCustomerRequest
import com.ares.ewe_shop.data.remote.model.UpdateCourierLocationRequest
import com.ares.ewe_shop.data.remote.model.UpdateDeliveryEtaRequest
import com.ares.ewe_shop.data.remote.model.UpdateDeliveryEtaResponse
import com.ares.ewe_shop.data.remote.model.VerifyDeliveryCodeRequest
import com.ares.ewe_shop.data.remote.model.VerifyDeliveryCodeResponse
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
import retrofit2.http.DELETE
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

    @POST("shop/push-device")
    suspend fun registerPushDevice(@Body body: RegisterPushDeviceRequest)

    @DELETE("shop/push-device")
    suspend fun unregisterPushDevice()

    @POST("shop/firebase-token")
    suspend fun getFirebaseCustomToken(): FirebaseTokenResponse

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

    /** Carwash: CONFIRMED → OUT_FOR_PICKUP (va a recoger el carro). */
    @PATCH("shop/orders/{id}/out-for-pickup")
    suspend fun markOrderOutForPickup(
        @Path("id") orderId: String,
        @Body body: MarkPreparingRequest
    ): AcceptRejectResponse

    /** Carwash: OUT_FOR_PICKUP → PICKED_UP (PIN; carro va al local). */
    @PATCH("shop/orders/{id}/confirm-pickup")
    suspend fun markOrderConfirmPickup(
        @Path("id") orderId: String,
        @Body body: MarkDeliveredRequest,
    ): AcceptRejectResponse

    /** Carwash: PICKED_UP → PREPARING (ya llegó al local). */
    @PATCH("shop/orders/{id}/start-wash")
    suspend fun markOrderStartWash(@Path("id") orderId: String): AcceptRejectResponse

    @PATCH("shop/orders/{id}/ready-for-pickup")
    suspend fun markOrderReadyForPickup(@Path("id") orderId: String): AcceptRejectResponse

    @PATCH("shop/orders/{id}/detailing")
    suspend fun markOrderDetailing(@Path("id") orderId: String): AcceptRejectResponse

    @PATCH("shop/orders/{id}/on-delivery")
    suspend fun markOrderOnDelivery(@Path("id") orderId: String): AcceptRejectResponse

    /** Carwash: la tienda llegó al domicilio del cliente (genera el PIN de entrega). */
    @PATCH("shop/orders/{id}/arrived")
    suspend fun markArrivedAtCustomer(@Path("id") orderId: String): MarkArrivedResponse

    /** Carwash: valida el PIN del cliente antes de cerrar la entrega. */
    @POST("shop/orders/{id}/verify-delivery-code")
    suspend fun verifyDeliveryCode(
        @Path("id") orderId: String,
        @Body body: VerifyDeliveryCodeRequest
    ): VerifyDeliveryCodeResponse

    /** Carwash: cierra la entrega con el PIN del cliente (ON_DELIVERY → DELIVERED). */
    @PATCH("shop/orders/{id}/delivered")
    suspend fun markOrderDelivered(
        @Path("id") orderId: String,
        @Body body: MarkDeliveredRequest
    ): AcceptRejectResponse

    /** Carwash: publica el ETA de conducción para el seguimiento del cliente. */
    @PATCH("shop/orders/{id}/delivery-eta")
    suspend fun updateDeliveryEta(
        @Path("id") orderId: String,
        @Body body: UpdateDeliveryEtaRequest
    ): UpdateDeliveryEtaResponse

    /** Carwash: publica la GPS del vehículo para el mapa del cliente. */
    @PATCH("shop/orders/{id}/courier-location")
    suspend fun updateCourierLocation(
        @Path("id") orderId: String,
        @Body body: UpdateCourierLocationRequest
    ): AcceptRejectResponse

    /** Carwash: calificación opcional del cliente tras entregar. */
    @POST("shop/orders/{id}/rate-customer")
    suspend fun rateCustomer(
        @Path("id") orderId: String,
        @Body body: RateCustomerRequest
    ): AcceptRejectResponse

    @PATCH("shop/orders/{id}/reject")
    suspend fun rejectOrder(@Path("id") orderId: String): AcceptRejectResponse

    @GET("shop/products")
    suspend fun getShopProducts(
        @Query("category") category: String? = null,
    ): List<ShopProductDto>

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
