package com.ares.ewe_shop.domain.model

/** Categorías predefinidas de producto (slug API ↔ etiqueta UI). */
object ShopProductCategory {
    const val BEBIDAS = "bebidas"
    const val POSTRES = "postres"
    const val COMIDAS = "comidas"
    const val SNACKS = "snacks"
    const val MISCELANEOS = "miscelaneos"

    val DEFAULT = MISCELANEOS

    val all: List<Pair<String, String>> = listOf(
        BEBIDAS to "Bebidas",
        POSTRES to "Postres",
        COMIDAS to "Comidas",
        SNACKS to "Snacks",
        MISCELANEOS to "Misceláneos",
    )

    fun labelFor(slug: String?): String =
        all.firstOrNull { it.first == slug?.trim()?.lowercase() }?.second ?: "Misceláneos"

    fun isValid(slug: String?): Boolean =
        slug != null && all.any { it.first == slug.trim().lowercase() }
}
