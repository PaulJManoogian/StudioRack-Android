package com.manoogianmedia.studiorack.ui

import org.json.JSONObject

internal data class PerformanceGroup(
    val id: String,
    val type: String,
    val name: String,
)

internal fun JSONObject.performanceGroupOrNull(): PerformanceGroup? {
    fun text(key: String) = optString(key).trim().takeUnless { it.equals("null", ignoreCase = true) }.orEmpty()

    val id = text("performance_group_id")
    val type = text("performance_group_type").lowercase()
    val name = text("performance_group_name")
    return if (id.isNotBlank() && type in setOf("medley", "tribute") && name.isNotBlank()) {
        PerformanceGroup(id, type, name)
    } else null
}
