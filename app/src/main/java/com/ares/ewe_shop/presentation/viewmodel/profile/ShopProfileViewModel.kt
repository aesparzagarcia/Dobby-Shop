package com.ares.ewe_shop.presentation.viewmodel.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe_shop.data.local.datastore.SessionManager
import com.ares.ewe_shop.data.remote.model.ShopEarningsDto
import com.ares.ewe_shop.data.remote.model.ShopProfileDto
import com.ares.ewe_shop.domain.repository.ShopProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

enum class ShopEarningsPeriod { TODAY, WEEK, RANGE }

data class ShopProfileUiState(
    val profile: ShopProfileDto? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val earnings: ShopEarningsDto? = null,
    val earningsLoading: Boolean = false,
    val earningsError: String? = null,
    val earningsPeriod: ShopEarningsPeriod = ShopEarningsPeriod.TODAY,
    val earningsFrom: String = "",
    val earningsTo: String = "",
)

@HiltViewModel
class ShopProfileViewModel @Inject constructor(
    private val shopProfileRepository: ShopProfileRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShopProfileUiState())
    val uiState: StateFlow<ShopProfileUiState> = _uiState.asStateFlow()
    private var earningsRequest = 0

    fun loadProfile() {
        loadEarnings()
        viewModelScope.launch {
            val hadProfile = _uiState.value.profile != null
            _uiState.value = _uiState.value.copy(
                isLoading = !hadProfile,
                errorMessage = null,
            )
            try {
                val result = withTimeout(PROFILE_LOAD_TIMEOUT_MS) {
                    shopProfileRepository.getProfile()
                }
                result.fold(
                    onSuccess = { dto ->
                        dto.type?.takeIf { it.isNotBlank() }?.let { sessionManager.updateShopType(it) }
                        _uiState.value = _uiState.value.copy(
                            profile = dto,
                            isLoading = false,
                            errorMessage = null,
                        )
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = e.message?.takeIf { it.isNotBlank() }
                                ?: "No se pudo cargar el perfil. Intenta de nuevo.",
                        )
                    },
                )
            } catch (_: TimeoutCancellationException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Tiempo de espera agotado. El servidor no respondió. Intenta de nuevo.",
                )
            }
        }
    }

    fun selectEarningsPeriod(period: ShopEarningsPeriod) {
        val today = LocalDate.now(MEXICO)
        val (from, to) = when (period) {
            ShopEarningsPeriod.TODAY -> today to today
            ShopEarningsPeriod.WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) to
                today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            ShopEarningsPeriod.RANGE -> {
                val currentFrom = _uiState.value.earningsFrom.takeIf { it.isNotBlank() } ?: today.toString()
                val currentTo = _uiState.value.earningsTo.takeIf { it.isNotBlank() } ?: today.toString()
                LocalDate.parse(currentFrom) to LocalDate.parse(currentTo)
            }
        }
        _uiState.value = _uiState.value.copy(
            earningsPeriod = period,
            earningsFrom = from.toString(),
            earningsTo = to.toString(),
        )
        loadEarnings()
    }

    fun setEarningsRange(from: LocalDate, to: LocalDate) {
        val start = if (from.isAfter(to)) to else from
        val end = if (from.isAfter(to)) from else to
        _uiState.value = _uiState.value.copy(
            earningsPeriod = ShopEarningsPeriod.RANGE,
            earningsFrom = start.toString(),
            earningsTo = end.toString(),
        )
        loadEarnings()
    }

    fun loadEarnings() {
        val state = _uiState.value
        val today = LocalDate.now(MEXICO)
        val (from, to) = when (state.earningsPeriod) {
            ShopEarningsPeriod.TODAY -> today.toString() to today.toString()
            ShopEarningsPeriod.WEEK -> {
                val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val end = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                start.toString() to end.toString()
            }
            ShopEarningsPeriod.RANGE -> {
                val start = state.earningsFrom.ifBlank { today.toString() }
                val end = state.earningsTo.ifBlank { today.toString() }
                start to end
            }
        }
        val request = ++earningsRequest
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                earningsLoading = true,
                earningsError = null,
                earningsFrom = from,
                earningsTo = to,
            )
            val result = shopProfileRepository.getEarnings(from, to)
            if (request != earningsRequest) return@launch
            result.fold(
                onSuccess = { dto ->
                    _uiState.value = _uiState.value.copy(
                        earnings = dto,
                        earningsLoading = false,
                        earningsError = null,
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        earningsLoading = false,
                        earningsError = e.message?.takeIf { it.isNotBlank() }
                            ?: "No se pudieron cargar las ganancias",
                    )
                },
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private companion object {
        const val PROFILE_LOAD_TIMEOUT_MS = 15_000L
        val MEXICO: ZoneId = ZoneId.of("America/Mexico_City")
    }
}
