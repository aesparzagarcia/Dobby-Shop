package com.ares.ewe_shop.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

    val authToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTH_TOKEN]
    }

    val refreshToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.REFRESH_TOKEN]
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        !prefs[Keys.AUTH_TOKEN].isNullOrBlank() || !prefs[Keys.REFRESH_TOKEN].isNullOrBlank()
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

    /** Solo rota tokens (mantiene tienda id/nombre/tipo). */
    suspend fun saveSession(accessToken: String, refreshToken: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTH_TOKEN] = accessToken
            prefs[Keys.REFRESH_TOKEN] = refreshToken
        }
    }

    suspend fun saveSession(
        accessToken: String,
        refreshToken: String,
        shopId: String?,
        shopName: String?,
        shopType: String? = null,
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTH_TOKEN] = accessToken
            prefs[Keys.REFRESH_TOKEN] = refreshToken
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
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.AUTH_TOKEN)
            prefs.remove(Keys.REFRESH_TOKEN)
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.SHOP_NAME)
            prefs.remove(Keys.SHOP_TYPE)
        }
    }
}
