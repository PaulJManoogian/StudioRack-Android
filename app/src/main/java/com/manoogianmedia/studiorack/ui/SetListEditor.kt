package com.manoogianmedia.studiorack.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.rememberScrollState
import com.manoogianmedia.studiorack.data.CachedRecord
import org.json.JSONObject
import java.util.UUID

private val EditorInk = Color(0xFF07090F)
private val EditorPanel = Color(0xFF121621)
private val EditorRaised = Color(0xFF202635)
private val EditorAmber = Color(0xFFFF9D1E)
private val EditorCyan = Color(0xFF42D9FF)
private val EditorSoft = Color(0xFFAEB8CB)

@Composable
internal fun SetListEditor(
    original: CachedRecord?,
    allSections: List<CachedRecord>,
    allEntries: List<CachedRecord>,
    songs: List<CachedRecord>,
    attachments: List<CachedRecord>,
    model: StudioRackViewModel,
    close: () -> Unit,
) {
    var draft by remember(original?.entityId, allSections, allEntries) {
        mutableStateOf(setListDraft(original, allSections, allEntries))
    }
    var pickingSection by remember { mutableStateOf<String?>(null) }
    var removedEntry by remember { mutableStateOf<RemovedSetEntry?>(null) }
    val songRows = songs.associate { it.entityId to JSONObject(it.json) }
    val attachmentsBySong = attachments.groupBy { JSONObject(it.json).optString("song_id") }
    val estimatedSeconds = draft.sections.sumOf { section ->
        section.entries.sumOf { entry -> entry.songId?.let { songRows[it]?.optInt("duration_seconds") } ?: 0 }
    }

    Surface(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), color = EditorInk) {
        LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = close, border = BorderStroke(1.dp, EditorAmber)) { Text("Back", color = EditorAmber) }
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text("SET LIST BUILDER", color = EditorAmber, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Text(if (original == null) "Create Set List" else "Edit Set List", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            removedEntry?.let { removed ->
                item {
                    Surface(color = Color(0xFF2B2023), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFFFF7A82).copy(alpha = .55f))) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Removed ${removed.label}", color = Color.White, modifier = Modifier.weight(1f), maxLines = 1)
                            TextButton(onClick = {
                                draft = draft.restoreEntry(removed.sectionId, removed.index, removed.entry)
                                removedEntry = null
                            }) { Text("Undo", color = EditorAmber, fontWeight = FontWeight.Black) }
                        }
                    }
                }
            }
            item { DictationTextField(draft.name, { draft = draft.copy(name = it) }, "Set list name") }
            item { DictationTextField(draft.description, { draft = draft.copy(description = it) }, "Description") }
            item { DictationTextField(draft.notes, { draft = draft.copy(notes = it) }, "Set list notes", singleLine = false, minLines = 2) }
            if (estimatedSeconds > 0) item {
                Surface(color = EditorAmber.copy(alpha = 0.12f), shape = RoundedCornerShape(50), border = BorderStroke(1.dp, EditorAmber.copy(alpha = 0.35f))) {
                    Text("Estimated music time  ${formatDuration(estimatedSeconds)}", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("PRINTED SONG ATTACHMENTS", color = EditorSoft, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("none" to "None", "performance" to "Performance", "all_marked" to "All Marked").forEach { (value, label) ->
                            Surface(
                                color = if (draft.attachmentPrintMode == value) EditorAmber else EditorRaised,
                                contentColor = if (draft.attachmentPrintMode == value) EditorInk else Color.White,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.weight(1f).clickable { draft = draft.copy(attachmentPrintMode = value) },
                            ) { Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { draft = draft.copy(favorite = !draft.favorite) }) {
                        Checkbox(draft.favorite, { draft = draft.copy(favorite = it) })
                        Text("Favorite set list", color = Color.White)
                    }
                }
            }
            if (draft.sections.isEmpty()) item {
                Card(colors = CardDefaults.cardColors(containerColor = EditorPanel), shape = RoundedCornerShape(8.dp)) {
                    Text("No sets yet. Add a set, then choose songs in performance order.", color = EditorSoft, modifier = Modifier.padding(18.dp))
                }
            }
            items(draft.sections, key = { it.id }) { section ->
                val sectionIndex = draft.sections.indexOfFirst { it.id == section.id }
                Card(colors = CardDefaults.cardColors(containerColor = EditorPanel), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFF30384A))) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("SET ${sectionIndex + 1}", color = EditorAmber, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                            TextButton(onClick = { draft = draft.copy(sections = draft.sections.filterNot { it.id == section.id }) }) { Text("Remove", color = Color(0xFFFF7A82)) }
                        }
                        DictationTextField(section.name, { value -> draft = draft.updateSection(section.id) { it.copy(name = value) } }, "Set name")
                        DictationTextField(section.notes, { value -> draft = draft.updateSection(section.id) { it.copy(notes = value) } }, "Set notes", singleLine = false, minLines = 2)
                        section.entries.forEachIndexed { index, entry ->
                            val song = entry.songId?.let(songRows::get)
                            val label = song?.optString("title")?.takeIf(String::isNotBlank) ?: entry.manualTitle.ifBlank { "item" }
                            var dragDistance by remember(entry.id) { mutableFloatStateOf(0f) }
                            val dismissState = rememberSwipeToDismissBoxState(confirmValueChange = { value ->
                                if (value != SwipeToDismissBoxValue.Settled) {
                                    removedEntry = RemovedSetEntry(section.id, index, entry, label)
                                    draft = draft.removeEntry(section.id, entry.id)
                                }
                                false
                            })
                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    Row(
                                        Modifier.fillMaxSize().background(Color(0xFF7D2930), RoundedCornerShape(8.dp)).padding(horizontal = 18.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Arrangement.Start else Arrangement.End,
                                    ) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Remove $label", tint = Color.White)
                                        Text(" Remove", color = Color.White, fontWeight = FontWeight.Black)
                                    }
                                },
                            ) {
                            Column(
                                Modifier.fillMaxWidth().background(EditorPanel).padding(vertical = 4.dp).pointerInput(section.id, entry.id, index) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { dragDistance = 0f },
                                        onDragCancel = { dragDistance = 0f },
                                        onDragEnd = { dragDistance = 0f },
                                    ) { change, amount ->
                                        change.consume()
                                        dragDistance += amount.y
                                        val threshold = 52.dp.toPx()
                                        when {
                                            dragDistance <= -threshold && index > 0 -> {
                                                draft = draft.moveEntry(section.id, index, -1)
                                                dragDistance = 0f
                                            }
                                            dragDistance >= threshold && index < section.entries.lastIndex -> {
                                                draft = draft.moveEntry(section.id, index, 1)
                                                dragDistance = 0f
                                            }
                                        }
                                    }
                                }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${index + 1}", color = EditorCyan, fontWeight = FontWeight.Black, modifier = Modifier.padding(end = 10.dp))
                                    Column(Modifier.weight(1f)) {
                                        if (song == null) {
                                            DictationTextField(
                                                entry.manualTitle,
                                                { value -> draft = draft.updateEntry(section.id, entry.id) { it.copy(manualTitle = value) } },
                                                "Manual song, break, or note",
                                            )
                                        } else {
                                            Text(song.optString("title", "Untitled"), color = Color.White, fontWeight = FontWeight.Bold)
                                            song.optString("artist").takeIf(String::isNotBlank)?.let { Text(it, color = EditorSoft, fontSize = 12.sp) }
                                        }
                                    }
                                    Icon(Icons.Rounded.DragHandle, contentDescription = "Hold and drag to reorder $label", tint = EditorAmber, modifier = Modifier.size(34.dp))
                                }
                                DictationTextField(entry.notes, { value -> draft = draft.updateEntry(section.id, entry.id) { it.copy(notes = value) } }, "Notation for this set", singleLine = false, minLines = 2)
                                val songAttachments = entry.songId?.let { attachmentsBySong[it] }.orEmpty()
                                if (songAttachments.isNotEmpty()) {
                                    Text("PERFORMANCE ATTACHMENT", color = EditorSoft, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 6.dp))
                                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        val choices = listOf(null to "Song Default") + songAttachments.map { record -> record.entityId to attachmentName(JSONObject(record.json)) }
                                        choices.forEach { (id, label) ->
                                            val active = entry.performanceAttachmentId == id
                                            Surface(
                                                color = if (active) EditorAmber else EditorRaised,
                                                contentColor = if (active) EditorInk else Color.White,
                                                shape = RoundedCornerShape(50),
                                                modifier = Modifier.clickable { draft = draft.updateEntry(section.id, entry.id) { it.copy(performanceAttachmentId = id) } },
                                            ) { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) }
                                        }
                                    }
                                }
                            }
                            }
                            HorizontalDivider(color = Color(0xFF30384A))
                        }
                        StudioButton(onClick = { pickingSection = section.id }) { Text("Choose Songs", color = EditorInk, fontWeight = FontWeight.Black) }
                        OutlinedButton(onClick = {
                            draft = draft.updateSection(section.id) { it.copy(entries = it.entries + SetEntryDraft(newId("sle"))) }
                        }, border = BorderStroke(1.dp, EditorCyan)) { Text("Add Manual Entry", color = EditorCyan) }
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { draft = draft.copy(sections = draft.sections + SetSectionDraft(newId("sls"), "Set ${draft.sections.size + 1}")) },
                    border = BorderStroke(1.dp, EditorAmber), modifier = Modifier.fillMaxWidth(),
                ) { Text("Add Set", color = EditorAmber, fontWeight = FontWeight.Bold) }
            }
            item {
                StudioButton(
                    onClick = { model.saveSetList(draft, close) }, enabled = draft.name.isNotBlank() && draft.sections.all { section -> section.entries.all { it.songId != null || it.manualTitle.isNotBlank() } },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Save Set List", color = EditorInk, fontWeight = FontWeight.Black) }
                if (original != null) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { model.deleteSetList(draft.id, close) }, border = BorderStroke(1.dp, Color(0xFFFF7A82)), modifier = Modifier.fillMaxWidth()) { Text("Delete Set List", color = Color(0xFFFF7A82)) }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    pickingSection?.let { sectionId ->
        val section = draft.sections.firstOrNull { it.id == sectionId }
        if (section != null) SongPicker(section, songs, close = { pickingSection = null }) { songId, checked ->
            draft = draft.updateSection(sectionId) { current ->
                val entries = if (checked) current.entries + SetEntryDraft(newId("sle"), songId = songId)
                else current.entries.filterNot { it.songId == songId }
                current.copy(entries = entries)
            }
        }
    }
}

@Composable
private fun SongPicker(section: SetSectionDraft, songs: List<CachedRecord>, close: () -> Unit, toggle: (String, Boolean) -> Unit) {
    var query by remember { mutableStateOf("") }
    val chosen = section.entries.mapNotNull(SetEntryDraft::songId)
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(12.dp), color = EditorInk, shape = RoundedCornerShape(8.dp)) {
            LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("CHOOSE SONGS", color = EditorAmber, fontWeight = FontWeight.Black)
                            Text(section.name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        StudioButton(onClick = close) { Text("Done", color = EditorInk, fontWeight = FontWeight.Black) }
                    }
                }
                item { DictationTextField(query, { query = it }, "Find a song") }
                items(songs.filter { query.isBlank() || it.json.contains(query, true) }, key = { it.entityId }) { record ->
                    val song = JSONObject(record.json)
                    val selected = record.entityId in chosen
                    Row(
                        Modifier.fillMaxWidth().clickable { toggle(record.entityId, !selected) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(chosen.indexOf(record.entityId).takeIf { it >= 0 }?.plus(1)?.toString().orEmpty(), color = EditorAmber, fontWeight = FontWeight.Black, modifier = Modifier.size(28.dp))
                        Checkbox(selected, null)
                        Column(Modifier.weight(1f)) {
                            Text(song.optString("title", "Untitled"), color = Color.White, fontWeight = FontWeight.Bold)
                            Text(listOf(song.optString("artist"), song.optString("style"), song.optString("tempo")).filter(String::isNotBlank).joinToString("  |  "), color = EditorSoft, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun setListDraft(original: CachedRecord?, allSections: List<CachedRecord>, allEntries: List<CachedRecord>): SetListDraft {
    val id = original?.entityId ?: newId("setlist")
    val root = original?.let { JSONObject(it.json) } ?: JSONObject()
    val sections = allSections.filter { JSONObject(it.json).optString("set_list_id") == id }.sortedBy { JSONObject(it.json).optInt("position") }.map { sectionRecord ->
        val section = JSONObject(sectionRecord.json)
        val entries = allEntries.filter { JSONObject(it.json).optString("section_id") == sectionRecord.entityId }.sortedBy { JSONObject(it.json).optInt("position") }.map { entryRecord ->
            val entry = JSONObject(entryRecord.json)
            SetEntryDraft(
                entryRecord.entityId,
                entry.optString("song_id").takeIf(String::isNotBlank),
                entry.optString("manual_title"),
                entry.optString("entry_notes"),
                entry.optString("performance_attachment_id").takeIf(String::isNotBlank),
            )
        }
        SetSectionDraft(sectionRecord.entityId, section.optString("name"), section.optString("notes"), entries)
    }
    return SetListDraft(id, root.optString("name"), root.optString("description"), root.optString("notes"), root.optString("attachment_print_mode", "none"), root.optInt("is_favorite") == 1, sections)
}

private fun SetListDraft.updateSection(id: String, transform: (SetSectionDraft) -> SetSectionDraft) = copy(sections = sections.map { if (it.id == id) transform(it) else it })
private fun SetListDraft.updateEntry(sectionId: String, entryId: String, transform: (SetEntryDraft) -> SetEntryDraft) = updateSection(sectionId) { section -> section.copy(entries = section.entries.map { if (it.id == entryId) transform(it) else it }) }
private fun SetListDraft.removeEntry(sectionId: String, entryId: String) = updateSection(sectionId) { section -> section.copy(entries = section.entries.filterNot { it.id == entryId }) }
private fun SetListDraft.restoreEntry(sectionId: String, index: Int, entry: SetEntryDraft) = updateSection(sectionId) { section ->
    section.copy(entries = section.entries.toMutableList().apply { add(index.coerceIn(0, size), entry) })
}
private fun SetListDraft.moveEntry(sectionId: String, index: Int, delta: Int) = updateSection(sectionId) { section ->
    val target = index + delta
    if (index !in section.entries.indices || target !in section.entries.indices) section else section.copy(entries = section.entries.toMutableList().apply { add(target, removeAt(index)) })
}
private fun newId(prefix: String) = "${prefix}_${UUID.randomUUID().toString().replace("-", "").take(12)}"
private fun attachmentName(data: JSONObject) = data.optString("display_name").ifBlank { data.optString("attachment_type", "Attachment").replace('_', ' ').replaceFirstChar(Char::uppercase) }
private data class RemovedSetEntry(val sectionId: String, val index: Int, val entry: SetEntryDraft, val label: String)
