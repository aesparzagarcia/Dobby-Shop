package com.ares.ewe_shop.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = DobbyShopColors.Primary,
    onPrimary = DobbyShopColors.OnPrimary,
    primaryContainer = DobbyPureScale.Mist,
    onPrimaryContainer = DobbyShopColors.TextPrimary,
    secondary = DobbyShopColors.Accent,
    onSecondary = DobbyPureScale.Pure,
    tertiary = DobbyShopColors.Warning,
    onTertiary = DobbyShopColors.TextPrimary,
    background = DobbyShopColors.ScreenBackground,
    onBackground = DobbyShopColors.TextPrimary,
    surface = DobbyShopColors.CardSurface,
    onSurface = DobbyShopColors.TextPrimary,
    surfaceVariant = DobbyPureScale.Fog,
    onSurfaceVariant = DobbyShopColors.TextSecondary,
    outline = DobbyShopColors.IconBorder,
    outlineVariant = DobbyPureScale.Mist,
)

@Composable
fun DobbyShopTheme(
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content,
    )
}
