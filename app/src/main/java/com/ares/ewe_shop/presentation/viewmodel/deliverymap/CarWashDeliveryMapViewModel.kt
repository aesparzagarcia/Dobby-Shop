package com.ares.ewe_shop.presentation.viewmodel.deliverymap

import android.location.Location
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe_shop.data.location.LocationProvider
import com.ares.ewe_shop.domain.repository.DirectionsRepository
import com.ares.ewe_shop.domain.repository.DrivingRouteInfo
import com.ares.ewe_shop.domain.repository.OrderRepository
import com.ares.ewe_shop.realtime.OrderRealtimeBus
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

/** Radio en metros dentro del cual habilitamos "Ya llegué". */
private const val ARRIVAL_RADIUS_METERS = 150.0

/** Intervalo de sondeo del GPS para la cercanía al cliente. */
private const val LOCATION_POLL_MS = 5_000L

/** No recalcular Directions en cada poll de GPS. */
private const val ROUTE_REFRESH_MS = 30_000L

/** ~25 km/h promedio para un ETA aproximado si falla Directions. */
private const val ROUGH_SPEED_METERS_PER_MIN = 420.0

private const val MAX_ETA_MINUTES = 24 * 60

data class CarWashDeliveryMapUiState(
    val shopLatLng: LatLng? = null,
    val customerLatLng: LatLng? = null,
    val shopName: String? = null,
    val deliveryAddress: String? = null,
    val customerName: String? = null,
    val currentLocation: LatLng? = null,
    val routePoints: List<LatLng> = emptyList(),
    /** Texto de duración de Directions, p. ej. "23 min". */
    val etaText: String? = null,
    /** Distancia de la ruta, p. ej. "8,2 km". */
    val distanceText: String? = null,
    /** True cuando el ETA es estimado (Directions no disponible). */
    val etaIsApproximate: Boolean = false,
    val isLoading: Boolean = true,
    val isNearCustomer: Boolean = false,
    val hasMarkedArrived: Boolean = false,
    val isMarkingArrived: Boolean = false,
    val deliveryCodeInput: String = "",
    val deliveryCodeValid: Boolean? = null,
    val isVerifyingDeliveryCode: Boolean = false,
    val isMarkingDelivered: Boolean = false,
    val isDelivered: Boolean = false,
    /** OUT_FOR_PICKUP: yendo a recoger al cliente. */
    val isPickupTrip: Boolean = false,
    /** PICKED_UP: carro recogido, regresando al local. */
    val isReturnToShopTrip: Boolean = false,
    val isConfirmingPickup: Boolean = false,
    val isStartingWash: Boolean = false,
    /** API status; used to leave the map when another device advances the order. */
    val orderStatus: String? = null,
    val showCustomerRating: Boolean = false,
    val customerRatingStars: Int = 0,
    val customerPunctual: Boolean = false,
    val customerPaysWell: Boolean = false,
    val customerTipped: Boolean = false,
    val customerRecommended: Boolean = false,
    val isSubmittingCustomerRating: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class CarWashDeliveryMapViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val directionsRepository: DirectionsRepository,
    private val locationProvider: LocationProvider,
    orderRealtimeBus: OrderRealtimeBus,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val orderId: String = savedStateHandle.get<String>("orderId").orEmpty()

    private val _uiState = MutableStateFlow(CarWashDeliveryMapUiState())
    val uiState: StateFlow<CarWashDeliveryMapUiState> = _uiState.asStateFlow()

    private var locationPollJob: Job? = null
    private var verifyDeliveryCodeJob: Job? = null
    private var lastPostedEtaMinutes: Int? = null
    private var lastRouteFetchAt: Long = 0L
    private var pendingAfterRating: (() -> Unit)? = null

    init {
        loadData()
        viewModelScope.launch {
            orderRealtimeBus.refreshOrders.collect {
                loadData(preserveLocation = true)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationPollJob?.cancel()
        verifyDeliveryCodeJob?.cancel()
    }

    fun loadData(preserveLocation: Boolean = false) {
        if (orderId.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Pedido no válido",
            )
            return
        }
        viewModelScope.launch {
            if (!preserveLocation) {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            }
            val order = orderRepository.getOrderById(orderId).getOrElse { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Error al cargar el pedido",
                )
                return@launch
            }
            val shopLatLng = latLngOrNull(order.shopLat, order.shopLng)
            val customerLatLng = latLngOrNull(order.lat, order.lng)
            val previous = _uiState.value
            _uiState.value = previous.copy(
                shopLatLng = shopLatLng,
                customerLatLng = customerLatLng,
                shopName = order.shopName,
                deliveryAddress = order.deliveryAddress,
                customerName = listOfNotNull(
                    order.customerName?.trim()?.takeIf { it.isNotBlank() },
                    order.customerLastName?.trim()?.takeIf { it.isNotBlank() },
                ).joinToString(" ").takeIf { it.isNotBlank() },
                hasMarkedArrived = !order.arrivedAtCustomerAt.isNullOrBlank(),
                isDelivered = order.status == "DELIVERED",
                isPickupTrip = order.status == "OUT_FOR_PICKUP",
                isReturnToShopTrip = order.status == "PICKED_UP",
                orderStatus = order.status,
                isLoading = false,
                deliveryCodeInput = if (order.status == "PICKED_UP") "" else previous.deliveryCodeInput,
                deliveryCodeValid = if (order.status == "PICKED_UP") null else previous.deliveryCodeValid,
            )
            // Evita reenviar el ETA (y notificaciones) cada vez que se reabre el mapa.
            if (!preserveLocation || lastPostedEtaMinutes == null) {
                lastPostedEtaMinutes = order.estimatedDeliveryMinutes
            }
            if (order.status == "DELIVERED") {
                stopLocationTracking()
                return@launch
            }
            val routeDestination = when (order.status) {
                "PICKED_UP" -> shopLatLng
                else -> customerLatLng
            }
            val origin = previous.currentLocation ?: when (order.status) {
                "PICKED_UP" -> previous.currentLocation ?: customerLatLng ?: shopLatLng
                else -> shopLatLng
            }
            if (origin != null && routeDestination != null && !preserveLocation) {
                fetchRoute(origin = origin, destination = routeDestination, isPeriodicRefresh = false)
            } else if (preserveLocation && previous.currentLocation != null && routeDestination != null) {
                fetchRoute(
                    origin = previous.currentLocation!!,
                    destination = routeDestination,
                    isPeriodicRefresh = false,
                )
            }
        }
    }

    /** El mapa llama esto cuando el permiso de ubicación está concedido. */
    fun startLocationTracking() {
        if (locationPollJob?.isActive == true) return
        locationPollJob = viewModelScope.launch {
            while (isActive) {
                locationProvider.getCurrentLocation().onSuccess { latLng ->
                    val state = _uiState.value
                    val destination = when {
                        state.isReturnToShopTrip -> state.shopLatLng
                        else -> state.customerLatLng
                    }
                    val near = destination != null &&
                        distanceInMeters(latLng, destination) <= ARRIVAL_RADIUS_METERS
                    val hadLocation = state.currentLocation != null
                    _uiState.value = state.copy(
                        currentLocation = latLng,
                        isNearCustomer = near || state.isNearCustomer,
                    )
                    // Publica GPS para que el cliente vea el vehículo en Dobby.
                    if (!_uiState.value.isDelivered) {
                        orderRepository.updateCourierLocation(
                            orderId,
                            latLng.latitude,
                            latLng.longitude,
                        )
                    }
                    if (destination != null && !_uiState.value.isDelivered) {
                        fetchRoute(
                            origin = latLng,
                            destination = destination,
                            isPeriodicRefresh = hadLocation,
                        )
                    }
                }
                delay(LOCATION_POLL_MS)
            }
        }
    }

    fun stopLocationTracking() {
        locationPollJob?.cancel()
        locationPollJob = null
    }

    /** Driving route from the phone's current GPS to the customer. */
    private fun fetchRoute(origin: LatLng, destination: LatLng, isPeriodicRefresh: Boolean) {
        val now = System.currentTimeMillis()
        if (isPeriodicRefresh && now - lastRouteFetchAt < ROUTE_REFRESH_MS) return
        lastRouteFetchAt = now
        viewModelScope.launch {
            directionsRepository.getDrivingRoute(origin, destination)
                .onSuccess { info ->
                    val points = info.points.ifEmpty { listOf(origin, destination) }
                    _uiState.value = _uiState.value.copy(
                        routePoints = ensureRouteStartsAt(origin, points),
                        etaText = info.durationText,
                        distanceText = info.distanceText,
                        etaIsApproximate = false,
                    )
                    pushEtaMinutes(etaMinutesFromRoute(info, origin, destination))
                }
                .onFailure {
                    val meters = distanceInMeters(origin, destination)
                    val minutes = roughEtaMinutes(meters)
                    _uiState.value = _uiState.value.copy(
                        routePoints = listOf(origin, destination),
                        etaText = "~$minutes min",
                        distanceText = formatDistance(meters),
                        etaIsApproximate = true,
                    )
                    pushEtaMinutes(minutes)
                }
        }
    }

    private fun ensureRouteStartsAt(origin: LatLng, points: List<LatLng>): List<LatLng> {
        val first = points.firstOrNull() ?: return listOf(origin)
        val sameStart =
            abs(first.latitude - origin.latitude) < 1e-5 &&
                abs(first.longitude - origin.longitude) < 1e-5
        return if (sameStart) points else listOf(origin) + points
    }

    private fun etaMinutesFromRoute(info: DrivingRouteInfo, origin: LatLng, destination: LatLng): Int {
        val seconds = info.durationSeconds
        if (seconds != null && seconds > 0) {
            return ((seconds + 59) / 60).coerceIn(1, MAX_ETA_MINUTES)
        }
        val meters = info.distanceMeters?.takeIf { it > 0 }?.toDouble()
            ?: distanceInMeters(origin, destination)
        return roughEtaMinutes(meters)
    }

    private fun roughEtaMinutes(meters: Double): Int =
        (meters / ROUGH_SPEED_METERS_PER_MIN).roundToInt().coerceIn(1, MAX_ETA_MINUTES)

    private fun pushEtaMinutes(minutes: Int) {
        if (orderId.isBlank() || lastPostedEtaMinutes == minutes) return
        lastPostedEtaMinutes = minutes
        viewModelScope.launch {
            orderRepository.updateDeliveryEta(orderId, minutes)
        }
    }

    fun markArrived() {
        val state = _uiState.value
        if (orderId.isBlank() || state.isMarkingArrived || state.hasMarkedArrived) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isMarkingArrived = true, errorMessage = null)
            orderRepository.markArrivedAtCustomer(orderId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isMarkingArrived = false,
                        hasMarkedArrived = true,
                        deliveryCodeInput = "",
                        deliveryCodeValid = null,
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isMarkingArrived = false,
                        errorMessage = e.message ?: "Error al registrar la llegada",
                    )
                }
        }
    }

    fun onDeliveryCodeChange(raw: String) {
        val digits = raw.filter { it.isDigit() }.take(6)
        _uiState.value = _uiState.value.copy(
            deliveryCodeInput = digits,
            deliveryCodeValid = if (digits.length == 6) null else false,
        )
        verifyDeliveryCodeJob?.cancel()
        if (digits.length != 6 || orderId.isBlank()) return
        verifyDeliveryCodeJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isVerifyingDeliveryCode = true)
            orderRepository.verifyDeliveryCode(orderId, digits)
                .onSuccess { valid ->
                    _uiState.value = _uiState.value.copy(
                        isVerifyingDeliveryCode = false,
                        deliveryCodeValid = valid,
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isVerifyingDeliveryCode = false,
                        deliveryCodeValid = false,
                    )
                }
        }
    }

    fun markDelivered(onSuccess: () -> Unit) {
        if (_uiState.value.isPickupTrip) {
            confirmPickup(onSuccess = null)
            return
        }
        if (_uiState.value.isReturnToShopTrip) {
            startWash(onSuccess)
            return
        }
        val state = _uiState.value
        val code = state.deliveryCodeInput
        if (orderId.isBlank() ||
            !state.hasMarkedArrived ||
            code.length != 6 ||
            state.deliveryCodeValid != true ||
            state.isMarkingDelivered
        ) {
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isMarkingDelivered = true, errorMessage = null)
            orderRepository.markOrderDelivered(orderId, code)
                .onSuccess {
                    stopLocationTracking()
                    pendingAfterRating = onSuccess
                    _uiState.value = _uiState.value.copy(
                        isMarkingDelivered = false,
                        isDelivered = true,
                        showCustomerRating = true,
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isMarkingDelivered = false,
                        errorMessage = e.message ?: "Error al marcar como entregado",
                    )
                }
        }
    }

    /** PIN en domicilio: OUT_FOR_PICKUP → PICKED_UP (tracking al autolavado). */
    fun confirmPickup(onSuccess: (() -> Unit)?) {
        val state = _uiState.value
        val code = state.deliveryCodeInput
        if (orderId.isBlank() ||
            !state.hasMarkedArrived ||
            !state.isPickupTrip ||
            code.length != 6 ||
            state.deliveryCodeValid != true ||
            state.isConfirmingPickup
        ) {
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isConfirmingPickup = true, errorMessage = null)
            orderRepository.markOrderConfirmPickup(orderId, code)
                .onSuccess {
                    lastRouteFetchAt = 0L
                    _uiState.value = _uiState.value.copy(
                        isConfirmingPickup = false,
                        isPickupTrip = false,
                        isReturnToShopTrip = true,
                        orderStatus = "PICKED_UP",
                        hasMarkedArrived = false,
                        isNearCustomer = false,
                        deliveryCodeInput = "",
                        deliveryCodeValid = null,
                    )
                    // Continúa en el mapa hacia el local.
                    val shop = _uiState.value.shopLatLng
                    val current = _uiState.value.currentLocation
                    if (shop != null && current != null) {
                        fetchRoute(origin = current, destination = shop, isPeriodicRefresh = false)
                    }
                    onSuccess?.invoke()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isConfirmingPickup = false,
                        errorMessage = e.message ?: "Error al confirmar la recolección",
                    )
                }
        }
    }

    /** Al llegar al local: PICKED_UP → PREPARING. */
    fun startWash(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (orderId.isBlank() ||
            !state.hasMarkedArrived ||
            !state.isReturnToShopTrip ||
            state.isStartingWash
        ) {
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isStartingWash = true, errorMessage = null)
            orderRepository.markOrderStartWash(orderId)
                .onSuccess {
                    stopLocationTracking()
                    _uiState.value = _uiState.value.copy(
                        isStartingWash = false,
                        isReturnToShopTrip = false,
                        orderStatus = "PREPARING",
                    )
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isStartingWash = false,
                        errorMessage = e.message ?: "Error al iniciar el lavado",
                    )
                }
        }
    }

    fun setCustomerRatingStars(stars: Int) {
        val next = if (_uiState.value.customerRatingStars == stars) 0 else stars.coerceIn(0, 5)
        _uiState.value = _uiState.value.copy(customerRatingStars = next)
    }

    fun toggleCustomerPunctual() {
        _uiState.value = _uiState.value.copy(customerPunctual = !_uiState.value.customerPunctual)
    }

    fun toggleCustomerPaysWell() {
        _uiState.value = _uiState.value.copy(customerPaysWell = !_uiState.value.customerPaysWell)
    }

    fun toggleCustomerTipped() {
        _uiState.value = _uiState.value.copy(customerTipped = !_uiState.value.customerTipped)
    }

    fun toggleCustomerRecommended() {
        _uiState.value = _uiState.value.copy(customerRecommended = !_uiState.value.customerRecommended)
    }

    fun skipCustomerRating() {
        finishCustomerRatingFlow()
    }

    fun submitCustomerRating() {
        if (orderId.isBlank() || _uiState.value.isSubmittingCustomerRating) return
        val s = _uiState.value
        val stars = s.customerRatingStars.takeIf { it in 1..5 }
        val punctual = s.customerPunctual.takeIf { it }
        val paysWell = s.customerPaysWell.takeIf { it }
        val tipped = s.customerTipped.takeIf { it }
        val recommended = s.customerRecommended.takeIf { it }
        val hasAny =
            stars != null || punctual == true || paysWell == true || tipped == true || recommended == true
        if (!hasAny) {
            finishCustomerRatingFlow()
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingCustomerRating = true, errorMessage = null)
            orderRepository.rateCustomer(
                orderId = orderId,
                stars = stars,
                punctual = punctual,
                paysWell = paysWell,
                tipped = tipped,
                recommended = recommended,
            ).onSuccess {
                _uiState.value = _uiState.value.copy(isSubmittingCustomerRating = false)
                finishCustomerRatingFlow()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isSubmittingCustomerRating = false,
                    errorMessage = e.message ?: "Error al guardar la calificación",
                )
            }
        }
    }

    private fun finishCustomerRatingFlow() {
        _uiState.value = _uiState.value.copy(showCustomerRating = false)
        val done = pendingAfterRating
        pendingAfterRating = null
        done?.invoke()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun latLngOrNull(lat: Double?, lng: Double?): LatLng? =
        if (lat != null && lng != null) LatLng(lat, lng) else null

    private fun distanceInMeters(a: LatLng, b: LatLng): Double {
        val results = FloatArray(1)
        Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, results)
        return results[0].toDouble()
    }

    private fun formatDistance(meters: Double): String =
        if (meters >= 1000) {
            String.format(Locale.getDefault(), "%.1f km", meters / 1000.0)
        } else {
            String.format(Locale.getDefault(), "%.0f m", meters)
        }
}
