package com.ares.ewe_shop.presentation.ui.navigation

import android.net.Uri

object DobbyShopScreens {
    const val Splash = "splash"
    const val Phone = "phone"
    const val Otp = "otp/{phone}"
    const val Main = "main"
    const val OrderDetail = "orderDetail/{orderId}"

    fun otp(phone: String) = "otp/${Uri.encode(phone)}"
    fun orderDetail(orderId: String) = "orderDetail/${Uri.encode(orderId)}"
}
