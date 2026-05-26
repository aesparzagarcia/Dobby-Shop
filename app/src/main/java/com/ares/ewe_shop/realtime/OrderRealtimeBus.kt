package com.ares.ewe_shop.realtime

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRealtimeBus @Inject constructor() {
    private val _refreshOrders = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    val refreshOrders: SharedFlow<Unit> = _refreshOrders.asSharedFlow()

    fun notifyOrdersChanged() {
        _refreshOrders.tryEmit(Unit)
    }
}
