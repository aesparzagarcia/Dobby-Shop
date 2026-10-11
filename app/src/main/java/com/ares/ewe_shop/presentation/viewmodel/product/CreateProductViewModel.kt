package com.ares.ewe_shop.presentation.viewmodel.product

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe_shop.data.local.datastore.SessionManager
import com.ares.ewe_shop.data.remote.model.CreateShopProductRequest
import com.ares.ewe_shop.data.remote.model.ProductVariantInput
import com.ares.ewe_shop.data.remote.model.ShopProductDto
import com.ares.ewe_shop.domain.model.ShopProductCategory
import com.ares.ewe_shop.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MAX_PHOTOS = 5
private const val MAX_DESCRIPTION_LENGTH = 200
private const val DEFAULT_VARIANT_NAME = "Única"
const val SALE_TYPE_UNIQUE = "unique"
const val SALE_TYPE_VARIANTS = "variants"

private const val KEY_DRAFT_NAME = "draft_product_name"
private const val KEY_DRAFT_DESCRIPTION = "draft_product_description"
private const val KEY_DRAFT_PRICE = "draft_product_price"
private const val KEY_DRAFT_SALE_TYPE = "draft_product_sale_type"
private const val KEY_DRAFT_VARIANT_NAMES = "draft_product_variant_names"
private const val KEY_DRAFT_VARIANT_PRICES = "draft_product_variant_prices"
private const val KEY_DRAFT_HAS_PROMO = "draft_product_has_promo"
private const val KEY_DRAFT_DISCOUNT = "draft_product_discount"
private const val KEY_DRAFT_ACTIVE = "draft_product_active"
private const val KEY_DRAFT_CATEGORY = "draft_product_category"
private const val KEY_DRAFT_IMAGE_URLS = "draft_product_image_urls"

data class ProductVariantRow(
    val name: String = "",
    val priceText: String = "",
)

data class CreateProductUiState(
    val shopDisplayName: String? = null,
    val shopType: String? = null,
    /** Si no es null, estamos editando un producto existente. */
    val editingProductId: String? = null,
    val name: String = "",
    val description: String = "",
    val priceText: String = "",
    val saleType: String = SALE_TYPE_UNIQUE,
    val variantRows: List<ProductVariantRow> = listOf(ProductVariantRow()),
    val hasPromotion: Boolean = false,
    val discountText: String = "0",
    val isActive: Boolean = false,
    val canActivate: Boolean = false,
    val moderationStatus: String? = null,
    val category: String = "",
    val imageUrls: List<String> = emptyList(),
    val isSubmitting: Boolean = false,
    val isUploadingImage: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
) {
    val isRestaurant: Boolean
        get() = shopType.equals("RESTAURANT", ignoreCase = true)

    val parsedVariantRows: List<Pair<String, Double>>
        get() {
            val rows = mutableListOf<Pair<String, Double>>()
            val seen = mutableSetOf<String>()
            for (row in variantRows) {
                val n = row.name.trim()
                val rawPrice = row.priceText.replace(",", ".").trim()
                if (n.isEmpty() && rawPrice.isEmpty()) continue
                val price = rawPrice.toDoubleOrNull()
                if (n.isEmpty() || price == null || price < 0.0) return emptyList()
                val key = n.lowercase(Locale.ROOT)
                if (!seen.add(key)) return emptyList()
                rows.add(n to price)
            }
            return rows
        }

    val canSubmit: Boolean
        get() {
            val nameOk = name.trim().isNotEmpty()
            val descriptionOk = description.trim().isNotEmpty()
            val priceOk = if (isRestaurant && saleType == SALE_TYPE_VARIANTS) {
                parsedVariantRows.isNotEmpty()
            } else {
                val price = priceText.replace(",", ".").trim().toDoubleOrNull()
                price != null && price >= 0.0
            }
            val categoryOk = ShopProductCategory.isValidForShopType(category, shopType)
            val imageOk = imageUrls.any { it.isNotBlank() }
            return nameOk && descriptionOk && priceOk && categoryOk && imageOk
                && !isSubmitting && !isUploadingImage
        }
}

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
            val type = sessionManager.shopType.first()
            _uiState.update {
                it.copy(
                    shopDisplayName = name,
                    shopType = type,
                    category = it.category.takeIf { c -> ShopProductCategory.isValidForShopType(c, type) }
                        ?: "",
                )
            }
        }
    }

    private fun restoreDraftFromSavedState(): CreateProductUiState {
        @Suppress("UNCHECKED_CAST")
        val urls = (savedStateHandle.get<ArrayList<String>>(KEY_DRAFT_IMAGE_URLS) ?: arrayListOf()).toList()
        val names = savedStateHandle.get<ArrayList<String>>(KEY_DRAFT_VARIANT_NAMES)
        val prices = savedStateHandle.get<ArrayList<String>>(KEY_DRAFT_VARIANT_PRICES)
        val variantRows =
            if (names != null && prices != null && names.size == prices.size && names.isNotEmpty()) {
                names.zip(prices).map { ProductVariantRow(name = it.first, priceText = it.second) }
            } else {
                listOf(ProductVariantRow())
            }
        return CreateProductUiState(
            name = savedStateHandle[KEY_DRAFT_NAME] ?: "",
            description = savedStateHandle[KEY_DRAFT_DESCRIPTION] ?: "",
            priceText = savedStateHandle[KEY_DRAFT_PRICE] ?: "",
            saleType = savedStateHandle[KEY_DRAFT_SALE_TYPE] ?: SALE_TYPE_UNIQUE,
            variantRows = variantRows,
            hasPromotion = savedStateHandle[KEY_DRAFT_HAS_PROMO] ?: false,
            discountText = savedStateHandle[KEY_DRAFT_DISCOUNT] ?: "0",
            isActive = savedStateHandle[KEY_DRAFT_ACTIVE] ?: false,
            category = savedStateHandle[KEY_DRAFT_CATEGORY] ?: "",
            imageUrls = urls,
        )
    }

    /** Formulario nuevo: restaura el borrador persistido (no borrar SavedState antes de leer). */
    fun prepareNewProduct() {
        val shopName = _uiState.value.shopDisplayName
        val shopType = _uiState.value.shopType
        val draft = restoreDraftFromSavedState()
        _uiState.value = draft.copy(
            shopDisplayName = shopName,
            shopType = shopType,
            editingProductId = null,
            category = draft.category.takeIf { ShopProductCategory.isValidForShopType(it, shopType) }
                ?: "",
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
        val shopType = _uiState.value.shopType
        val variants = product.resolvedVariants
        val uniqueOnly = variants.isEmpty() || (variants.size == 1 && isUnicaName(variants.first().name))
        _uiState.value = CreateProductUiState(
            shopDisplayName = shopName,
            shopType = shopType,
            editingProductId = product.id,
            name = product.name,
            description = product.description ?: "",
            priceText = formatPriceForField(
                if (uniqueOnly) variants.firstOrNull()?.price ?: product.price else product.price
            ),
            saleType = if (uniqueOnly) SALE_TYPE_UNIQUE else SALE_TYPE_VARIANTS,
            variantRows = if (uniqueOnly) {
                listOf(ProductVariantRow())
            } else {
                variants.map { ProductVariantRow(it.name, formatPriceForField(it.price)) }
            },
            hasPromotion = product.hasPromotion,
            discountText = product.discount.coerceIn(0, 100).toString(),
            isActive = product.isActive,
            canActivate = product.canActivate,
            moderationStatus = product.moderationStatus,
            category = product.category.takeIf { ShopProductCategory.isValidForShopType(it, shopType) }
                ?: "",
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

    private fun isUnicaName(name: String): Boolean {
        val n = Normalizer.normalize(name.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
        return n == "unica"
    }

    /** Sobrevive a que el sistema mate el proceso mientras la app está en segundo plano (solo borrador de producto nuevo). */
    private fun persistDraftFields(s: CreateProductUiState) {
        savedStateHandle[KEY_DRAFT_NAME] = s.name
        savedStateHandle[KEY_DRAFT_DESCRIPTION] = s.description
        savedStateHandle[KEY_DRAFT_PRICE] = s.priceText
        savedStateHandle[KEY_DRAFT_SALE_TYPE] = s.saleType
        savedStateHandle[KEY_DRAFT_VARIANT_NAMES] = ArrayList(s.variantRows.map { it.name })
        savedStateHandle[KEY_DRAFT_VARIANT_PRICES] = ArrayList(s.variantRows.map { it.priceText })
        savedStateHandle[KEY_DRAFT_HAS_PROMO] = s.hasPromotion
        savedStateHandle[KEY_DRAFT_DISCOUNT] = s.discountText
        savedStateHandle[KEY_DRAFT_ACTIVE] = s.isActive
        savedStateHandle[KEY_DRAFT_CATEGORY] = s.category
        savedStateHandle[KEY_DRAFT_IMAGE_URLS] = ArrayList(s.imageUrls)
    }

    private fun clearSavedDraft() {
        savedStateHandle.remove<String>(KEY_DRAFT_NAME)
        savedStateHandle.remove<String>(KEY_DRAFT_DESCRIPTION)
        savedStateHandle.remove<String>(KEY_DRAFT_PRICE)
        savedStateHandle.remove<String>(KEY_DRAFT_SALE_TYPE)
        savedStateHandle.remove<ArrayList<String>>(KEY_DRAFT_VARIANT_NAMES)
        savedStateHandle.remove<ArrayList<String>>(KEY_DRAFT_VARIANT_PRICES)
        savedStateHandle.remove<Boolean>(KEY_DRAFT_HAS_PROMO)
        savedStateHandle.remove<String>(KEY_DRAFT_DISCOUNT)
        savedStateHandle.remove<Boolean>(KEY_DRAFT_ACTIVE)
        savedStateHandle.remove<String>(KEY_DRAFT_CATEGORY)
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
    fun onDescriptionChange(v: String) =
        updateDraft { it.copy(description = v.take(MAX_DESCRIPTION_LENGTH)) }
    fun onPriceChange(v: String) = updateDraft { it.copy(priceText = v) }
    fun onSaleTypeChange(v: String) = updateDraft { state ->
        state.copy(
            saleType = v,
            variantRows = if (v == SALE_TYPE_VARIANTS && state.variantRows.isEmpty()) {
                listOf(ProductVariantRow())
            } else {
                state.variantRows
            },
        )
    }
    fun onVariantNameChange(index: Int, v: String) = updateDraft { state ->
        state.copy(
            variantRows = state.variantRows.mapIndexed { i, row ->
                if (i == index) row.copy(name = v) else row
            },
        )
    }
    fun onVariantPriceChange(index: Int, v: String) = updateDraft { state ->
        state.copy(
            variantRows = state.variantRows.mapIndexed { i, row ->
                if (i == index) row.copy(priceText = v) else row
            },
        )
    }
    fun addVariantRow() = updateDraft { it.copy(variantRows = it.variantRows + ProductVariantRow()) }
    fun removeVariantRow(index: Int) = updateDraft { state ->
        if (state.variantRows.size <= 1) state
        else state.copy(variantRows = state.variantRows.filterIndexed { i, _ -> i != index })
    }
    fun onHasPromotionChange(v: Boolean) =
        updateDraft { it.copy(hasPromotion = v, discountText = if (v) it.discountText else "0") }
    fun onDiscountChange(v: String) = updateDraft { it.copy(discountText = v.filter { ch -> ch.isDigit() }) }
    fun onIsActiveChange(v: Boolean) = updateDraft { it.copy(isActive = v) }
    fun onCategoryChange(category: String) = updateDraft { it.copy(category = category) }

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
        if (!s.canSubmit) return
        val name = s.name.trim()
        val description = s.description.trim()
        val discount = s.discountText.toIntOrNull() ?: 0
        if (s.hasPromotion && (discount < 0 || discount > 100)) {
            _uiState.update { it.copy(errorMessage = "El descuento debe estar entre 0 y 100") }
            return
        }
        if (!ShopProductCategory.isValidForShopType(s.category, s.shopType)) {
            _uiState.update { it.copy(errorMessage = "Selecciona una categoría") }
            return
        }
        val variants: List<ProductVariantInput>
        val price: Double
        if (s.isRestaurant) {
            if (s.saleType == SALE_TYPE_VARIANTS) {
                val parsed = s.parsedVariantRows
                if (parsed.isEmpty()) {
                    _uiState.update { it.copy(errorMessage = "Agrega al menos una presentación con nombre y precio.") }
                    return
                }
                variants = parsed.map { ProductVariantInput(it.first, it.second) }
                price = parsed.minOf { it.second }
            } else {
                val uniquePrice = s.priceText.replace(",", ".").trim().toDoubleOrNull() ?: return
                variants = listOf(ProductVariantInput(DEFAULT_VARIANT_NAME, uniquePrice))
                price = uniquePrice
            }
        } else {
            variants = emptyList()
            price = s.priceText.replace(",", ".").trim().toDoubleOrNull() ?: return
        }
        val body = CreateShopProductRequest(
            name = name,
            description = description,
            price = price,
            variants = variants,
            imageUrls = s.imageUrls,
            hasPromotion = s.hasPromotion,
            discount = if (s.hasPromotion) discount else 0,
            isActive = if (s.editingProductId != null) s.isActive else false,
            category = s.category.trim().lowercase(),
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
                                shopType = it.shopType,
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
                                shopType = it.shopType,
                                successMessage = "«${created.name}» enviado a revisión"
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
