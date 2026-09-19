package com.manoogianmedia.studiorack.crew

import org.json.JSONArray
import org.json.JSONObject

data class CrewBehaviorSettings(
    val persistenceLevel: Int = 3,
    val adaptiveTiming: Boolean = false,
    val warmthLevel: Int = 3,
    val responseDetail: String = "balanced",
    val humorEnabled: Boolean = false,
    val proactiveSuggestions: Boolean = true,
    val uncertaintyStyle: String = "transparent",
    val preferredChannels: List<String> = listOf("in_app", "email"),
    val phrasesToAvoid: String = "",
    val communicationNotes: String = "",
    val approvedExamples: String = "",
    val postEventCheckinEnabled: Boolean = true,
    val postEventCheckinDelayHours: Int = 1,
    val postEventCheckinCloseDays: Int = 2,
    val postEventCheckinFollowupDays: Int = 0,
) {
    fun toJson(pendingSync: Boolean = false): JSONObject = JSONObject()
        .put("crew_persistence_level", persistenceLevel.coerceIn(1, 5))
        .put("crew_adaptive_timing", if (adaptiveTiming) 1 else 0)
        .put("crew_warmth_level", warmthLevel.coerceIn(1, 5))
        .put("crew_response_detail", responseDetail)
        .put("crew_humor_enabled", if (humorEnabled) 1 else 0)
        .put("crew_proactive_suggestions", if (proactiveSuggestions) 1 else 0)
        .put("crew_uncertainty_style", uncertaintyStyle)
        .put("crew_preferred_channels", JSONArray(preferredChannels))
        .put("crew_phrases_to_avoid", phrasesToAvoid)
        .put("crew_communication_notes", communicationNotes)
        .put("crew_approved_examples", approvedExamples)
        .put("post_event_checkin_enabled", if (postEventCheckinEnabled) 1 else 0)
        .put("post_event_checkin_delay_hours", postEventCheckinDelayHours.coerceIn(0, 72))
        .put("post_event_checkin_close_days", postEventCheckinCloseDays.coerceIn(1, 30))
        .put("post_event_checkin_followup_days", postEventCheckinFollowupDays.coerceIn(0, (postEventCheckinCloseDays - 1).coerceAtLeast(0)))
        .put("_mobile_pending", if (pendingSync) 1 else 0)

    companion object {
        fun fromJson(value: String): CrewBehaviorSettings {
            val json = runCatching { JSONObject(value) }.getOrDefault(JSONObject())
            val channels = json.optJSONArray("crew_preferred_channels")?.let { values ->
                (0 until values.length()).mapNotNull { index -> values.optString(index).takeIf(String::isNotBlank) }
            }.orEmpty().ifEmpty { listOf("in_app", "email") }
            return CrewBehaviorSettings(
                persistenceLevel = json.optInt("crew_persistence_level", 3).coerceIn(1, 5),
                adaptiveTiming = json.optInt("crew_adaptive_timing") == 1,
                warmthLevel = json.optInt("crew_warmth_level", 3).coerceIn(1, 5),
                responseDetail = json.optString("crew_response_detail", "balanced"),
                humorEnabled = json.optInt("crew_humor_enabled") == 1,
                proactiveSuggestions = json.optInt("crew_proactive_suggestions", 1) == 1,
                uncertaintyStyle = json.optString("crew_uncertainty_style", "transparent"),
                preferredChannels = channels,
                phrasesToAvoid = json.optString("crew_phrases_to_avoid"),
                communicationNotes = json.optString("crew_communication_notes"),
                approvedExamples = json.optString("crew_approved_examples"),
                postEventCheckinEnabled = json.optInt("post_event_checkin_enabled", 1) == 1,
                postEventCheckinDelayHours = json.optInt("post_event_checkin_delay_hours", 1).coerceIn(0, 72),
                postEventCheckinCloseDays = json.optInt("post_event_checkin_close_days", 2).coerceIn(1, 30),
                postEventCheckinFollowupDays = json.optInt("post_event_checkin_followup_days", 0).coerceIn(0, 29),
            )
        }
    }
}
