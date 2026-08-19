package com.ares.ewe_shop.realtime

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ares.ewe_shop.R
import com.ares.ewe_shop.presentation.ui.MainActivity

object ShopOrderNotificationHelper {
    const val EXTRA_ORDER_ID = "order_id"

    private const val CHANNEL_ID = "dobbyshop_orders"
    private const val CHANNEL_NAME = "Pedidos"
    private const val ORDER_NOTIFICATION_ID = 1001
    private const val ORDER_TAG_PREFIX = "order-"

    /** Matches backend `notificationGroupKey(orderId)`. */
    fun orderNotificationTag(orderId: String): String =
        "$ORDER_TAG_PREFIX${orderId.trim()}".take(64)

    fun show(context: Context, title: String, body: String, orderId: String?) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(notificationManager)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            orderId?.let { putExtra(EXTRA_ORDER_ID, it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            orderId?.hashCode() ?: 0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            .setColor(ContextCompat.getColor(context, R.color.dobby_notification_accent))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val trimmed = orderId?.trim().orEmpty()
        if (trimmed.isNotEmpty()) {
            notificationManager.notify(orderNotificationTag(trimmed), ORDER_NOTIFICATION_ID, notification)
        } else {
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        }
    }

    fun titleAndBodyForShopPush(
        type: String?,
        status: String? = null,
        shopType: String? = null,
        deliveryManName: String? = null,
        titleFromData: String? = null,
        bodyFromData: String? = null,
    ): Pair<String, String> {
        titleFromData?.trim()?.takeIf { it.isNotEmpty() }?.let { t ->
            bodyFromData?.trim()?.takeIf { it.isNotEmpty() }?.let { b ->
                return t to b
            }
        }
        val carWash = shopType.equals("CAR_WASH", ignoreCase = true)
        return when (type) {
            "shop_new_order" -> if (carWash) {
                "Nuevo servicio" to "Tienes un servicio pendiente por aceptar."
            } else {
                "Nuevo pedido" to "Tienes un pedido pendiente por aceptar."
            }
            "shop_courier_assigned" -> {
                val name = deliveryManName?.trim()
                "Pedido aceptado por repartidor" to if (!name.isNullOrEmpty()) {
                    "El pedido fue aceptado por $name."
                } else {
                    "Un repartidor aceptó el pedido."
                }
            }
            "order_status" -> when (status?.uppercase()) {
                "ON_DELIVERY" -> "Pedido en camino al cliente" to
                    "El repartidor recogió el pedido y va en camino al cliente."
                "DELIVERED" -> if (carWash) {
                    "Servicio entregado" to "El coche ha sido entregado."
                } else {
                    "Pedido entregado" to "El pedido fue entregado."
                }
                else -> "Actualización de pedido" to "Estado: ${status ?: "—"}"
            }
            else -> "Actualización" to "Hay novedades en tus pedidos."
        }
    }

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

    private fun ensureChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Avisos de pedidos nuevos y cambios de estado"
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)
    }
}
