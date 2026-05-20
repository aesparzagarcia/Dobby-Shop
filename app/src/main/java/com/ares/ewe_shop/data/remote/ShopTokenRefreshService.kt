package com.ares.ewe_shop.data.remote

import com.ares.ewe_shop.BuildConfig
import com.ares.ewe_shop.data.local.datastore.SessionManager
import com.ares.ewe_shop.data.remote.model.ShopRefreshRequest
import com.ares.ewe_shop.data.remote.model.ShopRefreshResponse
import com.ares.ewe_shop.di.DobbyShopNoAuthClient
import com.google.gson.Gson
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

sealed interface ShopCoordinatorResult {
    data object NoRefreshStored : ShopCoordinatorResult
    data object SessionInvalid : ShopCoordinatorResult
    data object TransientFailure : ShopCoordinatorResult
    data class UseAccess(val token: String) : ShopCoordinatorResult
    data class NewTokens(val token: String) : ShopCoordinatorResult
}

sealed interface ShopLaunchRefreshOutcome {
    data object Skipped : ShopLaunchRefreshOutcome
    data object Refreshed : ShopLaunchRefreshOutcome
    data object Unchanged : ShopLaunchRefreshOutcome
    data object SessionDead : ShopLaunchRefreshOutcome
}

@Singleton
class ShopTokenRefreshService @Inject constructor(
    @DobbyShopNoAuthClient private val noAuthClient: OkHttpClient,
) {
    private val gson = Gson()
    private val mutex = Mutex()

    suspend fun coordinateAfter401(
        requestAccessToken: String,
        sessionManager: SessionManager,
    ): ShopCoordinatorResult = mutex.withLock {
        val currentAccess = sessionManager.authToken.first().orEmpty()
        if (currentAccess.isNotBlank() && currentAccess != requestAccessToken.trim()) {
            return@withLock ShopCoordinatorResult.UseAccess(currentAccess)
        }
        val refresh = sessionManager.refreshToken.first()
        if (refresh.isNullOrBlank()) {
            return@withLock ShopCoordinatorResult.NoRefreshStored
        }
        when (val r = runShopRefreshWithRetries(sessionManager, refresh)) {
            is HttpRefreshResult.Success -> {
                sessionManager.saveSession(r.accessToken, r.refreshToken)
                ShopCoordinatorResult.NewTokens(r.accessToken)
            }
            HttpRefreshResult.SessionInvalid -> ShopCoordinatorResult.SessionInvalid
            HttpRefreshResult.TransientFailure -> ShopCoordinatorResult.TransientFailure
        }
    }

    suspend fun refreshStoredSession(sessionManager: SessionManager): ShopLaunchRefreshOutcome =
        mutex.withLock {
            val refresh = sessionManager.refreshToken.first()
            if (refresh.isNullOrBlank()) {
                return@withLock ShopLaunchRefreshOutcome.Skipped
            }
            when (val r = runShopRefreshWithRetries(sessionManager, refresh)) {
                is HttpRefreshResult.Success -> {
                    sessionManager.saveSession(r.accessToken, r.refreshToken)
                    ShopLaunchRefreshOutcome.Refreshed
                }
                HttpRefreshResult.SessionInvalid -> {
                    sessionManager.clearSession()
                    ShopLaunchRefreshOutcome.SessionDead
                }
                HttpRefreshResult.TransientFailure -> ShopLaunchRefreshOutcome.Unchanged
            }
        }

    /**
     * Si [auth/shop/refresh] responde 401/403, reintenta tras un breve delay y vuelve a leer el
     * refresh en DataStore (p. ej. otra petición pudo rotar tokens). Solo entonces se considera
     * sesión muerta.
     */
    private suspend fun runShopRefreshWithRetries(
        sessionManager: SessionManager,
        initialRefresh: String,
    ): HttpRefreshResult {
        repeat(SHOP_REFRESH_ATTEMPTS) { attempt ->
            val refresh = if (attempt == 0) {
                initialRefresh
            } else {
                sessionManager.refreshToken.first().orEmpty()
            }
            if (refresh.isBlank()) return HttpRefreshResult.SessionInvalid
            when (val r = withContext(Dispatchers.IO) { executeRefresh(refresh) }) {
                is HttpRefreshResult.Success -> return r
                HttpRefreshResult.TransientFailure -> return r
                HttpRefreshResult.SessionInvalid -> {
                    if (attempt == SHOP_REFRESH_ATTEMPTS - 1) return HttpRefreshResult.SessionInvalid
                    delay(SHOP_REFRESH_RETRY_BASE_MS * (attempt + 1))
                }
            }
        }
        return HttpRefreshResult.SessionInvalid
    }

    private fun executeRefresh(refresh: String): HttpRefreshResult {
        val url = BuildConfig.BASE_URL.trimEnd('/') + "/auth/shop/refresh"
        val bodyJson = gson.toJson(ShopRefreshRequest(refresh))
        val httpReq = Request.Builder()
            .url(url)
            .post(bodyJson.toRequestBody(jsonMediaType))
            .build()
        return try {
            noAuthClient.newCall(httpReq).execute().use { resp ->
                when (resp.code) {
                    401, 403 -> HttpRefreshResult.SessionInvalid
                    in 500..599 -> HttpRefreshResult.TransientFailure
                    else -> {
                        if (!resp.isSuccessful) {
                            if (resp.code == 400) HttpRefreshResult.SessionInvalid
                            else HttpRefreshResult.TransientFailure
                        } else {
                            val body = resp.body?.string().orEmpty()
                            val parsed = try {
                                gson.fromJson(body, ShopRefreshResponse::class.java)
                            } catch (_: Exception) {
                                null
                            }
                            val access = parsed?.token
                            val nextRefresh = parsed?.refreshToken
                            if (access.isNullOrBlank() || nextRefresh.isNullOrBlank()) {
                                HttpRefreshResult.TransientFailure
                            } else {
                                HttpRefreshResult.Success(access, nextRefresh)
                            }
                        }
                    }
                }
            }
        } catch (_: IOException) {
            HttpRefreshResult.TransientFailure
        }
    }

    private sealed interface HttpRefreshResult {
        data class Success(val accessToken: String, val refreshToken: String) : HttpRefreshResult
        data object SessionInvalid : HttpRefreshResult
        data object TransientFailure : HttpRefreshResult
    }

    private companion object {
        private const val SHOP_REFRESH_ATTEMPTS = 3
        private const val SHOP_REFRESH_RETRY_BASE_MS = 300L
    }
}
