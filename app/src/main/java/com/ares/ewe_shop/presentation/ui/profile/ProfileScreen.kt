package com.ares.ewe_shop.presentation.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ares.ewe_shop.core.theme.DobbyPureScale
import com.ares.ewe_shop.core.theme.DobbyShopColors
import com.ares.ewe_shop.data.remote.model.ShopMissionDto
import com.ares.ewe_shop.data.remote.model.ShopProfileBreakdownDto
import com.ares.ewe_shop.data.remote.model.ShopProfileDto
import com.ares.ewe_shop.data.remote.model.ShopProfileWindowMetricsDto
import com.ares.ewe_shop.presentation.ui.main.LocalMainBottomBarPadding
import com.ares.ewe_shop.presentation.viewmodel.profile.ShopProfileViewModel
import java.util.Locale

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShopProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val scroll = rememberScrollState()
    val bottomBarPadding = LocalMainBottomBarPadding.current

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        containerColor = DobbyShopColors.Background,
    ) { padding ->
        when {
            uiState.isLoading && uiState.profile == null -> {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = DobbyShopColors.Primary)
                }
            }
            uiState.errorMessage != null && uiState.profile == null -> {
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(padding)
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = uiState.errorMessage!!,
                        color = DobbyShopColors.Red,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadProfile() }) {
                        Text("Reintentar")
                    }
                }
            }
            else -> {
                val profile = uiState.profile
                Column(
                    modifier = modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(padding)
                        .verticalScroll(scroll)
                        .padding(bottom = bottomBarPadding + 16.dp),
                ) {
                    ProfileTopBar(
                        isRefreshing = uiState.isLoading,
                        onRefresh = { viewModel.loadProfile() },
                    )
                    if (profile != null) {
                        ProfileHeader(profile = profile)
                        Spacer(modifier = Modifier.height(20.dp))
                        RestaurantScoreCard(profile = profile)
                        Spacer(modifier = Modifier.height(12.dp))
                        LevelBenefitsBanner(levelKey = profile.levelKey)
                        Spacer(modifier = Modifier.height(8.dp))
                        ScoreBlendNote(profile = profile)
                        Spacer(modifier = Modifier.height(24.dp))
                        BreakdownSection(
                            breakdown = profile.breakdown,
                            previous = profile.breakdownPrev7d,
                        )
                        if (profile.insights.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            InsightsSection(insights = profile.insights)
                        }
                        if (profile.missions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            MissionsSection(missions = profile.missions)
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    TextButton(
                        onClick = onLogout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                    ) {
                        Text(
                            text = "Cerrar sesión",
                            color = DobbyShopColors.Red,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileTopBar(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = onRefresh,
            enabled = !isRefreshing,
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = DobbyShopColors.TextPrimary,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Actualizar",
                color = DobbyShopColors.TextPrimary,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ProfileHeader(profile: ShopProfileDto) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val logoUrl = resolveShopLogoUrl(profile.logoUrl)
        if (logoUrl != null) {
            AsyncImage(
                model = logoUrl,
                contentDescription = profile.name,
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .border(2.dp, DobbyPureScale.Mist, CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(DobbyPureScale.Mist),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Store,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = DobbyShopColors.TextSecondary,
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = profile.name.ifBlank { "Tu tienda" },
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = DobbyShopColors.TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(50),
            color = DobbyShopColors.Primary,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(text = "\uD83D\uDC51", fontSize = 14.sp)
                Text(
                    text = "Nivel: ${restaurantLevelDisplayName(profile.levelKey)}",
                    color = DobbyShopColors.OnPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun RestaurantScoreCard(profile: ShopProfileDto) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        color = DobbyShopColors.Carbon,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Restaurant Score",
                            color = DobbyPureScale.Mist,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = DobbyPureScale.Ash,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${profile.restaurantScore} / 100",
                        color = DobbyShopColors.OnPrimary,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = restaurantLevelEmoji(profile.levelKey),
                    fontSize = 48.sp,
                )
            }
            val next = profile.nextLevelMinScore
            if (next != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Progreso al siguiente nivel ($next pts)",
                        color = DobbyPureScale.Ash,
                        fontSize = 12.sp,
                    )
                    Text(
                        text = "$next",
                        color = DobbyPureScale.Ash,
                        fontSize = 12.sp,
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { profile.progressToNextLevel.toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = DobbyShopColors.OnPrimary,
                    trackColor = DobbyPureScale.Graphite,
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "¡Puntuación máxima alcanzada!",
                    color = DobbyShopColors.Accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun LevelBenefitsBanner(levelKey: String) {
    val blurb = levelBenefitsBlurb(levelKey)
    if (blurb.isBlank()) return
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        color = DobbyShopColors.CardSurface,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DobbyShopColors.Primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Campaign,
                    contentDescription = null,
                    tint = DobbyShopColors.OnPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = blurb,
                modifier = Modifier.weight(1f),
                color = DobbyShopColors.TextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = DobbyShopColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun ScoreBlendNote(profile: ShopProfileDto) {
    val w = profile.scoreBlendWeights
    Text(
        text = "Tu score combina: últimos 7 días (${(w.last7d * 100).toInt()}%), " +
            "días 8–30 (${(w.days8To30 * 100).toInt()}%), " +
            "histórico (${(w.historical * 100).toInt()}%).",
        modifier = Modifier.padding(horizontal = 20.dp),
        color = DobbyShopColors.TextSecondary,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BreakdownSection(
    breakdown: ShopProfileBreakdownDto,
    previous: ShopProfileWindowMetricsDto?,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Desglose (ventana 7 días)",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = DobbyShopColors.TextPrimary,
            )
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DobbyShopColors.CardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DobbyPureScale.Mist),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = DobbyShopColors.TextSecondary,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatBreakdownDateRange(),
                        fontSize = 12.sp,
                        color = DobbyShopColors.TextPrimary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        val ratingValue = breakdown.avgShopRating?.let {
            String.format(Locale.US, "%.1f (%d)", it, breakdown.ratedOrders)
        } ?: if (breakdown.shopRatingCountAggregate > 0) {
            String.format(Locale.US, "%.1f (%d)", breakdown.shopRateAggregate, breakdown.shopRatingCountAggregate)
        } else {
            "—"
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = 2,
        ) {
            MetricCard(
                icon = Icons.Default.ShoppingBag,
                iconTint = DobbyShopColors.Primary,
                iconBg = DobbyPureScale.Fog,
                label = "Órdenes completadas",
                value = "${breakdown.ordersDelivered}",
                modifier = Modifier.weight(1f, fill = true),
            )
            MetricCard(
                icon = Icons.Outlined.ReceiptLong,
                iconTint = DobbyShopColors.Primary,
                iconBg = DobbyPureScale.Fog,
                label = "Pedidos (últ. 7 días)",
                value = "${breakdown.ordersLast7d}",
                modifier = Modifier.weight(1f, fill = true),
            )
            MetricCard(
                icon = Icons.Default.CheckCircle,
                iconTint = DobbyShopColors.Green,
                iconBg = DobbyShopColors.GreenLight,
                label = "Tasa de aceptación",
                value = String.format(Locale("es", "MX"), "%.1f%%", breakdown.acceptanceRatePct),
                trend = previous?.let {
                    trendDeltaDouble(
                        current = breakdown.acceptanceRatePct,
                        previous = it.acceptanceRatePct,
                        lowerIsBetter = false,
                        formatDelta = { d -> String.format(Locale("es", "MX"), "%.1f%%", d) },
                    )
                },
                modifier = Modifier.weight(1f, fill = true),
            )
            MetricCard(
                icon = Icons.Default.AccessTime,
                iconTint = DobbyShopColors.Orange,
                iconBg = DobbyShopColors.OrangeLight,
                label = "Tiempo prep. prom.",
                value = breakdown.avgPrepMinutes?.let {
                    String.format(Locale("es", "MX"), "%.0f min", it)
                } ?: "—",
                trend = previous?.avgPrepMinutes?.let { prev ->
                    breakdown.avgPrepMinutes?.let { curr ->
                        trendDeltaDouble(
                            current = curr,
                            previous = prev,
                            lowerIsBetter = true,
                            formatDelta = { d -> String.format(Locale("es", "MX"), "%.1f min", d) },
                        )
                    }
                },
                modifier = Modifier.weight(1f, fill = true),
            )
            MetricCard(
                icon = Icons.Default.Timer,
                iconTint = Color(0xFF3B82F6),
                iconBg = Color(0xFFEFF6FF),
                label = "A tiempo (prep.)",
                value = breakdown.onTimePrepPct?.let { "$it%" } ?: "—",
                trend = previous?.onTimePrepPct?.let { prev ->
                    breakdown.onTimePrepPct?.let { curr ->
                        trendDeltaInt(current = curr, previous = prev, lowerIsBetter = false)?.copy(
                            deltaLabel = "${kotlin.math.abs(curr - prev)}%",
                        )
                    }
                },
                modifier = Modifier.weight(1f, fill = true),
            )
            MetricCard(
                icon = Icons.Default.Star,
                iconTint = DobbyShopColors.Orange,
                iconBg = DobbyShopColors.OrangeLight,
                label = "Rating usuarios",
                value = ratingValue,
                trend = previous?.avgShopRating?.let { prev ->
                    breakdown.avgShopRating?.let { curr ->
                        trendDeltaDouble(
                            current = curr,
                            previous = prev,
                            lowerIsBetter = false,
                            formatDelta = { d -> String.format(Locale.US, "%.1f", d) },
                        )
                    }
                },
                modifier = Modifier.weight(1f, fill = true),
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        CancellationMetricCard(
            count = breakdown.ordersCancelledShop,
            trend = previous?.let {
                trendDeltaInt(
                    current = breakdown.ordersCancelledShop,
                    previous = it.ordersCancelledShop,
                    lowerIsBetter = true,
                )
            },
        )
    }
}

@Composable
private fun MetricCard(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    trend: ProfileMetricTrend? = null,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = DobbyShopColors.CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DobbyPureScale.Mist),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = label,
                color = DobbyShopColors.TextSecondary,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = DobbyShopColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            if (trend != null) {
                Spacer(modifier = Modifier.height(6.dp))
                MetricTrendRow(trend = trend)
            }
        }
    }
}

@Composable
private fun CancellationMetricCard(
    count: Int,
    trend: ProfileMetricTrend?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = DobbyShopColors.CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DobbyPureScale.Mist),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DobbyShopColors.RedLight),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = null,
                    tint = DobbyShopColors.Red,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Cancelaciones tienda",
                    color = DobbyShopColors.TextSecondary,
                    fontSize = 12.sp,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$count",
                    color = DobbyShopColors.TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (trend != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    MetricTrendRow(trend = trend)
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = DobbyShopColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun MetricTrendRow(trend: ProfileMetricTrend) {
    val color = if (trend.isPositive) DobbyShopColors.Green else DobbyShopColors.Red
    val arrow = if (trend.isPositive) "\u2191" else "\u2193"
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$arrow ${trend.deltaLabel}",
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "vs 7 días anteriores",
            color = DobbyShopColors.TextSecondary,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun InsightsSection(insights: List<String>) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = "Insights",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = DobbyShopColors.TextPrimary,
        )
        Spacer(modifier = Modifier.height(10.dp))
        insights.forEach { line ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = DobbyShopColors.CardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DobbyPureScale.Mist),
            ) {
                Text(
                    text = line,
                    modifier = Modifier.padding(14.dp),
                    color = DobbyShopColors.TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
            }
        }
    }
}

@Composable
private fun MissionsSection(missions: List<ShopMissionDto>) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = "Misiones",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = DobbyShopColors.TextPrimary,
        )
        Spacer(modifier = Modifier.height(10.dp))
        missions.forEach { mission ->
            ShopMissionRow(mission = mission)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ShopMissionRow(mission: ShopMissionDto) {
    val icon = if (mission.completed) "\u2705" else "\u23F3"
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = DobbyShopColors.CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DobbyPureScale.Mist),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "$icon ${mission.title}",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = DobbyShopColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${mission.progress} / ${mission.goal}",
                fontSize = 13.sp,
                color = DobbyShopColors.TextSecondary,
            )
            if (mission.goal > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = {
                        (mission.progress.toFloat() / mission.goal).coerceIn(0f, 1f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = DobbyShopColors.Primary,
                    trackColor = DobbyPureScale.Mist,
                )
            }
        }
    }
}
