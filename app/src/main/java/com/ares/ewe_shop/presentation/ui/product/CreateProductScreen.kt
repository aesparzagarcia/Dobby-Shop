package com.ares.ewe_shop.presentation.ui.product

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ares.ewe_shop.core.util.absoluteUploadUrl
import com.ares.ewe_shop.data.remote.model.ShopProductDto
import com.ares.ewe_shop.presentation.viewmodel.product.CreateProductViewModel

private const val MAX_PHOTOS = 3

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateProductScreen(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    productToEdit: ShopProductDto? = null,
    /** Si se define, al crear o guardar bien se invoca con el mensaje y no se muestra snackbar aquí. */
    onCreateSuccess: ((String) -> Unit)? = null,
    viewModel: CreateProductViewModel = hiltViewModel()
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
        contract = ActivityResultContracts.PickVisualMedia()
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


    val shopLabel = uiState.shopDisplayName?.takeIf { it.isNotBlank() } ?: "Tu tienda"
    val isEditing = uiState.editingProductId != null
    val topTitle = if (isEditing) "Editar producto" else shopLabel
    // Sin insets extra en el contenido: el TopAppBar ya respeta la status bar; el default del
    // Scaffold (safeDrawing) duplica padding arriba y empuja el formulario hacia abajo.
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text(topTitle) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver"
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        // Con windowSoftInputMode=adjustResize el sistema ya reduce la ventana por encima del teclado.
        // NO usar imePadding() aquí: duplica el hueco y deja una franja blanca enorme sobre el IME.
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = viewModel::onNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nombre") },
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = uiState.priceText,
                        onValueChange = viewModel::onPriceChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Precio") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
                item {
                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = viewModel::onDescriptionChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Descripción (opcional)") },
                        minLines = 2
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tiene promoción", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = uiState.hasPromotion,
                            onCheckedChange = viewModel::onHasPromotionChange
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = uiState.discountText,
                        onValueChange = viewModel::onDiscountChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Descuento (%)") },
                        enabled = uiState.hasPromotion,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Producto activo (visible en la app)", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = uiState.isActive,
                            onCheckedChange = viewModel::onIsActiveChange
                        )
                    }
                }
                item {
                    Text("Fotos", style = MaterialTheme.typography.labelLarge)
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                pickImage.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = uiState.imageUrls.size < MAX_PHOTOS && !uiState.isUploadingImage
                        ) {
                            Text("Añadir foto")
                        }
                        if (uiState.isUploadingImage) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Text("Subiendo…", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (uiState.imageUrls.isNotEmpty()) {
                    item {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.imageUrls.forEachIndexed { index, url ->
                                Box {
                                    AsyncImage(
                                        model = absoluteUploadUrl(url),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(88.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = { viewModel.removeImageAt(index) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Quitar")
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Button(
                        onClick = { viewModel.submit() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSubmitting && !uiState.isUploadingImage
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (uiState.isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp).padding(end = 8.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            Text(if (isEditing) "Guardar producto" else "Crear producto")
                        }
                    }
                }
            }
        }
    }
}
