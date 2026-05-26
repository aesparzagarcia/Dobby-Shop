package com.ares.ewe_shop.presentation.ui.product

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ares.ewe_shop.core.theme.DobbyShopColors
import com.ares.ewe_shop.core.util.absoluteUploadUrl

@Composable
fun ShopProductCard(
    name: String,
    imageUrl: String?,
    price: Double,
    rate: Float,
    hasPromotion: Boolean,
    discount: Int,
    isActive: Boolean,
    categoryLabel: String? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onMenuClick: () -> Unit = onClick,
) {
    val validDiscount = discount.coerceIn(0, 100)
    val showPromotion = hasPromotion && validDiscount > 0
    val discountedPrice = if (showPromotion) price * (1 - validDiscount / 100.0) else price
    val cardAlpha = if (isActive) 1f else 0.72f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(cardAlpha)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DobbyShopColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = absoluteUploadUrl(imageUrl),
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.Center,
                    )
                } else {
                    Text(
                        text = name.take(1).uppercase(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = DobbyShopColors.Purple.copy(alpha = 0.45f),
                    )
                }
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(32.dp)
                        .background(DobbyShopColors.Surface.copy(alpha = 0.92f), CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Opciones",
                        tint = DobbyShopColors.TextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                if (showPromotion) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFFFE34D))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "-$validDiscount%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = DobbyShopColors.TextPrimary,
                        )
                        Text(
                            text = "$${String.format("%.2f", price)}",
                            fontSize = 11.sp,
                            color = DobbyShopColors.TextSecondary,
                            textDecoration = TextDecoration.LineThrough,
                        )
                    }
                }
                if (!isActive) {
                    Text(
                        text = "Pausado",
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DobbyShopColors.RedLight)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DobbyShopColors.RedDark,
                    )
                }
            }

            Text(
                text = "$${String.format("%.2f", discountedPrice)}",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = DobbyShopColors.TextPrimary,
                modifier = Modifier.padding(top = 10.dp, start = 10.dp, end = 10.dp),
            )
            Text(
                text = name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = DobbyShopColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp, start = 10.dp, end = 10.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, start = 10.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ProductRatingDisplay(rate = rate)
                categoryLabel?.let { label ->
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = DobbyShopColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductRatingDisplay(rate: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "★",
            fontSize = 14.sp,
            color = DobbyShopColors.Purple,
        )
        Text(
            text = String.format("%.1f", rate.coerceIn(0f, 5f)),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = DobbyShopColors.TextSecondary,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
