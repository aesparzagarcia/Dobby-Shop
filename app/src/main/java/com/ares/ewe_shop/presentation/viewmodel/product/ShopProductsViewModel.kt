package com.ares.ewe_shop.presentation.viewmodel.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe_shop.data.remote.model.ShopProductDto
import com.ares.ewe_shop.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShopProductsUiState(
    val products: List<ShopProductDto> = emptyList(),
    val selectedCategoryId: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false,
)

@HiltViewModel
class ShopProductsViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShopProductsUiState())
    val uiState: StateFlow<ShopProductsUiState> = _uiState.asStateFlow()

    fun onCategorySelected(categoryId: String?) {
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
            val category = _uiState.value.selectedCategoryId
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
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
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            productRepository.getShopProducts(_uiState.value.selectedCategoryId)
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
}
