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

    fun onCategorySelected(categoryId: String?) {
        if (_uiState.value.isCarWash) return
        if (_uiState.value.selectedCategoryId == categoryId) return
        _uiState.value = _uiState.value.copy(
            selectedCategoryId = categoryId,
            isLoading = true,
            errorMessage = null,
        )
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            resolveShopType()
            val category = if (_uiState.value.isCarWash) null else _uiState.value.selectedCategoryId
            _uiState.value = _uiState.value.copy(
                selectedCategoryId = category,
                isLoading = true,
                errorMessage = null,
            )
            productRepository.getShopProducts(category)
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
            val category = if (_uiState.value.isCarWash) null else _uiState.value.selectedCategoryId
            _uiState.value = _uiState.value.copy(
                selectedCategoryId = category,
                isRefreshing = true,
            )
            productRepository.getShopProducts(category)
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
