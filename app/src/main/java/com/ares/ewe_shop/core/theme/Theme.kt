package com.ares.ewe_shop.core.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = DobbyShopColors.Primary,
    onPrimary = Color.White,
    primaryContainer = DobbyShopColors.Light,
    onPrimaryContainer = DobbyShopColors.Dark,
    secondary = DobbyShopColors.Accent,
    onSecondary = Color.White,
    tertiary = DobbyShopColors.Warning,
    onTertiary = DobbyShopColors.Dark,
    background = Color.White,
    onBackground = DobbyShopColors.Dark,
    surface = DobbyShopColors.Surface,
    onSurface = DobbyShopColors.Dark,
    surfaceVariant = DobbyShopColors.Light,
    onSurfaceVariant = DobbyShopColors.Dark.copy(alpha = 0.72f),
    outline = DobbyShopColors.Border,
)

@Composable
fun DobbyShopTheme(
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            dynamicLightColorScheme(context)
        }
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
