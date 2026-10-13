package com.manoogianmedia.studiorack.ui

import org.json.JSONObject

data class EventReadinessSummary(
    val status: String,
    val ready: Int,
    val blockers: Int,
    val notApplicable: Int,
    val total: Int,
) {
    val label: String get() = status.replace('_', ' ').replaceFirstChar(Char::uppercase)
}

fun eventReadinessSummary(requirements: List<JSONObject>): EventReadinessSummary {
    val byId = requirements.associateBy { it.optString("id") }
    val applicable = requirements.filter { it.optString("applicability", "applicable") != "not_applicable" }
    val ready = applicable.count { it.optString("status") in setOf("ready", "overridden") }
    val blockingIds = mutableSetOf<String>()
    applicable.forEach { row ->
        if (row.optString("importance", "required") in setOf("critical", "required") && row.optString("status") !in setOf("ready", "overridden")) {
            blockingIds += row.optString("id")
        }
        val dependency = byId[row.optString("depends_on_requirement_id")]
        if (dependency != null && dependency.optString("applicability", "applicable") != "not_applicable" && dependency.optString("status") !in setOf("ready", "overridden")) {
            blockingIds += row.optString("id")
        }
        val evidenceRequired = row.optInt("evidence_required") == 1 || row.optBoolean("evidence_required")
        if (row.optString("status") != "overridden" && evidenceRequired && row.optString("evidence_text").isBlank()) {
            blockingIds += row.optString("id")
        }
        if (row.optString("status") != "overridden" && row.optString("approval_status", "not_required") in setOf("pending", "rejected")) {
            blockingIds += row.optString("id")
        }
    }
    val required = applicable.filter { it.optString("importance", "required") in setOf("critical", "required") }
    val touched = applicable.any { it.optString("status", "not_started") != "not_started" }
    val status = when {
        applicable.isEmpty() || (!touched && required.isNotEmpty()) -> "not_started"
        required.all { it.optString("status") in setOf("ready", "overridden") } && blockingIds.isEmpty() -> "ready"
        required.any { it.optString("status") == "at_risk" || it.optString("approval_status") == "rejected" } -> "at_risk"
        else -> "in_progress"
    }
    return EventReadinessSummary(
        status = status,
        ready = ready,
        blockers = blockingIds.size,
        notApplicable = requirements.size - applicable.size,
        total = requirements.size,
    )
}
