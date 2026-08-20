package com.ares.ewe_shop.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dobbyshop_session")

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        /** Shop id (JWT sub) after login. */
        val USER_ID = stringPreferencesKey("user_id")
        val SHOP_NAME = stringPreferencesKey("shop_name")
        /** RESTAURANT | SHOP | SERVICE_PROVIDER | CAR_WASH */
        val SHOP_TYPE = stringPreferencesKey("shop_type")
    }

    private val tokenStore = EncryptedTokenStore(context)
    private val migrationMutex = Mutex()
    @Volatile private var migrated = false

    private val _authToken = MutableStateFlow(tokenStore.accessToken)
    private val _refreshToken = MutableStateFlow(tokenStore.refreshToken)

    val authToken: Flow<String?> = flow {
        migrateLegacyTokensIfNeeded()
        _authToken.value = tokenStore.accessToken
        emitAll(_authToken)
    }

    val refreshToken: Flow<String?> = flow {
        migrateLegacyTokensIfNeeded()
        _refreshToken.value = tokenStore.refreshToken
        emitAll(_refreshToken)
    }

    val isLoggedIn: Flow<Boolean> = combine(authToken, refreshToken) { access, refresh ->
        !access.isNullOrBlank() || !refresh.isNullOrBlank()
    }

    val shopName: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.SHOP_NAME]
    }

    val shopId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.USER_ID]
    }

    val shopType: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.SHOP_TYPE]
    }

    private suspend fun migrateLegacyTokensIfNeeded() {
        if (migrated) return
        migrationMutex.withLock {
            if (migrated) return
            val hasSecure =
                !tokenStore.accessToken.isNullOrBlank() || !tokenStore.refreshToken.isNullOrBlank()
            if (!hasSecure) {
                val prefs = context.dataStore.data.first()
                val legacyAccess = prefs[Keys.AUTH_TOKEN]
                val legacyRefresh = prefs[Keys.REFRESH_TOKEN]
                if (!legacyAccess.isNullOrBlank() || !legacyRefresh.isNullOrBlank()) {
                    tokenStore.save(
                        accessToken = legacyAccess.orEmpty(),
                        refreshToken = legacyRefresh.orEmpty(),
                    )
                    _authToken.value = tokenStore.accessToken
                    _refreshToken.value = tokenStore.refreshToken
                }
            }
            context.dataStore.edit { prefs ->
                prefs.remove(Keys.AUTH_TOKEN)
                prefs.remove(Keys.REFRESH_TOKEN)
            }
            migrated = true
        }
    }

    /** Solo rota tokens (mantiene tienda id/nombre/tipo). */
    suspend fun saveSession(accessToken: String, refreshToken: String) {
        migrateLegacyTokensIfNeeded()
        tokenStore.save(accessToken, refreshToken)
        _authToken.value = accessToken
        _refreshToken.value = refreshToken
    }

    suspend fun saveSession(
        accessToken: String,
        refreshToken: String,
        shopId: String?,
        shopName: String?,
        shopType: String? = null,
    ) {
        migrateLegacyTokensIfNeeded()
        tokenStore.save(accessToken, refreshToken)
        _authToken.value = accessToken
        _refreshToken.value = refreshToken
        context.dataStore.edit { prefs ->
            shopId?.let { prefs[Keys.USER_ID] = it }
            shopName?.let { prefs[Keys.SHOP_NAME] = it }
            shopType?.let { prefs[Keys.SHOP_TYPE] = it }
        }
    }

    suspend fun updateShopType(shopType: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SHOP_TYPE] = shopType
        }
    }

    suspend fun clearSession() {
        migrateLegacyTokensIfNeeded()
        tokenStore.clear()
        _authToken.value = null
        _refreshToken.value = null
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.AUTH_TOKEN)
            prefs.remove(Keys.REFRESH_TOKEN)
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.SHOP_NAME)
            prefs.remove(Keys.SHOP_TYPE)
        }
    }

    suspend fun prepareSession() {
        migrateLegacyTokensIfNeeded()
        _authToken.value = tokenStore.accessToken
        _refreshToken.value = tokenStore.refreshToken
    }
}
