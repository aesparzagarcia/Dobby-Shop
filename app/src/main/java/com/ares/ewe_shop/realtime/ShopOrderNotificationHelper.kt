package com.ares.ewe_shop.realtime

import android.app.NotificationManager
import android.content.Context
import android.os.Build

object ShopOrderNotificationHelper {
    const val EXTRA_ORDER_ID = "order_id"

    private const val ORDER_NOTIFICATION_ID = 1001
    private const val ORDER_TAG_PREFIX = "order-"

    /** Matches backend `notificationGroupKey(orderId)`. */
    fun orderNotificationTag(orderId: String): String =
        "$ORDER_TAG_PREFIX${orderId.trim()}".take(64)

    /** Clears tray notifications for one order (FCM tag or local notify). */
    fun clearOrderNotifications(context: Context, orderId: String) {
        val trimmed = orderId.trim()
        if (trimmed.isEmpty()) return
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val tag = orderNotificationTag(trimmed)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.activeNotifications
                .filter { it.tag == tag }
                .forEach { status ->
                    notificationManager.cancel(status.tag, status.id)
                }
        }
        notificationManager.cancel(tag, ORDER_NOTIFICATION_ID)
    }

    /** Clears all order-related notifications when the shop returns to the app. */
    fun clearAllOrderNotifications(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.activeNotifications
            .filter { it.tag?.startsWith(ORDER_TAG_PREFIX) == true }
            .forEach { status ->
                notificationManager.cancel(status.tag, status.id)
            }
    }
}
