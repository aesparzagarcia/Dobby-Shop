package com.ares.ewe_shop.core.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Parses API ISO-8601 UTC timestamps and formats them in the device local timezone. */
object OrderDateFormat {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun formatList(createdAt: String): String {
        val zoned = parse(createdAt) ?: return createdAt
        val datePart = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault()).format(zoned)
        val timePart = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()).format(zoned)
        return "$datePart • $timePart"
    }

    fun formatDetail(createdAt: String): String {
        val zoned = parse(createdAt) ?: return createdAt
        val datePart = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault()).format(zoned)
        val timePart = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()).format(zoned)
        return "$datePart · $timePart"
    }

    private fun parse(createdAt: String) =
        runCatching { Instant.parse(createdAt.trim()).atZone(zone) }.getOrNull()
}
