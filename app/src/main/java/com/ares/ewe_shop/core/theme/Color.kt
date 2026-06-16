package com.ares.ewe_shop.core.theme

import androidx.compose.ui.graphics.Color

/**
 * Escala pura — de Onyx a Pure (misma que Dobby consumidor).
 * Nav/headers: Onyx · Texto secundario: Ash · Íconos/bordes: Graphite · Cards: Pure
 */
object DobbyPureScale {
    val Onyx = Color(0xFF0D0D0D)
    val Carbon = Color(0xFF1F1F1F)
    val Graphite = Color(0xFF3A3A3A)
    val Ash = Color(0xFF8A8A8A)
    val Mist = Color(0xFFE8E8E8)
    val Fog = Color(0xFFF5F5F5)
    val Pure = Color(0xFFFFFFFF)
}

/** Tokens semánticos — paridad con Dobby consumidor. */
object DobbyShopColors {
    val NavHeader = DobbyPureScale.Onyx
    val TextPrimary = DobbyPureScale.Onyx
    val TextSecondary = DobbyPureScale.Ash
    val IconBorder = DobbyPureScale.Graphite
    val CardSurface = DobbyPureScale.Pure
    val ScreenBackground = DobbyPureScale.Fog
    val Divider = DobbyPureScale.Mist
    val SurfaceMuted = DobbyPureScale.Fog

    val Primary = DobbyPureScale.Onyx
    val OnPrimary = DobbyPureScale.Pure

    val Dark = DobbyPureScale.Onyx
    val Light = DobbyPureScale.Fog
    val Carbon = DobbyPureScale.Carbon

    val Accent = Color(0xFF00C2A8)
    val Warning = Color(0xFFFFB800)

    /** Legacy names used across shop screens — map to brand colors. */
    val Purple = Primary
    val PurpleDark = Dark
    val PurpleLight = Light
    val PurpleMuted = Accent

    val Background = ScreenBackground
    val Surface = CardSurface
    val Border = Divider
    val Blue = Primary
    val BlueLight = Light
    val BlueDark = Dark

    /** Order status / semantic accents */
    val Orange = Warning
    val OrangeLight = Color(0xFFFFF8E6)
    val OrangeDark = Color(0xFFB38600)
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
