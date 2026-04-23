package com.ares.ewe_shop.presentation.ui.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ares.ewe_shop.data.remote.model.ShopProductDto
import com.ares.ewe_shop.presentation.viewmodel.product.ShopProductsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopProductsScreen(
    onNuevoClick: () -> Unit,
    onProductClick: (ShopProductDto) -> Unit = {},
    /** Tras crear o guardar un producto: refresca lista y muestra snackbar; luego llamar [onPostCreateMessageConsumed]. */
    postCreateMessage: String? = null,
    onPostCreateMessageConsumed: () -> Unit = {},
    viewModel: ShopProductsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadProducts()
    }

    LaunchedEffect(postCreateMessage) {
        val msg = postCreateMessage ?: return@LaunchedEffect
        viewModel.loadProducts()
        snackbarHostState.showSnackbar(msg)
        onPostCreateMessageConsumed()
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg -> snackbarHostState.showSnackbar(msg) }
    }

    // Sin insets inferiores: la NavigationBar vive en MainScreen; el default del Scaffold reservaría
    // otra franja y dejaría hueco blanco encima de la barra.
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text("Productos") },
                actions = {
                    TextButton(onClick = onNuevoClick) {
                        Text("Nuevo")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.products.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.errorMessage != null && uiState.products.isEmpty() && !uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadProducts() }) {
                                Text("Reintentar")
                            }
                        }
                    }
                }
                else -> {
                    var searchQuery by remember { mutableStateOf("") }
                    val filteredProducts = remember(uiState.products, searchQuery) {
                        filterProductsByQuery(uiState.products, searchQuery)
                    }
                    Column(modifier = Modifier.fillMaxSize()) {
                        ShopProductsFloatingSearchCard(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it }
                        )
                        when {
                            uiState.products.isEmpty() && !uiState.isLoading -> {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "Aún no tienes productos",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Pulsa «Nuevo» arriba para crear el primero",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            filteredProducts.isEmpty() -> {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Ningún producto coincide con la búsqueda",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            else -> {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(
                                        start = 16.dp,
                                        end = 16.dp,
                                        top = 20.dp,
                                        bottom = 0.dp
                                    ),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(filteredProducts, key = { it.id }) { product ->
                                        ShopProductCard(
                                            name = product.name,
                                            imageUrl = product.imageUrls.firstOrNull { it.isNotBlank() },
                                            price = product.price,
                                            rate = product.rate,
                                            hasPromotion = product.hasPromotion,
                                            discount = product.discount,
                                            isActive = product.isActive,
                                            onClick = { onProductClick(product) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun filterProductsByQuery(
    products: List<ShopProductDto>,
    query: String
): List<ShopProductDto> {
    val q = query.trim()
    if (q.isEmpty()) return products
    return products.filter { it.name.contains(q, ignoreCase = true) }
}

@Composable
private fun ShopProductsFloatingSearchCard(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            placeholder = {
                Text(
                    "Buscar",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = TextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        )
    }
}
