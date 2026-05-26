package com.ares.ewe_shop.data.remote.model

import com.google.gson.annotations.SerializedName

data class ShopProfileDto(
    @SerializedName("name") val name: String,
    @SerializedName("logo_url") val logoUrl: String? = null,
    @SerializedName("restaurant_score") val restaurantScore: Int,
    @SerializedName("level_key") val levelKey: String,
    @SerializedName("progress_to_next_level") val progressToNextLevel: Double,
    @SerializedName("next_level_min_score") val nextLevelMinScore: Int? = null,
    @SerializedName("score_blend_weights") val scoreBlendWeights: ScoreBlendWeightsDto,
    @SerializedName("breakdown") val breakdown: ShopProfileBreakdownDto,
    @SerializedName("insights") val insights: List<String>,
    @SerializedName("missions") val missions: List<ShopMissionDto>,
)

data class ScoreBlendWeightsDto(
    @SerializedName("last_7d") val last7d: Double,
    @SerializedName("days_8_to_30") val days8To30: Double,
    @SerializedName("historical") val historical: Double,
)

data class ShopProfileBreakdownDto(
    @SerializedName("orders_delivered") val ordersDelivered: Int,
    @SerializedName("orders_cancelled_shop") val ordersCancelledShop: Int,
    @SerializedName("reject_pending") val rejectPending: Int,
    @SerializedName("cancel_after_confirm") val cancelAfterConfirm: Int,
    @SerializedName("acceptance_rate_pct") val acceptanceRatePct: Double,
    @SerializedName("on_time_prep_pct") val onTimePrepPct: Int? = null,
    @SerializedName("avg_prep_minutes") val avgPrepMinutes: Double? = null,
    @SerializedName("rated_orders") val ratedOrders: Int,
    @SerializedName("avg_shop_rating") val avgShopRating: Double? = null,
    @SerializedName("orders_last_7d") val ordersLast7d: Int,
    @SerializedName("orders_last_30d") val ordersLast30d: Int,
    @SerializedName("shop_rate_aggregate") val shopRateAggregate: Double,
    @SerializedName("shop_rating_count_aggregate") val shopRatingCountAggregate: Int,
)

data class ShopMissionDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("progress") val progress: Int,
    @SerializedName("goal") val goal: Int,
    @SerializedName("completed") val completed: Boolean,
)
