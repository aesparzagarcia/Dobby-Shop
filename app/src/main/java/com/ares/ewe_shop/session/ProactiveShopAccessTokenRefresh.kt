package com.ares.ewe_shop.session

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.ares.ewe_shop.core.auth.AccessTokenJwtParser
import com.ares.ewe_shop.data.local.datastore.SessionManager
import com.ares.ewe_shop.data.remote.ShopLaunchRefreshOutcome
import com.ares.ewe_shop.data.remote.ShopTokenRefreshService
import com.ares.ewe_shop.data.session.SessionEventBus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Con la app en primer plano, revisa el [exp] del access JWT y llama a [auth/shop/refresh]
 * antes de que venza, para que casi no haya 401 ni sensación de “sesión caída”.
 */
@Singleton
class ProactiveShopAccessTokenRefresh @Inject constructor(
    private val sessionManager: SessionManager,
    private val shopTokenRefreshService: ShopTokenRefreshService,
    private val sessionEventBus: SessionEventBus,
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var foregroundJob: Job? = null

    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        foregroundJob?.cancel()
        foregroundJob = scope.launch {
            while (isActive) {
                runCatching { refreshIfExpiringSoon() }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        foregroundJob?.cancel()
        foregroundJob = null
    }

    private suspend fun refreshIfExpiringSoon() {
        if (!sessionManager.isLoggedIn.first()) return
        val access = sessionManager.authToken.first().orEmpty()
        if (access.isBlank()) return
        val exp = AccessTokenJwtParser.expiryEpochSeconds(access) ?: return
        val now = System.currentTimeMillis() / 1000
        val secondsLeft = exp - now
        if (secondsLeft > REFRESH_WHEN_SECONDS_LEFT) return
        if (sessionManager.refreshToken.first().isNullOrBlank()) return
        when (shopTokenRefreshService.refreshStoredSession(sessionManager)) {
            ShopLaunchRefreshOutcome.SessionDead -> sessionEventBus.notifySessionExpired()
            ShopLaunchRefreshOutcome.Skipped,
            ShopLaunchRefreshOutcome.Refreshed,
            ShopLaunchRefreshOutcome.Unchanged,
            -> Unit
        }
    }

    private companion object {
        /** Access JWT tienda: backend ~2h; renovar con margen antes de exp. */
        const val REFRESH_WHEN_SECONDS_LEFT = 10 * 60L
        const val POLL_INTERVAL_MS = 3 * 60 * 1000L
    }
}
