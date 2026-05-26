package com.ares.ewe_shop.presentation.ui.product

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.hilt.navigation.compose.hiltViewModel
import com.ares.ewe_shop.core.theme.DobbyShopColors
import com.ares.ewe_shop.data.remote.model.ShopProductDto
import com.ares.ewe_shop.domain.model.ShopProductCategory
import com.ares.ewe_shop.presentation.viewmodel.product.ShopProductsViewModel

private data class ProductCategoryChip(
    val id: String?,
    val label: String,
    val emoji: String,
)

private val productCategoryChips = listOf(ProductCategoryChip(null, "Todos", "")) +
    ShopProductCategory.all.map { (slug, label) ->
        val emoji = when (slug) {
            ShopProductCategory.BEBIDAS -> "🥤"
            ShopProductCategory.POSTRES -> "🍨"
            ShopProductCategory.COMIDAS -> "🍽️"
            ShopProductCategory.SNACKS -> "🍿"
            else -> "📦"
        }
        ProductCategoryChip(slug, label, emoji)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopProductsScreen(
    onNuevoClick: () -> Unit,
    onProductClick: (ShopProductDto) -> Unit = {},
    postCreateMessage: String? = null,
    onPostCreateMessageConsumed: () -> Unit = {},
    viewModel: ShopProductsViewModel = hiltViewModel(),
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

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        containerColor = DobbyShopColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopStart,
        ) {
            when {
                uiState.isLoading && uiState.products.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = DobbyShopColors.Purple)
                    }
                }
                uiState.errorMessage != null && uiState.products.isEmpty() && !uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadProducts() }) {
                                Text("Reintentar")
                            }
                        }
                    }
                }
                else -> {
                    var searchQuery by rememberSaveable { mutableStateOf("") }
                    val selectedCategoryId = uiState.selectedCategoryId
                    val searchFilteredProducts = remember(uiState.products, searchQuery) {
                        filterProductsBySearch(uiState.products, searchQuery)
                    }
                    val groupedSections = remember(searchFilteredProducts, selectedCategoryId) {
                        if (selectedCategoryId == null) {
                            groupProductsByCategory(searchFilteredProducts)
                        } else {
                            emptyList()
                        }
                    }
                    val productRows = remember(groupedSections, searchFilteredProducts, selectedCategoryId) {
                        if (selectedCategoryId == null) {
                            buildGroupedRows(groupedSections)
                        } else {
                            searchFilteredProducts.chunked(2).map { ProductListRow.ProductPair(it) }
                        }
                    }
                    Column(modifier = Modifier.fillMaxSize()) {
                        ProductsHeader(onNuevoClick = onNuevoClick)
                        ProductsSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                        )
                        ProductCategoryFilterRow(
                            selectedCategoryId = selectedCategoryId,
                            onCategorySelected = viewModel::onCategorySelected,
                        )
                        when {
                            uiState.products.isEmpty() && !uiState.isLoading -> {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    ProductsEmptyState(
                                        title = "Aún no tienes productos",
                                        subtitle = "Pulsa «+ Nuevo» para crear el primero",
                                    )
                                }
                            }
                            searchFilteredProducts.isEmpty() -> {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    ProductsEmptyState(
                                        title = when {
                                            searchQuery.isNotBlank() ->
                                                "Ningún producto coincide con la búsqueda"
                                            selectedCategoryId != null ->
                                                "No hay productos en esta categoría"
                                            else -> "Ningún producto coincide con la búsqueda"
                                        },
                                    )
                                }
                            }
                            else -> {
                                ProductsLazyList(
                                    rows = productRows,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    onProductClick = onProductClick,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductsHeader(onNuevoClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Productos",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = DobbyShopColors.PurpleDark,
        )
        Surface(
            onClick = onNuevoClick,
            shape = RoundedCornerShape(14.dp),
            color = DobbyShopColors.Purple,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Nuevo",
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun ProductCategoryFilterRow(
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(productCategoryChips, key = { it.label }) { chip ->
            val selected = selectedCategoryId == chip.id
            val shape = RoundedCornerShape(50)
            val background = when {
                selected -> DobbyShopColors.PurpleLight
                else -> DobbyShopColors.Surface
            }
            val borderColor = if (selected) DobbyShopColors.Purple else DobbyShopColors.Border
            val textColor = if (selected) DobbyShopColors.Purple else DobbyShopColors.TextSecondary

            Row(
                modifier = Modifier
                    .clip(shape)
                    .border(1.dp, borderColor, shape)
                    .background(background, shape)
                    .clickable { onCategorySelected(chip.id) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (chip.emoji.isNotEmpty()) {
                    Text(text = chip.emoji, fontSize = 14.sp)
                }
                Text(
                    text = chip.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = textColor,
                )
            }
        }
    }
}

@Composable
private fun ProductsSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(DobbyShopColors.Surface)
                .border(1.dp, DobbyShopColors.Border, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = DobbyShopColors.TextSecondary,
                modifier = Modifier.size(20.dp),
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = DobbyShopColors.TextPrimary,
                ),
                cursorBrush = SolidColor(DobbyShopColors.Purple),
                decorationBox = { inner ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text = "Buscar productos...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DobbyShopColors.TextSecondary,
                            )
                        }
                        inner()
                    }
                },
            )
        }
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = DobbyShopColors.PurpleLight,
            modifier = Modifier.size(48.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Filtros",
                    tint = DobbyShopColors.Purple,
                )
            }
        }
    }
}

private data class ProductCategorySection(
    val categoryId: String,
    val title: String,
    val products: List<ShopProductDto>,
)

private sealed class ProductListRow {
    data class Header(val title: String) : ProductListRow()
    data class ProductPair(val products: List<ShopProductDto>) : ProductListRow()
}

private fun normalizeProductCategory(raw: String?): String {
    val slug = raw?.trim()?.lowercase()
    return if (slug != null && ShopProductCategory.isValid(slug)) slug else ShopProductCategory.DEFAULT
}

private fun filterProductsBySearch(
    products: List<ShopProductDto>,
    query: String,
): List<ShopProductDto> {
    val q = query.trim()
    if (q.isEmpty()) return products
    return products.filter { it.name.contains(q, ignoreCase = true) }
}

private fun groupProductsByCategory(products: List<ShopProductDto>): List<ProductCategorySection> {
    return ShopProductCategory.all.mapNotNull { (slug, label) ->
        val inCategory = products.filter { normalizeProductCategory(it.category) == slug }
        if (inCategory.isEmpty()) null else ProductCategorySection(slug, label, inCategory)
    }
}

private fun buildGroupedRows(sections: List<ProductCategorySection>): List<ProductListRow> {
    val rows = mutableListOf<ProductListRow>()
    for (section in sections) {
        rows.add(ProductListRow.Header(section.title))
        section.products.chunked(2).forEach { chunk ->
            rows.add(ProductListRow.ProductPair(chunk))
        }
    }
    return rows
}

@Composable
private fun ProductsEmptyState(
    title: String,
    subtitle: String? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = DobbyShopColors.TextPrimary,
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = DobbyShopColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun ProductsLazyList(
    rows: List<ProductListRow>,
    modifier: Modifier = Modifier,
    onProductClick: (ShopProductDto) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 8.dp,
            bottom = 20.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            items = rows,
            key = { row ->
                when (row) {
                    is ProductListRow.Header -> "h-${row.title}"
                    is ProductListRow.ProductPair -> "r-${row.products.joinToString { it.id }}"
                }
            },
        ) { row ->
            when (row) {
                is ProductListRow.Header -> {
                    Text(
                        text = row.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DobbyShopColors.PurpleDark,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                    )
                }
                is ProductListRow.ProductPair -> {
                    ProductCardRow(
                        products = row.products,
                        onProductClick = onProductClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductCardRow(
    products: List<ShopProductDto>,
    onProductClick: (ShopProductDto) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        products.forEach { product ->
            Box(modifier = Modifier.weight(1f)) {
                ShopProductCard(
                    name = product.name,
                    imageUrl = product.imageUrls.firstOrNull { it.isNotBlank() },
                    price = product.price,
                    rate = product.rate,
                    hasPromotion = product.hasPromotion,
                    discount = product.discount,
                    isActive = product.isActive,
                    categoryLabel = ShopProductCategory.labelFor(product.category),
                    onClick = { onProductClick(product) },
                    onMenuClick = { onProductClick(product) },
                )
            }
        }
        if (products.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
