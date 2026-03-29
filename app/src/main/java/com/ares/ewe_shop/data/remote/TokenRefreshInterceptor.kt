package com.ares.ewe_shop.data.remote

import com.ares.ewe_shop.data.local.datastore.SessionManager
import com.ares.ewe_shop.data.session.SessionEventBus
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

private const val HEADER_AUTH_RETRY = "X-Dobby-Auth-Retry"

/**
 * On 401, rotates tokens via [auth/shop/refresh]. Transient failures keep the session; invalid
 * refresh clears it.
 */
class TokenRefreshInterceptor @Inject constructor(
    private val sessionManager: SessionManager,
    private val shopTokenRefreshService: ShopTokenRefreshService,
    private val sessionEventBus: SessionEventBus,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.code != 401) return response

        if (request.header(HEADER_AUTH_RETRY) != null) {
            return response
        }

        if (shouldSkipRefresh(request)) {
            return response
        }

        response.close()

        val requestAccess = request.header("Authorization").orEmpty()
            .removePrefix("Bearer ")
            .trim()

        val result = runBlocking {
            shopTokenRefreshService.coordinateAfter401(requestAccess, sessionManager)
        }

        when (result) {
            is ShopCoordinatorResult.NoRefreshStored -> {
                runBlocking { sessionManager.clearSession() }
                sessionEventBus.notifySessionExpired()
                return chain.proceed(
                    request.newBuilder()
                        .header(HEADER_AUTH_RETRY, "1")
                        .removeHeader("Authorization")
                        .build()
                )
            }
            is ShopCoordinatorResult.SessionInvalid -> {
                runBlocking { sessionManager.clearSession() }
                sessionEventBus.notifySessionExpired()
                return chain.proceed(
                    request.newBuilder()
                        .header(HEADER_AUTH_RETRY, "1")
                        .removeHeader("Authorization")
                        .build()
                )
            }
            is ShopCoordinatorResult.TransientFailure -> {
                throw IOException("No se pudo renovar la sesión. Comprueba tu conexión e inténtalo de nuevo.")
            }
            is ShopCoordinatorResult.UseAccess,
            is ShopCoordinatorResult.NewTokens -> {
                val access = when (result) {
                    is ShopCoordinatorResult.UseAccess -> result.token
                    is ShopCoordinatorResult.NewTokens -> result.token
                    else -> error("unreachable")
                }
                val retry = request.newBuilder()
                    .header("Authorization", "Bearer $access")
                    .header(HEADER_AUTH_RETRY, "1")
                    .build()
                val retryResp = chain.proceed(retry)
                if (retryResp.code == 401) {
                    retryResp.close()
                    runBlocking { sessionManager.clearSession() }
                    sessionEventBus.notifySessionExpired()
                    return chain.proceed(
                        request.newBuilder()
                            .header(HEADER_AUTH_RETRY, "1")
                            .removeHeader("Authorization")
                            .build()
                    )
                }
                return retryResp
            }
        }
    }

    private fun shouldSkipRefresh(request: Request): Boolean {
        val u = request.url.toString()
        return u.contains("auth/shop/request-otp") ||
            u.contains("auth/shop/verify-otp") ||
            u.contains("auth/shop/refresh")
    }
}
