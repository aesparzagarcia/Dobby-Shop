package com.ares.ewe_shop.presentation.viewmodel.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe_shop.data.local.datastore.SessionManager
import com.ares.ewe_shop.data.remote.model.ShopOrderDto
import com.ares.ewe_shop.domain.repository.OrderRepository
import com.ares.ewe_shop.realtime.OrderRealtimeBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrderStats(
    val total: Int = 0,
    val pending: Int = 0,
    val preparing: Int = 0,
    val delivered: Int = 0,
)

data class OrdersUiState(
    val shopDisplayName: String? = null,
    val orders: List<ShopOrderDto> = emptyList(),
    val orderStats: OrderStats = OrderStats(),
    val selectedStatusFilter: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false
)

private fun computeOrderStats(orders: List<ShopOrderDto>): OrderStats = OrderStats(
    total = orders.size,
    pending = orders.count { it.status == "PENDING" },
    preparing = orders.count { it.status == "PREPARING" },
    delivered = orders.count { it.status == "DELIVERED" },
)

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val orderRealtimeBus: OrderRealtimeBus,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val name = sessionManager.shopName.first()
            _uiState.update { it.copy(shopDisplayName = name) }
        }
        loadOrders()
        viewModelScope.launch {
            orderRealtimeBus.refreshOrders.collect {
                refresh()
            }
        }
    }

    fun loadOrders() {
        viewModelScope.launch {
            val filter = _uiState.value.selectedStatusFilter
            val hasCachedOrders = _uiState.value.orders.isNotEmpty()
            _uiState.value = _uiState.value.copy(
                isLoading = !hasCachedOrders,
                isRefreshing = hasCachedOrders,
                errorMessage = null,
            )
            val ordersResult = orderRepository.getOrders(filter)
            val statsResult = orderRepository.getOrders(null)
            ordersResult
                .onSuccess { list ->
                    val stats = statsResult.getOrNull()?.let(::computeOrderStats)
                        ?: computeOrderStats(list)
                    _uiState.value = _uiState.value.copy(
                        orders = list,
                        orderStats = stats,
                        isLoading = false,
                        isRefreshing = false,
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = e.message ?: "Error al cargar los pedidos",
                        isLoading = false,
                        isRefreshing = false,
                    )
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val filter = _uiState.value.selectedStatusFilter
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            val ordersResult = orderRepository.getOrders(filter)
            val statsResult = orderRepository.getOrders(null)
            ordersResult
                .onSuccess { list ->
                    val stats = statsResult.getOrNull()?.let(::computeOrderStats)
                        ?: computeOrderStats(list)
                    _uiState.value = _uiState.value.copy(
                        orders = list,
                        orderStats = stats,
                        isRefreshing = false,
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isRefreshing = false)
                }
        }
    }

    fun setStatusFilter(status: String?) {
        _uiState.value = _uiState.value.copy(selectedStatusFilter = status)
        loadOrders()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
