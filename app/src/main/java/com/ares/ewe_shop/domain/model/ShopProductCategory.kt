package com.ares.ewe_shop.domain.model

/** Categorías predefinidas de producto (slug API ↔ etiqueta UI). */
object ShopProductCategory {
    const val BEBIDAS = "bebidas"
    const val ALCOHOL = "alcohol"
    const val POSTRES = "postres"
    const val COMIDAS = "comidas"
    const val SNACKS = "snacks"
    const val MISCELANEOS = "miscelaneos"
    const val MEDICAMENTOS = "medicamentos"

    val DEFAULT = MISCELANEOS

    val all: List<Pair<String, String>> = listOf(
        BEBIDAS to "Bebidas",
        ALCOHOL to "Alcohol",
        POSTRES to "Postres",
        COMIDAS to "Comidas",
        SNACKS to "Snacks",
        MISCELANEOS to "Misceláneos",
        MEDICAMENTOS to "Medicamentos",
    )

    private val restaurantOnly = setOf(POSTRES, COMIDAS)
    private val shopOnly = setOf(MEDICAMENTOS)

    /** Categorías seleccionables según tipo (`SHOP` vs `RESTAURANT`). */
    fun availableForShopType(shopType: String?): List<Pair<String, String>> {
        val t = shopType?.trim()?.uppercase().orEmpty()
        if (t == "CAR_WASH") return emptyList()
        val isShop = t == "SHOP"
        return all.filter { (slug, _) ->
            when {
                slug in restaurantOnly -> !isShop
                slug in shopOnly -> isShop
                else -> true
            }
        }
    }

    fun labelFor(slug: String?): String =
        all.firstOrNull { it.first == slug?.trim()?.lowercase() }?.second ?: "Misceláneos"

    fun isValid(slug: String?): Boolean =
        slug != null && all.any { it.first == slug.trim().lowercase() }

    fun isValidForShopType(slug: String?, shopType: String?): Boolean {
        val normalized = slug?.trim()?.lowercase() ?: return false
        return availableForShopType(shopType).any { it.first == normalized }
    }
}
