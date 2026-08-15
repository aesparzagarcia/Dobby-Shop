package com.ares.ewe_shop.presentation.viewmodel.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe_shop.data.local.datastore.SessionManager
import com.ares.ewe_shop.domain.repository.AuthRepository
import com.ares.ewe_shop.domain.repository.ShopProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val shopType: String? = null,
) {
    val isCarWash: Boolean
        get() = shopType.equals("CAR_WASH", ignoreCase = true)
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val profileRepository: ShopProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { resolveShopType() }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }

    private suspend fun resolveShopType() {
        val cached = sessionManager.shopType.first()
        if (!cached.isNullOrBlank()) {
            _uiState.value = MainUiState(shopType = cached)
            return
        }
        profileRepository.getProfile()
            .onSuccess { profile ->
                val type = profile.type?.takeIf { it.isNotBlank() } ?: return@onSuccess
                sessionManager.updateShopType(type)
                _uiState.value = MainUiState(shopType = type)
            }
    }
}
