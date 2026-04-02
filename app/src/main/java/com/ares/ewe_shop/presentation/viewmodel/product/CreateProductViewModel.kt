package com.ares.ewe_shop.presentation.viewmodel.product

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe_shop.data.local.datastore.SessionManager
import com.ares.ewe_shop.data.remote.model.CreateShopProductRequest
import com.ares.ewe_shop.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MAX_PHOTOS = 3

data class CreateProductUiState(
    val shopDisplayName: String? = null,
    val name: String = "",
    val description: String = "",
    val priceText: String = "",
    val hasPromotion: Boolean = false,
    val discountText: String = "0",
    val isActive: Boolean = true,
    val imageUrls: List<String> = emptyList(),
    val isSubmitting: Boolean = false,
    val isUploadingImage: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class CreateProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateProductUiState())
    val uiState: StateFlow<CreateProductUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val name = sessionManager.shopName.first()
            _uiState.update { it.copy(shopDisplayName = name) }
        }
    }

    fun onNameChange(v: String) = _uiState.update { it.copy(name = v) }
    fun onDescriptionChange(v: String) = _uiState.update { it.copy(description = v) }
    fun onPriceChange(v: String) = _uiState.update { it.copy(priceText = v) }
    fun onHasPromotionChange(v: Boolean) =
        _uiState.update { it.copy(hasPromotion = v, discountText = if (v) it.discountText else "0") }
    fun onDiscountChange(v: String) = _uiState.update { it.copy(discountText = v.filter { ch -> ch.isDigit() }) }
    fun onIsActiveChange(v: Boolean) = _uiState.update { it.copy(isActive = v) }

    fun removeImageAt(index: Int) {
        _uiState.update { s ->
            s.copy(imageUrls = s.imageUrls.filterIndexed { i, _ -> i != index })
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
    fun clearSuccess() = _uiState.update { it.copy(successMessage = null) }

    fun addImageFromUri(uri: Uri) {
        if (_uiState.value.imageUrls.size >= MAX_PHOTOS) return
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingImage = true, errorMessage = null) }
            productRepository.uploadProductImage(uri)
                .onSuccess { url ->
                    _uiState.update { s ->
                        s.copy(
                            imageUrls = (s.imageUrls + url).take(MAX_PHOTOS),
                            isUploadingImage = false
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isUploadingImage = false,
                            errorMessage = e.message ?: "Error al subir la imagen"
                        )
                    }
                }
        }
    }

    fun submit() {
        val s = _uiState.value
        val name = s.name.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Ingresa el nombre del producto") }
            return
        }
        val price = s.priceText.replace(",", ".").trim().toDoubleOrNull()
        if (price == null || price < 0) {
            _uiState.update { it.copy(errorMessage = "Precio inválido") }
            return
        }
        val discount = s.discountText.toIntOrNull() ?: 0
        if (s.hasPromotion && (discount < 0 || discount > 100)) {
            _uiState.update { it.copy(errorMessage = "El descuento debe estar entre 0 y 100") }
            return
        }
        val body = CreateShopProductRequest(
            name = name,
            description = s.description.trim().ifEmpty { null },
            price = price,
            imageUrls = s.imageUrls,
            hasPromotion = s.hasPromotion,
            discount = if (s.hasPromotion) discount else 0,
            isActive = s.isActive
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }
            productRepository.createProduct(body)
                .onSuccess { created ->
                    _uiState.update {
                        CreateProductUiState(
                            shopDisplayName = it.shopDisplayName,
                            successMessage = "«${created.name}» publicado en tu tienda"
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isSubmitting = false, errorMessage = e.message ?: "Error al crear")
                    }
                }
        }
    }
}
