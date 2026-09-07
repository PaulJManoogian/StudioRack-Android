package com.manoogianmedia.studiorack.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.manoogianmedia.studiorack.MainActivity
import com.manoogianmedia.studiorack.R
import com.manoogianmedia.studiorack.StudioRackApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.Instant
import java.time.temporal.ChronoUnit

internal const val EXTRA_NOTIFICATION_SOURCE = "studiorack_notification_source"
internal const val EXTRA_NOTIFICATION_DESTINATION = "studiorack_notification_destination"
internal const val EXTRA_NOTIFICATION_RECORD = "studiorack_notification_record"

data class NotificationRoute(val destination: String, val recordId: String)

private const val GROUP_ID = "studiorack_alerts"
private const val GROUP_KEY = "studiorack.notifications"
private const val SUMMARY_ID = 7300
private const val CHANNEL_MAINTENANCE = "studiorack_maintenance"
private const val CHANNEL_SCHEDULE = "studiorack_schedule"
private const val CHANNEL_BUDDY = "studiorack_buddy"

private data class NotificationCandidate(
    val sourceId: String,
    val fingerprint: String,
    val channelId: String,
    val title: String,
    val body: String,
    val destination: String,
    val recordId: String,
)

class StudioRackNotifications(private val context: Context, private val dao: StudioRackDao) {
    private val manager = NotificationManagerCompat.from(context)

    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val system = context.getSystemService(NotificationManager::class.java)
        val productName = context.getString(R.string.app_name)
        val agentName = context.getString(R.string.agent_name)
        system.createNotificationChannelGroup(NotificationChannelGroup(GROUP_ID, productName))
        listOf(
            NotificationChannel(CHANNEL_MAINTENANCE, "Maintenance", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Equipment care and maintenance reminders"
                group = GROUP_ID
            },
            NotificationChannel(CHANNEL_SCHEDULE, "Sessions", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Performance, rehearsal, and session reminders"
                group = GROUP_ID
            },
            NotificationChannel(CHANNEL_BUDDY, agentName, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "$agentName actions and responses"
                group = GROUP_ID
            },
        ).forEach(system::createNotificationChannel)
    }

    suspend fun reconcile() {
        scheduleEventAlarms()
        val candidates = candidates()
        val activeIds = candidates.mapTo(mutableSetOf()) { it.sourceId }
        val receipts = dao.notificationReceipts().associateBy(NotificationReceipt::sourceId)
        receipts.values.filter { it.unread && it.sourceId !in activeIds }.forEach {
            dao.markNotificationRead(it.sourceId)
            manager.cancel(it.notificationId)
        }
        if (!canNotify()) {
            updateSummary()
            return
        }
        candidates.forEach { candidate ->
            val receipt = receipts[candidate.sourceId]
            if (receipt == null || (receipt.unread && receipt.fingerprint != candidate.fingerprint)) post(candidate)
        }
        updateSummary()
    }

    suspend fun notifyDueEvent(eventId: String) {
        val event = dao.record("studio_event", eventId)?.let { JSONObject(it.json) } ?: return
        if (!eventIsActive(event) || event.optInt("reminder_enabled", 1) != 1) return
        val candidate = eventCandidate(event, eventId) ?: return
        val receipt = dao.notificationReceipts().firstOrNull { it.sourceId == candidate.sourceId }
        if ((receipt != null && receipt.fingerprint == candidate.fingerprint) || !canNotify()) return
        post(candidate)
        updateSummary()
    }

    suspend fun markRead(sourceId: String) {
        val receipt = dao.notificationReceipts().firstOrNull { it.sourceId == sourceId } ?: return
        dao.markNotificationRead(sourceId)
        manager.cancel(receipt.notificationId)
        updateSummary()
    }

    suspend fun clearAll() {
        dao.notificationReceipts().forEach { manager.cancel(it.notificationId) }
        manager.cancel(SUMMARY_ID)
        dao.clearNotificationReceipts()
    }

    private suspend fun candidates(): List<NotificationCandidate> {
        val itemNames = dao.supporting("item").associate { it.entityId to JSONObject(it.json).optString("display_name", "Equipment") }
        val actions = dao.supporting("studio_buddy_action").map { it.entityId to JSONObject(it.json) }
            .filter { (_, row) -> row.optString("cleared_utc").isBlank() && row.optString("status") in setOf("sent", "waiting", "failed") }
            .filter { (_, row) -> row.optString("status") != "sent" || isRecent(row.optString("updated_utc")) }
            .sortedByDescending { (_, row) -> row.optString("updated_utc") }
        val actionIds = actions.mapTo(mutableSetOf()) { it.first }
        val output = actions.map { (id, row) ->
            val actionType = row.optString("action_type")
            val eventId = id.removePrefix("sba_evt_").takeIf { id.startsWith("sba_evt_") }.orEmpty()
            val itemId = row.optString("item_id")
            NotificationCandidate(
                sourceId = id,
                fingerprint = listOf(row.optString("updated_utc"), row.optString("status"), row.optString("last_reply_utc")).joinToString("|"),
                channelId = when { actionType == "event_reminder" -> CHANNEL_SCHEDULE; itemId.isNotBlank() -> CHANNEL_MAINTENANCE; else -> CHANNEL_BUDDY },
                title = cleanSubject(row.optString("subject").ifBlank { context.getString(R.string.agent_name) }),
                body = row.optString("last_reply_body").ifBlank { row.optString("body") }.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty().take(240),
                destination = when { eventId.isNotBlank() -> "sessions"; itemId.isNotBlank() -> "equipment"; else -> "dashboard" },
                recordId = eventId.ifBlank { itemId },
            )
        }.toMutableList()

        dao.records("maintenance_note").map { it.entityId to JSONObject(it.json) }
            .filter { (_, row) -> row.optString("status", "pending") in setOf("pending", "notified", "rescheduled") }
            .forEach { (id, row) ->
                val sourceId = "sba_note_$id"
                if (sourceId !in actionIds) output += NotificationCandidate(
                    sourceId = sourceId,
                    fingerprint = listOf(row.optString("updated_utc"), row.optString("status"), row.optString("note")).joinToString("|"),
                    channelId = CHANNEL_MAINTENANCE,
                    title = "${itemNames[row.optString("item_id")] ?: "Equipment"} needs attention",
                    body = row.optString("note", "Review this maintenance item."),
                    destination = "equipment",
                    recordId = row.optString("item_id"),
                )
            }

        dao.records("studio_event").map { it.entityId to JSONObject(it.json) }
            .filter { (_, event) -> eventIsActive(event) }
            .filter { (_, event) -> event.optInt("reminder_enabled", 1) == 1 && (eventReminderAt(event)?.isAfter(LocalDateTime.now()) == false) }
            .forEach { (eventId, event) ->
                val candidate = eventCandidate(event, eventId)
                if (candidate != null && candidate.sourceId !in actionIds) output += candidate
            }
        return output.distinctBy(NotificationCandidate::sourceId)
    }

    private suspend fun scheduleEventAlarms() {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        dao.records("studio_event").forEach { record ->
            val event = JSONObject(record.json)
            val pending = eventAlarmIntent(record.entityId)
            val due = eventReminderAt(event)
            if (!eventIsActive(event) || event.optInt("reminder_enabled", 1) != 1 || due == null || !due.isAfter(LocalDateTime.now())) {
                alarmManager.cancel(pending)
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    due.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    pending,
                )
            }
        }
    }

    private fun eventAlarmIntent(eventId: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        stableId("alarm:$eventId"),
        Intent(context, EventReminderReceiver::class.java).putExtra(EXTRA_NOTIFICATION_RECORD, eventId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    @SuppressLint("MissingPermission")
    private suspend fun post(candidate: NotificationCandidate) {
        val currentUnread = dao.notificationReceipts().count { it.unread } + 1
        val notificationId = stableId(candidate.sourceId)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_NOTIFICATION_SOURCE, candidate.sourceId)
            putExtra(EXTRA_NOTIFICATION_DESTINATION, candidate.destination)
            putExtra(EXTRA_NOTIFICATION_RECORD, candidate.recordId)
        }
        val contentIntent = PendingIntent.getActivity(context, notificationId, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val dismissIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            Intent(context, NotificationDismissReceiver::class.java).putExtra(EXTRA_NOTIFICATION_SOURCE, candidate.sourceId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, candidate.channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(candidate.title)
            .setContentText(candidate.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(candidate.body))
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentIntent(contentIntent)
            .setDeleteIntent(dismissIntent)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY)
            .setNumber(currentUnread)
            .build()
        manager.notify(notificationId, notification)
        dao.putNotificationReceipt(NotificationReceipt(candidate.sourceId, candidate.fingerprint, notificationId, candidate.destination, candidate.recordId))
    }

    @SuppressLint("MissingPermission")
    private suspend fun updateSummary() {
        val count = dao.notificationReceipts().count { it.unread }
        if (count == 0 || !canNotify()) {
            manager.cancel(SUMMARY_ID)
            return
        }
        val openIntent = PendingIntent.getActivity(
            context,
            SUMMARY_ID,
            Intent(context, MainActivity::class.java).putExtra(EXTRA_NOTIFICATION_DESTINATION, "dashboard"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        manager.notify(
            SUMMARY_ID,
            NotificationCompat.Builder(context, CHANNEL_BUDDY)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText("$count item${if (count == 1) "" else "s"} need your attention")
                .setContentIntent(openIntent)
                .setAutoCancel(true)
                .setGroup(GROUP_KEY)
                .setGroupSummary(true)
                .setNumber(count)
                .build(),
        )
    }

    private fun canNotify(): Boolean = manager.areNotificationsEnabled() &&
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    private fun eventCandidate(event: JSONObject, eventId: String): NotificationCandidate? {
        if (eventId.isBlank()) return null
        val whenLabel = listOf(event.optString("event_date"), event.optString("start_time")).filter(String::isNotBlank).joinToString(" at ")
        return NotificationCandidate(
            sourceId = "sba_evt_$eventId",
            fingerprint = listOf(event.optString("updated_utc"), event.optString("event_date"), event.optString("start_time")).joinToString("|"),
            channelId = CHANNEL_SCHEDULE,
            title = event.optString("title", "Upcoming ${context.getString(R.string.app_name)} session"),
            body = listOf(whenLabel, event.optString("location")).filter(String::isNotBlank).joinToString(" - "),
            destination = "sessions",
            recordId = eventId,
        )
    }

    private fun eventIsActive(event: JSONObject): Boolean = event.optString("event_status", "scheduled") != "ended" && event.optString("event_date").isNotBlank()

    private fun eventReminderAt(event: JSONObject): LocalDateTime? = calculateEventReminderAt(
        event.optString("event_date"),
        event.optString("start_time"),
        event.optInt("reminder_lead_value", 2),
        event.optString("reminder_lead_unit", "days"),
    )

    private fun cleanSubject(value: String): String = value.replace(Regex("^\\[SR-[^]]+]\\s*"), "").ifBlank { context.getString(R.string.app_name) }
    private fun isRecent(value: String): Boolean = runCatching {
        Instant.parse(value).isAfter(Instant.now().minus(2, ChronoUnit.DAYS))
    }.getOrDefault(false)
    private fun stableId(value: String): Int = value.hashCode().and(0x7fffffff).coerceAtLeast(1)
}

internal fun calculateEventReminderAt(
    dateValue: String,
    timeValue: String,
    leadValue: Int,
    leadUnit: String,
): LocalDateTime? = runCatching {
    val date = LocalDate.parse(dateValue)
    val time = timeValue.takeIf(String::isNotBlank)?.let { LocalTime.parse(it) } ?: LocalTime.of(9, 0)
    val lead = leadValue.coerceIn(1, 999).toLong()
    when (leadUnit) {
        "hours" -> LocalDateTime.of(date, time).minusHours(lead)
        "weeks" -> LocalDateTime.of(date, time).minusWeeks(lead)
        else -> LocalDateTime.of(date, time).minusDays(lead)
    }
}.getOrNull()

class EventReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getStringExtra(EXTRA_NOTIFICATION_RECORD).orEmpty()
        if (eventId.isBlank()) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { (context.applicationContext as StudioRackApplication).repository.notifyDueEvent(eventId) }
            finally { pending.finish() }
        }
    }
}

class NotificationDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sourceId = intent.getStringExtra(EXTRA_NOTIFICATION_SOURCE).orEmpty()
        if (sourceId.isBlank()) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { (context.applicationContext as StudioRackApplication).repository.markNotificationRead(sourceId) }
            finally { pending.finish() }
        }
    }
}
