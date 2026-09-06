package com.manoogianmedia.studiorack.ui

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

internal fun serviceDue(values: Map<String, String>): String = values["next_service_due"].orEmpty().ifBlank {
    runCatching {
        val interval = values["service_interval_days"]?.toLongOrNull() ?: 0
        if (interval > 0) LocalDate.parse(values["last_service_date"]).plusDays(interval).toString() else ""
    }.getOrDefault("")
}

internal fun resolvedNoteIds(records: List<JSONObject>): Set<String> = records.flatMap { row ->
    val ids = runCatching { JSONArray(row.optString("resolved_note_ids", "[]")) }.getOrDefault(JSONArray())
    (0 until ids.length()).map { ids.optString(it) }
}.toSet()

internal fun projectedMaintenanceSpecs(values: Map<String, String>, records: List<JSONObject>): Map<String, String> {
    val projected = values.toMutableMap()
    records.sortedWith(compareBy({ it.optString("completed_on") }, { it.optString("created_utc") })).forEach { record ->
        val completed = record.optString("completed_on")
        if (serviceDue(projected) == record.optString("expected_due") && completed >= projected["last_service_date"].orEmpty()) {
            projected["last_service_date"] = completed
            projected["last_service_notes"] = record.optString("summary")
            projected["care_status"] = "handled"
            projected.remove("next_service_due")
            projected["next_service_due"] = record.optString("next_due").ifBlank { serviceDue(projected) }
        }
    }
    return projected
}
