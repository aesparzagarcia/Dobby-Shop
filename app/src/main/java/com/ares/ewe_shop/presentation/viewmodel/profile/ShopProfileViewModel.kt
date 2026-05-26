package com.ares.ewe_shop.presentation.viewmodel.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe_shop.data.remote.model.ShopProfileDto
import com.ares.ewe_shop.domain.repository.ShopProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShopProfileUiState(
    val profile: ShopProfileDto? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class ShopProfileViewModel @Inject constructor(
    private val shopProfileRepository: ShopProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShopProfileUiState())
    val uiState: StateFlow<ShopProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            val hadProfile = _uiState.value.profile != null
            _uiState.value = _uiState.value.copy(
                isLoading = !hadProfile,
                errorMessage = null,
            )
            shopProfileRepository.getProfile().fold(
                onSuccess = { dto ->
                    _uiState.value = ShopProfileUiState(profile = dto, isLoading = false, errorMessage = null)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Error al cargar el perfil",
                    )
                },
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
