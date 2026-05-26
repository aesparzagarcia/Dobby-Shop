package com.ares.ewe_shop.core.auth

import android.util.Base64
import org.json.JSONObject

/**
 * Lee [exp] del payload JWT sin verificar firma (suficiente para programar refresh proactivo).
 */
object AccessTokenJwtParser {

    fun expiryEpochSeconds(jwt: String): Long? {
        val parts = jwt.trim().split('.')
        if (parts.size < 2) return null
        val payload = parts[1]
        val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
        val decoded = try {
            String(Base64.decode(padded, Base64.URL_SAFE or Base64.NO_WRAP))
        } catch (_: Exception) {
            return null
        }
        return try {
            val exp = JSONObject(decoded).optLong("exp", -1L)
            if (exp < 0) null else exp
        } catch (_: Exception) {
            null
        }
    }
}
