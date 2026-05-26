package com.ares.ewe_shop.presentation.ui.product

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ares.ewe_shop.core.theme.DobbyShopColors
import com.ares.ewe_shop.core.util.absoluteUploadUrl
import com.ares.ewe_shop.data.remote.model.ShopProductDto
import com.ares.ewe_shop.domain.model.ShopProductCategory
import com.ares.ewe_shop.presentation.viewmodel.product.CreateProductViewModel

private const val MAX_PHOTOS = 5
private const val MAX_DESCRIPTION_LENGTH = 200

private fun categoryEmoji(slug: String): String = when (slug) {
    ShopProductCategory.BEBIDAS -> "🥤"
    ShopProductCategory.POSTRES -> "🍨"
    ShopProductCategory.COMIDAS -> "🍽️"
    ShopProductCategory.SNACKS -> "🍿"
    else -> "📦"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProductScreen(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    productToEdit: ShopProductDto? = null,
    onCreateSuccess: ((String) -> Unit)? = null,
    viewModel: CreateProductViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(productToEdit?.id) {
        if (productToEdit == null) {
            viewModel.prepareNewProduct()
        } else {
            viewModel.loadForEdit(productToEdit)
        }
    }

    BackHandler(enabled = onBack != null) {
        onBack?.invoke()
    }

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let { viewModel.addImageFromUri(it) }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    LaunchedEffect(uiState.successMessage) {
        val msg = uiState.successMessage ?: return@LaunchedEffect
        viewModel.clearSuccess()
        if (onCreateSuccess != null) {
            onCreateSuccess(msg)
        } else {
            snackbarHostState.showSnackbar(msg)
        }
    }

    val isEditing = uiState.editingProductId != null
    val screenTitle = if (isEditing) "Editar producto" else "Nuevo producto"
    val submitLabel = if (isEditing) "Guardar producto" else "Crear producto"

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DobbyShopColors.Surface,
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CreateProductTopBar(
                title = screenTitle,
                onBack = onBack,
            )
        },
        bottomBar = {
            CreateProductSubmitBar(
                label = submitLabel,
                isSubmitting = uiState.isSubmitting,
                enabled = !uiState.isSubmitting && !uiState.isUploadingImage,
                onClick = { viewModel.submit() },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                CreateFormField(
                    label = "Nombre",
                    required = true,
                    value = uiState.name,
                    onValueChange = viewModel::onNameChange,
                    placeholder = "Ej. Hamburguesa clásica",
                    leadingIcon = Icons.Default.LocalOffer,
                    singleLine = true,
                )
            }
            item {
                CreateFormField(
                    label = "Precio",
                    required = true,
                    value = uiState.priceText,
                    onValueChange = viewModel::onPriceChange,
                    placeholder = "Ej. 8.50",
                    leadingIcon = Icons.Outlined.AttachMoney,
                    singleLine = true,
                    keyboardType = KeyboardType.Decimal,
                )
            }
            item {
                CreateDescriptionField(
                    value = uiState.description,
                    onValueChange = viewModel::onDescriptionChange,
                    maxLength = MAX_DESCRIPTION_LENGTH,
                )
            }
            item {
                CreateCategorySelector(
                    selectedCategory = uiState.category,
                    onCategorySelected = viewModel::onCategoryChange,
                )
            }
            item {
                CreateToggleRow(
                    label = "Tiene promoción",
                    checked = uiState.hasPromotion,
                    onCheckedChange = viewModel::onHasPromotionChange,
                )
            }
            if (uiState.hasPromotion) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(DobbyShopColors.PurpleLight)
                            .padding(12.dp),
                    ) {
                        CreateFormField(
                            label = "Descuento (%)",
                            required = false,
                            value = uiState.discountText,
                            onValueChange = viewModel::onDiscountChange,
                            placeholder = "Ej. 10",
                            leadingIcon = Icons.Default.LocalOffer,
                            singleLine = true,
                            keyboardType = KeyboardType.Number,
                            fieldBackground = Color.Transparent,
                        )
                    }
                }
            }
            item {
                CreateToggleRow(
                    label = "Producto activo (visible en la app)",
                    checked = uiState.isActive,
                    onCheckedChange = viewModel::onIsActiveChange,
                )
            }
            item {
                CreatePhotoSection(
                    imageUrls = uiState.imageUrls,
                    isUploading = uiState.isUploadingImage,
                    maxPhotos = MAX_PHOTOS,
                    onAddPhoto = {
                        pickImage.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    onRemovePhoto = viewModel::removeImageAt,
                )
            }
        }
    }
}

@Composable
private fun CreateProductTopBar(
    title: String,
    onBack: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DobbyShopColors.Surface)
            .statusBarsPadding()
            .padding(start = 4.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = DobbyShopColors.TextPrimary,
                )
            }
        }
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = DobbyShopColors.PurpleDark,
        )
    }
}

@Composable
private fun CreateProductSubmitBar(
    label: String,
    isSubmitting: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DobbyShopColors.Surface)
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp),
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DobbyShopColors.Purple,
                disabledContainerColor = DobbyShopColors.Purple.copy(alpha = 0.5f),
            ),
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AddShoppingCart,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = label,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun CreateFormField(
    label: String,
    required: Boolean,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    singleLine: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    showLabel: Boolean = true,
    fieldBackground: Color = DobbyShopColors.Surface,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (showLabel) {
            CreateFieldLabel(label = label, required = required)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(fieldBackground)
                .border(1.dp, DobbyShopColors.Border, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = if (singleLine) 12.dp else 10.dp),
            verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    ,
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = DobbyShopColors.Purple,
                    modifier = Modifier.size(20.dp),
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                singleLine = singleLine,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = DobbyShopColors.TextPrimary,
                    fontWeight = FontWeight.Medium,
                ),
                cursorBrush = SolidColor(DobbyShopColors.Purple),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = DobbyShopColors.TextSecondary,
                            )
                        }
                        inner()
                    }
                },
            )
        }
    }
}

@Composable
private fun CreateDescriptionField(
    value: String,
    onValueChange: (String) -> Unit,
    maxLength: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CreateFieldLabel(label = "Descripción (opcional)", required = false)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DobbyShopColors.Surface)
                .border(1.dp, DobbyShopColors.Border, RoundedCornerShape(14.dp))
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DobbyShopColors.PurpleLight),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = DobbyShopColors.Purple,
                        modifier = Modifier.size(20.dp),
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = DobbyShopColors.TextPrimary,
                    ),
                    cursorBrush = SolidColor(DobbyShopColors.Purple),
                    decorationBox = { inner ->
                        Box {
                            if (value.isEmpty()) {
                                Text(
                                    text = "Cuenta a tus clientes de qué trata este producto...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DobbyShopColors.TextSecondary,
                                )
                            }
                            inner()
                        }
                    },
                )
            }
            Text(
                text = "${value.length}/$maxLength",
                style = MaterialTheme.typography.labelSmall,
                color = DobbyShopColors.TextSecondary,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}

@Composable
private fun CreateFieldLabel(label: String, required: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = DobbyShopColors.TextPrimary,
        )
        if (required) {
            Text(
                text = " *",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = DobbyShopColors.Purple,
            )
        }
    }
}

@Composable
private fun CreateCategorySelector(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CreateFieldLabel(label = "Categoría", required = true)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 4.dp),
        ) {
            items(ShopProductCategory.all, key = { it.first }) { (slug, label) ->
                val selected = selectedCategory == slug
                val shape = RoundedCornerShape(50)
                val background = if (selected) DobbyShopColors.PurpleLight else DobbyShopColors.Surface
                val borderColor = if (selected) DobbyShopColors.Purple else DobbyShopColors.Border
                val textColor = if (selected) DobbyShopColors.Purple else DobbyShopColors.TextSecondary

                Row(
                    modifier = Modifier
                        .clip(shape)
                        .border(1.dp, borderColor, shape)
                        .background(background, shape)
                        .clickable { onCategorySelected(slug) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(text = categoryEmoji(slug), fontSize = 14.sp)
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = textColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = DobbyShopColors.TextPrimary,
            modifier = Modifier.weight(1f).padding(end = 12.dp),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = DobbyShopColors.Purple,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFD1D5DB),
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun CreatePhotoSection(
    imageUrls: List<String>,
    isUploading: Boolean,
    maxPhotos: Int,
    onAddPhoto: () -> Unit,
    onRemovePhoto: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Fotos del producto",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = DobbyShopColors.TextPrimary,
        )

        if (imageUrls.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(imageUrls.size, key = { imageUrls[it] }) { index ->
                    Box {
                        AsyncImage(
                            model = absoluteUploadUrl(imageUrls[index]),
                            contentDescription = null,
                            modifier = Modifier
                                .size(88.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        IconButton(
                            onClick = { onRemovePhoto(index) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f)),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Quitar",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
                if (imageUrls.size < maxPhotos) {
                    item {
                        CreateAddPhotoTile(
                            isUploading = isUploading,
                            compact = true,
                            onClick = onAddPhoto,
                        )
                    }
                }
            }
        }

        if (imageUrls.isEmpty()) {
            CreateAddPhotoTile(
                isUploading = isUploading,
                compact = false,
                onClick = onAddPhoto,
                enabled = !isUploading && imageUrls.size < maxPhotos,
            )
        }

        Text(
            text = "Puedes añadir hasta $maxPhotos fotos",
            style = MaterialTheme.typography.bodySmall,
            color = DobbyShopColors.TextSecondary,
        )
    }
}

@Composable
private fun CreateAddPhotoTile(
    isUploading: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(14.dp)
    val dashColor = DobbyShopColors.Purple.copy(alpha = 0.45f)
    val modifier = if (compact) {
        Modifier.size(88.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .height(140.dp)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(DobbyShopColors.PurpleLight.copy(alpha = 0.45f))
            .drawBehind {
                val stroke = 2.dp.toPx()
                drawRoundRect(
                    color = dashColor,
                    style = Stroke(
                        width = stroke,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f),
                    ),
                    cornerRadius = CornerRadius(14.dp.toPx()),
                )
            }
            .clickable(enabled = enabled && !isUploading, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isUploading) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = DobbyShopColors.Purple,
                strokeWidth = 2.dp,
            )
        } else if (compact) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Añadir foto",
                tint = DobbyShopColors.Purple,
                modifier = Modifier.size(28.dp),
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(DobbyShopColors.PurpleLight),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = DobbyShopColors.Purple,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Text(
                    text = "Añadir foto",
                    fontWeight = FontWeight.SemiBold,
                    color = DobbyShopColors.Purple,
                )
                Text(
                    text = "Toca para seleccionar imágenes",
                    style = MaterialTheme.typography.bodySmall,
                    color = DobbyShopColors.TextSecondary,
                )
            }
        }
    }
}
