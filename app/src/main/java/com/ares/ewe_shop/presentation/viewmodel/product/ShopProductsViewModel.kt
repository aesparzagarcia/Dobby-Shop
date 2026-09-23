package com.ares.ewe_shop.presentation.viewmodel.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe_shop.data.local.datastore.SessionManager
import com.ares.ewe_shop.data.remote.model.ShopProductDto
import com.ares.ewe_shop.domain.repository.ProductRepository
import com.ares.ewe_shop.domain.repository.ShopProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShopProductsUiState(
    val products: List<ShopProductDto> = emptyList(),
    val selectedCategoryId: String? = null,
    val showBestSellers: Boolean = false,
    val shopType: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false,
) {
    val isCarWash: Boolean
        get() = shopType.equals("CAR_WASH", ignoreCase = true)
}

@HiltViewModel
class ShopProductsViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val sessionManager: SessionManager,
    private val profileRepository: ShopProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShopProductsUiState())
    val uiState: StateFlow<ShopProductsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { resolveShopType() }
    }

    fun onAllSelected() {
        val state = _uiState.value
        if (!state.showBestSellers && state.selectedCategoryId == null) return
        _uiState.value = state.copy(
            selectedCategoryId = null,
            showBestSellers = false,
        )
    }

    fun onBestSellersSelected() {
        val state = _uiState.value
        if (state.showBestSellers && state.selectedCategoryId == null) return
        _uiState.value = state.copy(
            selectedCategoryId = null,
            showBestSellers = true,
        )
    }

    fun onCategorySelected(categoryId: String?) {
        if (categoryId == null) {
            onAllSelected()
            return
        }
        if (_uiState.value.isCarWash) return
        if (_uiState.value.selectedCategoryId == categoryId && !_uiState.value.showBestSellers) return
        _uiState.value = _uiState.value.copy(
            selectedCategoryId = categoryId,
            showBestSellers = false,
        )
    }

    fun loadProducts() {
        viewModelScope.launch {
            resolveShopType()
            _uiState.value = _uiState.value.copy(
                isLoading = _uiState.value.products.isEmpty(),
                errorMessage = null,
            )
            productRepository.getShopProducts(null)
                .onSuccess { list ->
                    _uiState.value = _uiState.value.copy(
                        products = list,
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = null,
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = e.message ?: "Error al cargar los productos",
                        isLoading = false,
                        isRefreshing = false,
                    )
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            resolveShopType()
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            productRepository.getShopProducts(null)
                .onSuccess { list ->
                    _uiState.value = _uiState.value.copy(
                        products = list,
                        isRefreshing = false,
                        errorMessage = null,
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        isLoading = false,
                        errorMessage = e.message ?: "Error al cargar los productos",
                    )
                }
        }
    }

    private suspend fun resolveShopType() {
        val cached = sessionManager.shopType.first()
        if (!cached.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(shopType = cached)
            return
        }
        profileRepository.getProfile()
            .onSuccess { profile ->
                val type = profile.type?.takeIf { it.isNotBlank() } ?: return@onSuccess
                sessionManager.updateShopType(type)
                _uiState.value = _uiState.value.copy(shopType = type)
            }
    }
}
