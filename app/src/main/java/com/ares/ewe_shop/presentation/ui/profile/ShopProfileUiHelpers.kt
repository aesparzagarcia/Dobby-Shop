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

data class ProfileMetricTrend(
    val deltaLabel: String,
    val isPositive: Boolean,
)

fun formatBreakdownDateRange(): String {
    val end = java.time.LocalDate.now()
    val start = end.minusDays(6)
    val fmt = java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale("es", "MX"))
    return "${start.format(fmt)} – ${end.format(fmt)}"
}

fun trendDeltaDouble(
    current: Double?,
    previous: Double?,
    lowerIsBetter: Boolean,
    formatDelta: (Double) -> String,
): ProfileMetricTrend? {
    if (current == null || previous == null) return null
    val delta = current - previous
    if (kotlin.math.abs(delta) < 0.05) return null
    val improved = if (lowerIsBetter) delta < 0 else delta > 0
    return ProfileMetricTrend(
        deltaLabel = formatDelta(kotlin.math.abs(delta)),
        isPositive = improved,
    )
}

fun trendDeltaInt(
    current: Int,
    previous: Int,
    lowerIsBetter: Boolean,
): ProfileMetricTrend? {
    val delta = current - previous
    if (delta == 0) return null
    val improved = if (lowerIsBetter) delta < 0 else delta > 0
    return ProfileMetricTrend(
        deltaLabel = kotlin.math.abs(delta).toString(),
        isPositive = improved,
    )
}
