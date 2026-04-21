package com.ares.ewe_shop.presentation.viewmodel.product

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe_shop.data.local.datastore.SessionManager
import com.ares.ewe_shop.data.remote.model.CreateShopProductRequest
import com.ares.ewe_shop.data.remote.model.ShopProductDto
import com.ares.ewe_shop.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MAX_PHOTOS = 3

private const val KEY_DRAFT_NAME = "draft_product_name"
private const val KEY_DRAFT_DESCRIPTION = "draft_product_description"
private const val KEY_DRAFT_PRICE = "draft_product_price"
private const val KEY_DRAFT_HAS_PROMO = "draft_product_has_promo"
private const val KEY_DRAFT_DISCOUNT = "draft_product_discount"
private const val KEY_DRAFT_ACTIVE = "draft_product_active"
private const val KEY_DRAFT_IMAGE_URLS = "draft_product_image_urls"

data class CreateProductUiState(
    val shopDisplayName: String? = null,
    /** Si no es null, estamos editando un producto existente. */
    val editingProductId: String? = null,
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
    private val sessionManager: SessionManager,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(restoreDraftFromSavedState())
    val uiState: StateFlow<CreateProductUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val name = sessionManager.shopName.first()
            _uiState.update { it.copy(shopDisplayName = name) }
        }
    }

    private fun restoreDraftFromSavedState(): CreateProductUiState {
        @Suppress("UNCHECKED_CAST")
        val urls = (savedStateHandle.get<ArrayList<String>>(KEY_DRAFT_IMAGE_URLS) ?: arrayListOf()).toList()
        return CreateProductUiState(
            name = savedStateHandle[KEY_DRAFT_NAME] ?: "",
            description = savedStateHandle[KEY_DRAFT_DESCRIPTION] ?: "",
            priceText = savedStateHandle[KEY_DRAFT_PRICE] ?: "",
            hasPromotion = savedStateHandle[KEY_DRAFT_HAS_PROMO] ?: false,
            discountText = savedStateHandle[KEY_DRAFT_DISCOUNT] ?: "0",
            isActive = savedStateHandle[KEY_DRAFT_ACTIVE] ?: true,
            imageUrls = urls,
        )
    }

    /** Formulario nuevo: restaura el borrador persistido (no borrar SavedState antes de leer). */
    fun prepareNewProduct() {
        val shopName = _uiState.value.shopDisplayName
        _uiState.value = restoreDraftFromSavedState().copy(
            shopDisplayName = shopName,
            editingProductId = null,
            errorMessage = null,
            successMessage = null,
            isSubmitting = false,
            isUploadingImage = false,
        )
    }

    /** Rellena el formulario para editar un producto de la lista. */
    fun loadForEdit(product: ShopProductDto) {
        clearSavedDraft()
        val shopName = _uiState.value.shopDisplayName
        _uiState.value = CreateProductUiState(
            shopDisplayName = shopName,
            editingProductId = product.id,
            name = product.name,
            description = product.description ?: "",
            priceText = formatPriceForField(product.price),
            hasPromotion = product.hasPromotion,
            discountText = product.discount.coerceIn(0, 100).toString(),
            isActive = product.isActive,
            imageUrls = product.imageUrls,
            errorMessage = null,
            successMessage = null,
            isSubmitting = false,
            isUploadingImage = false,
        )
    }

    private fun formatPriceForField(price: Double): String {
        val s = String.format(Locale.US, "%.2f", price)
        return if (s.endsWith(".00")) s.dropLast(3) else s.trimEnd('0').trimEnd('.')
    }

    /** Sobrevive a que el sistema mate el proceso mientras la app está en segundo plano (solo borrador de producto nuevo). */
    private fun persistDraftFields(s: CreateProductUiState) {
        savedStateHandle[KEY_DRAFT_NAME] = s.name
        savedStateHandle[KEY_DRAFT_DESCRIPTION] = s.description
        savedStateHandle[KEY_DRAFT_PRICE] = s.priceText
        savedStateHandle[KEY_DRAFT_HAS_PROMO] = s.hasPromotion
        savedStateHandle[KEY_DRAFT_DISCOUNT] = s.discountText
        savedStateHandle[KEY_DRAFT_ACTIVE] = s.isActive
        savedStateHandle[KEY_DRAFT_IMAGE_URLS] = ArrayList(s.imageUrls)
    }

    private fun clearSavedDraft() {
        savedStateHandle.remove<String>(KEY_DRAFT_NAME)
        savedStateHandle.remove<String>(KEY_DRAFT_DESCRIPTION)
        savedStateHandle.remove<String>(KEY_DRAFT_PRICE)
        savedStateHandle.remove<Boolean>(KEY_DRAFT_HAS_PROMO)
        savedStateHandle.remove<String>(KEY_DRAFT_DISCOUNT)
        savedStateHandle.remove<Boolean>(KEY_DRAFT_ACTIVE)
        savedStateHandle.remove<ArrayList<String>>(KEY_DRAFT_IMAGE_URLS)
    }

    private fun persistDraftIfNeeded(s: CreateProductUiState) {
        if (s.editingProductId == null) {
            persistDraftFields(s)
        }
    }

    private inline fun updateDraft(crossinline block: (CreateProductUiState) -> CreateProductUiState) {
        _uiState.update { old ->
            val next = block(old)
            persistDraftIfNeeded(next)
            next
        }
    }

    fun onNameChange(v: String) = updateDraft { it.copy(name = v) }
    fun onDescriptionChange(v: String) = updateDraft { it.copy(description = v) }
    fun onPriceChange(v: String) = updateDraft { it.copy(priceText = v) }
    fun onHasPromotionChange(v: Boolean) =
        updateDraft { it.copy(hasPromotion = v, discountText = if (v) it.discountText else "0") }
    fun onDiscountChange(v: String) = updateDraft { it.copy(discountText = v.filter { ch -> ch.isDigit() }) }
    fun onIsActiveChange(v: Boolean) = updateDraft { it.copy(isActive = v) }

    fun removeImageAt(index: Int) {
        updateDraft { s ->
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
                        val next = s.copy(
                            imageUrls = (s.imageUrls + url).take(MAX_PHOTOS),
                            isUploadingImage = false
                        )
                        persistDraftIfNeeded(next)
                        next
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
        val editingId = s.editingProductId
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }
            if (editingId != null) {
                productRepository.updateProduct(editingId, body)
                    .onSuccess { updated ->
                        clearSavedDraft()
                        _uiState.update {
                            CreateProductUiState(
                                shopDisplayName = it.shopDisplayName,
                                successMessage = "«${updated.name}» guardado"
                            )
                        }
                    }
                    .onFailure { e ->
                        _uiState.update {
                            it.copy(isSubmitting = false, errorMessage = e.message ?: "Error al guardar")
                        }
                    }
            } else {
                productRepository.createProduct(body)
                    .onSuccess { created ->
                        clearSavedDraft()
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
}
