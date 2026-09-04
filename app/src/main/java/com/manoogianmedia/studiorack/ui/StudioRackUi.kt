package com.manoogianmedia.studiorack.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manoogianmedia.studiorack.data.CachedAttachment
import com.manoogianmedia.studiorack.data.CachedRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.DateFormat
import java.util.Date

private val Ink = Color(0xFF080B13)
private val Panel = Color(0xFF151A27)
private val PanelRaised = Color(0xFF202635)
private val Amber = Color(0xFFFFA300)
private val Cyan = Color(0xFF71D8FF)
private val TextSoft = Color(0xFFB9C0D3)

@Composable
fun StudioRackApp(model: StudioRackViewModel) {
    val uiState by model.uiState.collectAsState()
    MaterialTheme(colorScheme = darkColorScheme(background = Ink, surface = Panel, primary = Amber, secondary = Cyan)) {
        Surface(Modifier.fillMaxSize(), color = Ink) {
            var selectedEvent by remember { mutableStateOf<String?>(null) }
            when {
                !uiState.signedIn -> LoginScreen(model, uiState)
                selectedEvent != null -> GigModeScreen(model, selectedEvent!!) { selectedEvent = null }
                else -> DashboardScreen(model, uiState) { selectedEvent = it }
            }
        }
    }
}

@Composable
private fun LoginScreen(model: StudioRackViewModel, uiState: StudioRackUiState) {
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var mfa by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("StudioRack", color = Amber, fontSize = 38.sp, fontWeight = FontWeight.Black)
        Text("Your performance library, available offline.", color = TextSoft)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(code, { code = it }, label = { Text("StudioRack access code") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(mfa, { mfa = it.filter(Char::isDigit).take(6) }, label = { Text("Authenticator code") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(18.dp))
        Button(onClick = { model.signIn(email, code, mfa) }, enabled = !uiState.busy && email.isNotBlank() && code.isNotBlank() && mfa.length == 6) {
            Text("Connect this device")
        }
        if (uiState.busy) CircularProgressIndicator(Modifier.padding(top = 16.dp))
        if (uiState.message.isNotBlank()) Text(uiState.message, color = TextSoft, modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun DashboardScreen(model: StudioRackViewModel, uiState: StudioRackUiState, openGig: (String) -> Unit) {
    val events by model.events.collectAsState()
    val entries by model.entries.collectAsState()
    val attachments by model.attachments.collectAsState()
    val cachedAttachments by model.cachedAttachments.collectAsState()
    val state by model.syncState.collectAsState()
    val account = runCatching { JSONObject(state?.accountJson ?: "{}") }.getOrDefault(JSONObject())
    val upcoming = events.map(::recordJson).filter { it.optString("event_status") != "ended" }.sortedBy { it.optString("event_date") + it.optString("start_time") }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Spacer(Modifier.height(18.dp))
            Text(account.optString("studio_name", "StudioRack"), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(account.optString("location_name", "Offline performance workspace"), color = TextSoft)
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(state?.lastSyncAt?.let { "Synced ${DateFormat.getDateTimeInstance().format(Date(it))}" } ?: "Not synchronized", color = TextSoft, fontSize = 12.sp)
                Button(onClick = model::sync, enabled = !uiState.busy) { Text(if (uiState.busy) "Syncing" else "Sync now") }
            }
        }
        item { Text("Upcoming Sessions", color = Amber, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
        if (upcoming.isEmpty()) item { EmptyCard("No upcoming sessions are stored on this device.") }
        items(upcoming, key = { it.getString("id") }) { event ->
            val readiness = eventPacketReadiness(event, entries, attachments, cachedAttachments)
            EventCard(event, readiness) { if (event.optString("set_list_id").isNotBlank()) openGig(event.getString("id")) }
        }
        item { Spacer(Modifier.height(30.dp)) }
    }
}

@Composable
private fun EventCard(event: JSONObject, readiness: PacketReadiness, open: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = open),
        colors = CardDefaults.cardColors(containerColor = PanelRaised),
        border = BorderStroke(1.dp, Color(0xFF343B4D)),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(event.optString("event_type").uppercase(), color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(event.optString("title", "Untitled session"), color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Text(listOf(event.optString("event_date"), event.optString("start_time"), event.optString("location")).filter(String::isNotBlank).joinToString("  |  "), color = TextSoft)
            if (readiness.total > 0) {
                Text(
                    if (readiness.ready == readiness.total) "Offline packet ready (${readiness.ready} attachments)" else "Offline packet: ${readiness.ready}/${readiness.total} attachments ready",
                    color = if (readiness.ready == readiness.total) Color(0xFF63E6A4) else Cyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (event.optString("set_list_id").isNotBlank()) Text("Open Gig Mode", color = Amber, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun GigModeScreen(model: StudioRackViewModel, eventId: String, back: () -> Unit) {
    val events by model.events.collectAsState()
    val songs by model.songs.collectAsState()
    val sections by model.sections.collectAsState()
    val entries by model.entries.collectAsState()
    val attachments by model.attachments.collectAsState()
    val cachedAttachments by model.cachedAttachments.collectAsState()
    val event = events.firstOrNull { it.entityId == eventId }?.let(::recordJson) ?: JSONObject()
    val setListId = event.optString("set_list_id")
    val songMap = songs.associate { it.entityId to recordJson(it) }
    val sectionRows = sections.map(::recordJson).filter { it.optString("set_list_id") == setListId }.sortedBy { it.optInt("position") }
    val entryRows = entries.map(::recordJson).filter { it.optString("set_list_id") == setListId }.groupBy { it.optString("section_id") }
    val attachmentsBySong = attachments.map(::recordJson).groupBy { it.optString("song_id") }
    val cacheById = cachedAttachments.associateBy(CachedAttachment::attachmentId)
    var viewing by remember { mutableStateOf<GigAttachment?>(null) }
    viewing?.let { selected ->
        OfflineAttachmentViewer(selected, close = { viewing = null })
        return
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = back, colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Ink)) { Text("Back") }
                Column(Modifier.padding(start = 14.dp)) {
                    Text("GIG MODE", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text(event.optString("title", "Set List"), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(event.optString("location"), color = TextSoft)
                }
            }
        }
        sectionRows.forEach { section ->
            item { Text(section.optString("name", "Set"), color = Amber, fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) }
            items(entryRows[section.optString("id")].orEmpty().sortedBy { it.optInt("position") }) { entry ->
                val song = songMap[entry.optString("song_id")]
                val attachment = selectPerformanceAttachment(entry, attachmentsBySong[entry.optString("song_id")].orEmpty())
                val cached = attachment?.optString("id")?.let(cacheById::get)
                SongRow(entry, song, attachment, cached) {
                    if (attachment != null && cached?.status == "ready" && cached.localPath != null) {
                        viewing = GigAttachment(song, attachment, cached)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun SongRow(entry: JSONObject, song: JSONObject?, attachment: JSONObject?, cached: CachedAttachment?, openAttachment: () -> Unit) {
    val availableOffline = cached?.status == "ready" && cached.localPath != null
    Card(
        colors = CardDefaults.cardColors(containerColor = PanelRaised),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth().then(if (availableOffline) Modifier.clickable(onClick = openAttachment) else Modifier),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(song?.optString("title")?.takeIf(String::isNotBlank) ?: entry.optString("manual_title", "Untitled"), color = Amber, fontSize = 23.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(listOf(song?.optString("tempo"), song?.optString("time_signature")).filterNotNull().filter(String::isNotBlank).joinToString("  "), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Text(listOf(song?.optString("artist"), song?.optString("starts_by"), song?.optString("style")).filterNotNull().filter(String::isNotBlank).joinToString("  |  "), color = TextSoft)
            val patch = listOf(song?.optString("patch_name"), song?.optString("patch_number")).filterNotNull().filter(String::isNotBlank).joinToString(" / ")
            if (patch.isNotBlank()) Text("Patch: $patch", color = Cyan, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            if (attachment != null) {
                Text(
                    when {
                        availableOffline -> "Open ${attachmentLabel(attachment)} offline"
                        cached?.status == "failed" -> "${attachmentLabel(attachment)} could not be cached"
                        cached?.status == "remote_only" -> "${attachmentLabel(attachment)} requires internet"
                        else -> "${attachmentLabel(attachment)} is not cached yet"
                    },
                    color = if (availableOffline) Amber else TextSoft,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun OfflineAttachmentViewer(item: GigAttachment, close: () -> Unit) {
    val path = item.cache.localPath.orEmpty()
    val isPdf = item.cache.mimeType == "application/pdf" || path.endsWith(".pdf", true)
    val pageCount = remember(path, isPdf) { if (isPdf) pdfPageCount(path) else 1 }
    var page by remember(path) { mutableIntStateOf(0) }
    val rendered by produceState(initialValue = AttachmentRender(), path, page) {
        val bitmap = withContext(Dispatchers.IO) {
            if (isPdf) renderPdfPage(path, page) else decodeAttachmentImage(path)
        }
        value = AttachmentRender(bitmap = bitmap, complete = true)
    }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = close, colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Ink)) { Text("Back") }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(item.song?.optString("title", item.attachment.optString("display_name")) ?: item.attachment.optString("display_name"), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(attachmentLabel(item.attachment) + if (pageCount > 1) "  |  Page ${page + 1} of $pageCount" else "", color = TextSoft, fontSize = 12.sp)
            }
            if (pageCount > 1) {
                Button(onClick = { page = (page - 1).coerceAtLeast(0) }, enabled = page > 0) { Text("<") }
                Spacer(Modifier.size(6.dp))
                Button(onClick = { page = (page + 1).coerceAtMost(pageCount - 1) }, enabled = page < pageCount - 1) { Text(">") }
            }
        }
        Box(Modifier.fillMaxSize().padding(top = 10.dp), contentAlignment = Alignment.Center) {
            val renderedBitmap = rendered.bitmap
            when {
                !rendered.complete -> CircularProgressIndicator()
                renderedBitmap == null -> Text("This attachment is cached, but Android cannot display its file format.", color = TextSoft)
                else -> Image(renderedBitmap.asImageBitmap(), contentDescription = attachmentLabel(item.attachment), modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
        }
    }
}

@Composable
private fun EmptyCard(text: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) { Text(text, color = TextSoft) }
}

private fun recordJson(record: CachedRecord): JSONObject = runCatching { JSONObject(record.json) }.getOrDefault(JSONObject())

private data class GigAttachment(val song: JSONObject?, val attachment: JSONObject, val cache: CachedAttachment)
private data class PacketReadiness(val ready: Int, val total: Int)
private data class AttachmentRender(val bitmap: Bitmap? = null, val complete: Boolean = false)

private fun eventPacketReadiness(
    event: JSONObject,
    entries: List<CachedRecord>,
    attachments: List<CachedRecord>,
    cached: List<CachedAttachment>,
): PacketReadiness {
    val setListId = event.optString("set_list_id")
    if (setListId.isBlank()) return PacketReadiness(0, 0)
    val bySong = attachments.map(::recordJson).groupBy { it.optString("song_id") }
    val cacheById = cached.associateBy(CachedAttachment::attachmentId)
    val selectedIds = entries.asSequence()
        .map(::recordJson)
        .filter { it.optString("set_list_id") == setListId }
        .mapNotNull { entry -> selectPerformanceAttachment(entry, bySong[entry.optString("song_id")].orEmpty())?.optString("id") }
        .filter(String::isNotBlank)
        .distinct()
        .toList()
    return PacketReadiness(selectedIds.count { cacheById[it]?.status == "ready" }, selectedIds.size)
}

private fun selectPerformanceAttachment(entry: JSONObject, attachments: List<JSONObject>): JSONObject? {
    if (attachments.isEmpty()) return null
    val overrideId = entry.optString("performance_attachment_id")
    if (overrideId.isNotBlank()) attachments.firstOrNull { it.optString("id") == overrideId }?.let { return it }
    return attachments.firstOrNull { it.optInt("is_gig_default") == 1 }
        ?: attachments.minByOrNull { it.optInt("position", Int.MAX_VALUE) }
}

private fun attachmentLabel(attachment: JSONObject): String = attachment.optString("display_name").ifBlank {
    attachment.optString("attachment_type", "attachment").replace('_', ' ').replaceFirstChar(Char::uppercase)
}

private fun pdfPageCount(path: String): Int = runCatching {
    ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
        PdfRenderer(descriptor).use { renderer -> renderer.pageCount }
    }
}.getOrDefault(0)

private fun renderPdfPage(path: String, pageIndex: Int): Bitmap? = runCatching {
    ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
        PdfRenderer(descriptor).use { renderer ->
            renderer.openPage(pageIndex.coerceIn(0, renderer.pageCount - 1)).use { page ->
                val width = 1800
                val height = (width.toFloat() / page.width * page.height).toInt().coerceAtLeast(1)
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                }
            }
        }
    }
}.getOrNull()

private fun decodeAttachmentImage(path: String): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    var sample = 1
    while (bounds.outWidth / sample > 2400 || bounds.outHeight / sample > 3200) sample *= 2
    return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
}
