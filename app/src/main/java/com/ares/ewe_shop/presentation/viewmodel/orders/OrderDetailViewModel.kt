package com.ares.ewe_shop.presentation.viewmodel.orders

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrderDetailUiState(
    val order: ShopOrderDto? = null,
    val isCarWash: Boolean = false,
    val isLoading: Boolean = false,
    val isAccepting: Boolean = false,
    val isPreparing: Boolean = false,
    val isReadyForPickup: Boolean = false,
    val isDetailing: Boolean = false,
    val isMarkingOnDelivery: Boolean = false,
    val isRejecting: Boolean = false,
    val errorMessage: String? = null,
    val actionSuccess: Boolean = false
)

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val sessionManager: SessionManager,
    orderRealtimeBus: OrderRealtimeBus,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val orderId: String = checkNotNull(savedStateHandle.get<String>("orderId"))

    private val _uiState = MutableStateFlow(OrderDetailUiState())
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val type = sessionManager.shopType.first()
            _uiState.value = _uiState.value.copy(
                isCarWash = type.equals("CAR_WASH", ignoreCase = true),
            )
        }
        viewModelScope.launch {
            orderRealtimeBus.refreshOrders.collect {
                loadOrder(null)
            }
        }
    }

    fun loadOrder(ordersFromList: List<ShopOrderDto>?) {
        val order = ordersFromList?.firstOrNull { it.id == orderId }
        if (order != null) {
            _uiState.value = _uiState.value.copy(order = order)
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            orderRepository.getOrders(null)
                .onSuccess { list ->
                    val o = list.firstOrNull { it.id == orderId }
                    _uiState.value = _uiState.value.copy(order = o, isLoading = false)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = e.message ?: "Error al cargar el pedido",
                        isLoading = false
                    )
                }
        }
    }

    fun acceptOrder(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAccepting = true, errorMessage = null)
            orderRepository.acceptOrder(orderId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        order = _uiState.value.order?.copy(status = "CONFIRMED"),
                        isAccepting = false,
                        actionSuccess = true
                    )
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isAccepting = false,
                        errorMessage = e.message ?: "Error al aceptar el pedido"
                    )
                }
        }
    }

    fun markPreparing(estimatedPreparationMinutes: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPreparing = true, errorMessage = null)
            orderRepository.markOrderPreparing(orderId, estimatedPreparationMinutes)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        order = _uiState.value.order?.copy(
                            status = "PREPARING",
                            estimatedPreparationMinutes = estimatedPreparationMinutes,
                        ),
                        isPreparing = false,
                        actionSuccess = true,
                    )
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isPreparing = false,
                        errorMessage = e.message ?: "Error al marcar en preparación",
                    )
                }
        }
    }

    /** Carwash: CONFIRMED → OUT_FOR_PICKUP con tiempo estimado para recoger el carro. */
    fun markOutForPickup(estimatedPreparationMinutes: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPreparing = true, errorMessage = null)
            orderRepository.markOrderOutForPickup(orderId, estimatedPreparationMinutes)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        order = _uiState.value.order?.copy(
                            status = "OUT_FOR_PICKUP",
                            estimatedPreparationMinutes = estimatedPreparationMinutes,
                        ),
                        isPreparing = false,
                        actionSuccess = true,
                    )
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isPreparing = false,
                        errorMessage = e.message ?: "Error al salir a recoger el carro",
                    )
                }
        }
    }

    fun markReadyForPickup(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isReadyForPickup = true, errorMessage = null)
            orderRepository.markOrderReadyForPickup(orderId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        order = _uiState.value.order?.copy(status = "READY_FOR_PICKUP"),
                        isReadyForPickup = false,
                    )
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isReadyForPickup = false,
                        errorMessage = e.message ?: "Error al marcar listo para recoger"
                    )
                }
        }
    }

    fun markDetailing(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDetailing = true, errorMessage = null)
            orderRepository.markOrderDetailing(orderId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        order = _uiState.value.order?.copy(status = "ASSIGNED"),
                        isDetailing = false,
                        actionSuccess = true,
                    )
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isDetailing = false,
                        errorMessage = e.message ?: "Error al marcar Detallado",
                    )
                }
        }
    }

    fun markOnDelivery(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isMarkingOnDelivery = true, errorMessage = null)
            orderRepository.markOrderOnDelivery(orderId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        order = _uiState.value.order?.copy(status = "ON_DELIVERY"),
                        isMarkingOnDelivery = false,
                        actionSuccess = true,
                    )
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isMarkingOnDelivery = false,
                        errorMessage = e.message ?: "Error al marcar En camino",
                    )
                }
        }
    }

    fun rejectOrder(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRejecting = true, errorMessage = null)
            orderRepository.rejectOrder(orderId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        order = _uiState.value.order?.copy(status = "CANCELLED"),
                        isRejecting = false,
                        actionSuccess = true
                    )
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isRejecting = false,
                        errorMessage = e.message ?: "Error al rechazar el pedido"
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
