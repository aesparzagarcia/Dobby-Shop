package com.ares.ewe_shop.domain.repository

import android.net.Uri
import com.ares.ewe_shop.data.remote.model.CreateShopProductRequest
import com.ares.ewe_shop.data.remote.model.ShopProductDto

interface ProductRepository {
    suspend fun getShopProducts(category: String? = null): Result<List<ShopProductDto>>
    suspend fun createProduct(body: CreateShopProductRequest): Result<ShopProductDto>
    suspend fun updateProduct(
        productId: String,
        body: CreateShopProductRequest
    ): Result<ShopProductDto>
    suspend fun uploadProductImage(fileUri: Uri): Result<String>
}
