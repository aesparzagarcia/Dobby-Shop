package com.ares.ewe_shop.core.theme

import androidx.compose.ui.graphics.Color

/** Dobby brand palette (parity with Dobby consumer app). */
object DobbyShopColors {
    val Primary = Color(0xFF0061FF)
    val Accent = Color(0xFF00C2A8)
    val Light = Color(0xFFF0F4FF)
    val Dark = Color(0xFF1D2B4F)
    val Warning = Color(0xFFFFB800)

    /** Legacy names used across shop screens — map to brand colors. */
    val Purple = Primary
    val PurpleDark = Dark
    val PurpleLight = Light
    val PurpleMuted = Accent

    val Background = Light
    val Surface = Color.White
    val TextPrimary = Dark
    val TextSecondary = Color(0xFF6B7280)
    val Border = Color(0xFFE5E7EB)

    /** Order status / semantic accents */
    val Orange = Warning
    val OrangeLight = Color(0xFFFFF8E6)
    val OrangeDark = Color(0xFFB38600)
    val Blue = Primary
    val BlueLight = Light
    val BlueDark = Dark
    val Teal = Accent
    val TealLight = Color(0xFFE6FBF7)
    val TealDark = Color(0xFF009985)
    val Green = Color(0xFF22C55E)
    val GreenLight = Color(0xFFECFDF5)
    val GreenDark = Color(0xFF15803D)
    val Red = Color(0xFFEF4444)
    val RedLight = Color(0xFFFEF2F2)
    val RedDark = Color(0xFFB91C1C)
}
