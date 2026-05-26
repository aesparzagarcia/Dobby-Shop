package com.ares.ewe_shop.presentation.ui.profile

import com.ares.ewe_shop.BuildConfig

/** URL absoluta para Coil; rutas relativas se resuelven contra el host del API. */
fun resolveShopLogoUrl(logoUrl: String?): String? {
    val raw = logoUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true)) {
        return raw
    }
    val origin = BuildConfig.BASE_URL
        .trim()
        .trimEnd('/')
        .removeSuffix("/api")
        .trimEnd('/')
    val path = raw.trimStart('/')
    return "$origin/$path"
}

fun restaurantLevelEmoji(levelKey: String): String = when (levelKey.uppercase()) {
    "ELITE" -> "\uD83E\uDD47" // 1st place medal
    "PRO" -> "\uD83E\uDD48" // 2nd place medal
    "REGULAR" -> "\uD83E\uDD49" // 3rd place medal
    "AT_RISK" -> "\u26A0\uFE0F" // warning
    else -> "\u2B50" // star
}

fun restaurantLevelDisplayName(levelKey: String): String = when (levelKey.uppercase()) {
    "ELITE" -> "Elite"
    "PRO" -> "Pro"
    "REGULAR" -> "Regular"
    "AT_RISK" -> "En riesgo"
    else -> levelKey
}

fun levelBenefitsBlurb(levelKey: String): String = when (levelKey.uppercase()) {
    "ELITE" -> "Máxima visibilidad en búsquedas, badge destacado y mejores condiciones."
    "PRO" -> "Buena visibilidad y acceso a promociones de la plataforma."
    "REGULAR" -> "Posicionamiento estándar; mejora tu score para desbloquear beneficios."
    "AT_RISK" -> "Menos visibilidad; revisa cancelaciones y tiempos de preparación."
    else -> ""
}
