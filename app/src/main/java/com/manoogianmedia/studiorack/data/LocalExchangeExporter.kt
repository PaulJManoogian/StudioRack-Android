package com.manoogianmedia.studiorack.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

internal data class LocalExportStore(
    val entities: Map<String, List<JSONObject>>,
    val supporting: Map<String, List<JSONObject>>,
)

internal class LocalExchangeExporter(
    private val dao: StudioRackDao,
    private val productName: String,
    private val publisherName: String,
    private val filePrefix: String,
) {
    suspend fun export(kind: String, format: String, ids: List<String>): DataExport {
        val entityTypes = listOf(
            "song", "song_attachment", "set_list", "set_list_section", "set_list_entry",
            "studio_event", "studio_event_kit",
        )
        val supportingTypes = listOf(
            "item", "item_spec", "item_unit", "kit", "kit_member", "kit_member_unit",
            "brand", "category", "item_type", "location", "kit_designation",
        )
        val store = LocalExportStore(
            entities = entityTypes.associateWith { type -> dao.records(type).map(::recordJson) },
            supporting = supportingTypes.associateWith { type -> dao.supporting(type).map(::supportingJson) },
        )
        return export(kind, format, ids, store)
    }

    internal fun export(
        kind: String,
        format: String,
        ids: List<String>,
        store: LocalExportStore,
        exportDate: LocalDate = LocalDate.now(),
    ): DataExport {
        require(kind in setOf("songs", "setlists", "items", "kits", "events")) { "Unsupported export type." }
        require(format in setOf("csv", "json", "xml", "xls")) { "Unsupported export format." }
        val records = select(buildRecords(kind, store), ids)
        val (bytes, mimeType) = serialize(kind, records, format)
        return DataExport("$filePrefix-$kind-$exportDate.$format", mimeType, bytes)
    }

    private fun buildRecords(kind: String, store: LocalExportStore): List<JSONObject> = when (kind) {
        "songs" -> store.entities.rows("song")
            .sortedWith(compareBy({ it.text("title").lowercase() }, { it.text("artist").lowercase() }))
            .map { song ->
                copy(song).put(
                    "attachments",
                    JSONArray(store.entities.rows("song_attachment")
                        .filter { it.text("song_id") == song.text("id") }
                        .sortedBy { it.number("position") }),
                )
            }
        "setlists" -> setListRecords(store)
        "items" -> itemRecords(store)
        "kits" -> kitRecords(store)
        else -> eventRecords(store)
    }

    private fun setListRecords(store: LocalExportStore): List<JSONObject> {
        val songs = store.entities.rows("song").associateBy { it.text("id") }
        val sections = store.entities.rows("set_list_section")
        val entries = store.entities.rows("set_list_entry")
        return store.entities.rows("set_list").sortedBy { it.text("name").lowercase() }.map { setList ->
            val setListId = setList.text("id")
            val sectionRows = sections.filter { it.text("set_list_id") == setListId }.sortedBy { it.number("position") }.map { section ->
                val sectionId = section.text("id")
                val entryRows = entries.filter {
                    it.text("set_list_id") == setListId && it.text("section_id") == sectionId
                }.sortedBy { it.number("position") }.map { entry ->
                    val output = copy(entry)
                    val song = songs[entry.text("song_id")]
                    if (song != null) {
                        mapOf(
                            "song_title" to "title", "song_artist" to "artist", "song_tempo" to "tempo",
                            "song_duration_seconds" to "duration_seconds", "song_time_signature" to "time_signature",
                            "song_style" to "style", "song_starts_by" to "starts_by",
                            "song_patch_name" to "patch_name", "song_patch_number" to "patch_number",
                            "song_notes" to "notes",
                        ).forEach { (target, source) -> output.put(target, song.opt(source)) }
                    }
                    output.remove("performance_attachment_id")
                    output
                }
                copy(section).put("entries", JSONArray(entryRows))
            }
            copy(setList).put("sections", JSONArray(sectionRows))
        }
    }

    private fun itemRecords(store: LocalExportStore): List<JSONObject> {
        val brands = store.supporting.names("brand")
        val categories = store.supporting.names("category")
        val types = store.supporting.names("item_type")
        val locations = store.supporting.names("location")
        val specs = store.supporting.rows("item_spec")
        val units = store.supporting.rows("item_unit")
        return store.supporting.rows("item").sortedBy { it.text("display_name").lowercase() }.map { item ->
            val id = item.text("id")
            val specObject = JSONObject()
            specs.filter { it.text("item_id") == id }.forEach { specObject.put(it.text("key"), it.opt("value")) }
            copy(item)
                .put("brand_name", brands[item.text("brand_id")].orEmpty())
                .put("category_name", categories[item.text("category_id")].orEmpty())
                .put("type_name", types[item.text("type_id")].orEmpty())
                .put("location_name", locations[item.text("default_location_id")].orEmpty())
                .put("specs", specObject)
                .put("units", JSONArray(units.filter { it.text("item_id") == id }))
        }
    }

    private fun kitRecords(store: LocalExportStore): List<JSONObject> {
        val locations = store.supporting.names("location")
        val designations = store.supporting.names("kit_designation")
        val items = store.supporting.rows("item").associateBy { it.text("id") }
        val brands = store.supporting.names("brand")
        val units = store.supporting.rows("item_unit").associateBy { it.text("id") }
        val members = store.supporting.rows("kit_member")
        val unitMembers = store.supporting.rows("kit_member_unit")
        return store.supporting.rows("kit").sortedBy { it.text("name").lowercase() }.map { kit ->
            val kitId = kit.text("id")
            val memberRows = members.filter { it.text("kit_id") == kitId }.map { member ->
                val item = items[member.text("item_id")]
                copy(member)
                    .put("item_name", item?.text("display_name").orEmpty())
                    .put("brand_name", brands[item?.text("brand_id").orEmpty()].orEmpty())
            }
            val unitMemberRows = unitMembers.filter { it.text("kit_id") == kitId }.map { member ->
                val unit = units[member.text("item_unit_id")]
                val item = items[unit?.text("item_id")]
                copy(member)
                    .put("item_id", unit?.opt("item_id"))
                    .put("unit_label", unit?.opt("unit_label"))
                    .put("serial_number", unit?.opt("serial_number"))
                    .put("item_name", item?.text("display_name").orEmpty())
                    .put("brand_name", brands[item?.text("brand_id").orEmpty()].orEmpty())
            }
            copy(kit)
                .put("location_name", locations[kit.text("location_id")].orEmpty())
                .put("designation_name", designations[kit.text("designation_id")].orEmpty())
                .put("members", JSONArray(memberRows))
                .put("unit_members", JSONArray(unitMemberRows))
        }
    }

    private fun eventRecords(store: LocalExportStore): List<JSONObject> {
        val setLists = store.entities.rows("set_list").associateBy { it.text("id") }
        val kits = store.supporting.rows("kit").associateBy { it.text("id") }
        val eventKits = store.entities.rows("studio_event_kit")
        return store.entities.rows("studio_event")
            .sortedWith(compareBy({ it.text("event_date") }, { it.text("start_time") }, { it.text("title").lowercase() }))
            .map { event ->
                val kitNames = eventKits.filter { it.text("event_id") == event.text("id") }
                    .mapNotNull { kits[it.text("kit_id")]?.text("name")?.takeIf(String::isNotBlank) }
                copy(event)
                    .put("set_list_name", setLists[event.text("set_list_id")]?.text("name").orEmpty())
                    .put("kit_summary", kitNames.joinToString(", "))
            }
    }

    private fun select(records: List<JSONObject>, ids: List<String>): List<JSONObject> {
        if (ids.isEmpty()) return records
        val byId = records.associateBy { it.text("id") }
        return ids.mapNotNull(byId::get)
    }

    private fun serialize(kind: String, records: List<JSONObject>, format: String): Pair<ByteArray, String> {
        if (format == "json") {
            val payload = JSONObject()
                .put("product", productName)
                .put("format_version", 1)
                .put("resource", kind)
                .put("records", JSONArray(records))
            return payload.toString(2).toByteArray(Charsets.UTF_8) to "application/json"
        }
        val (headers, rows) = flatten(kind, records)
        if (format == "csv") {
            val lines = buildList {
                add(headers.joinToString(",", transform = ::csvCell))
                rows.forEach { row -> add(headers.joinToString(",") { csvCell(row[it].orEmpty()) }) }
            }
            return ("\uFEFF" + lines.joinToString("\r\n") + "\r\n").toByteArray(Charsets.UTF_8) to "text/csv; charset=utf-8"
        }
        if (format == "xml") {
            val body = records.joinToString("") { "<record>${xml(it.toString())}</record>" }
            val output = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><application-export product=\"${xml(productName)}\" version=\"1\" resource=\"${xml(kind)}\">$body</application-export>"
            return output.toByteArray(Charsets.UTF_8) to "application/xml"
        }
        val table = buildList { add(headers); rows.forEach { row -> add(headers.map { row[it].orEmpty() }) } }
            .joinToString("") { values -> "<Row>" + values.joinToString("") { "<Cell><Data ss:Type=\"String\">${xml(it)}</Data></Cell>" } + "</Row>" }
        val output = "<?xml version=\"1.0\"?><?mso-application progid=\"Excel.Sheet\"?>" +
            "<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">" +
            "<DocumentProperties xmlns=\"urn:schemas-microsoft-com:office:office\"><Title>${xml(productName)} ${xml(kind.replaceFirstChar(Char::uppercase))} Export</Title><Author>${xml(publisherName)}</Author><Company>${xml(publisherName)}</Company></DocumentProperties>" +
            "<Worksheet ss:Name=\"${xml(kind.replaceFirstChar(Char::uppercase))}\"><Table>$table</Table></Worksheet></Workbook>"
        return output.toByteArray(Charsets.UTF_8) to "application/vnd.ms-excel"
    }

    private fun flatten(kind: String, records: List<JSONObject>): Pair<List<String>, List<Map<String, String>>> {
        val headers = when (kind) {
            "songs" -> listOf("title", "artist", "tempo", "duration", "time_signature", "style", "starts_by", "patch_name", "patch_number", "media_ref", "notes", "favorite")
            "setlists" -> listOf("set_list", "description", "set", "set_number", "song_number", "song", "artist", "tempo", "duration", "time_signature", "style", "starts_by", "patch_name", "patch_number", "set_note", "song_note", "set_list_notes", "favorite")
            "items" -> listOf("display_name", "brand", "category", "type", "location", "usage_status", "quantity", "notes")
            "kits" -> listOf("name", "designation", "location", "members", "notes")
            else -> listOf("title", "type", "status", "date", "start_time", "location", "set_list", "kits", "notes")
        }
        val output = mutableListOf<Map<String, String>>()
        records.forEach { record ->
            when (kind) {
                "songs" -> output += mapOf(
                    "title" to record.text("title"), "artist" to record.text("artist"), "tempo" to record.text("tempo"),
                    "duration" to duration(record.opt("duration_seconds")), "time_signature" to record.text("time_signature"),
                    "style" to record.text("style"), "starts_by" to record.text("starts_by"), "patch_name" to record.text("patch_name"),
                    "patch_number" to record.text("patch_number"), "media_ref" to record.text("media_ref"), "notes" to record.text("notes"),
                    "favorite" to record.text("is_favorite"),
                )
                "setlists" -> flattenSetList(record, output)
                "items" -> output += mapOf(
                    "display_name" to record.text("display_name"), "brand" to record.text("brand_name"),
                    "category" to record.text("category_name"), "type" to record.text("type_name"),
                    "location" to record.text("location_name"), "usage_status" to record.text("usage_status"),
                    "quantity" to record.text("quantity"), "notes" to record.text("notes"),
                )
                "kits" -> output += mapOf(
                    "name" to record.text("name"), "designation" to record.text("designation_name"),
                    "location" to record.text("location_name"),
                    "members" to (record.optJSONArray("members")?.length().orZero() + record.optJSONArray("unit_members")?.length().orZero()).toString(),
                    "notes" to record.text("notes"),
                )
                else -> output += mapOf(
                    "title" to record.text("title"), "type" to record.text("event_type"), "status" to record.text("event_status"),
                    "date" to record.text("event_date"), "start_time" to record.text("start_time"), "location" to record.text("location"),
                    "set_list" to record.text("set_list_name"), "kits" to record.text("kit_summary"), "notes" to record.text("notes"),
                )
            }
        }
        return headers to output
    }

    private fun flattenSetList(record: JSONObject, output: MutableList<Map<String, String>>) {
        val sections = record.optJSONArray("sections") ?: JSONArray().put(JSONObject())
        for (sectionIndex in 0 until sections.length()) {
            val section = sections.optJSONObject(sectionIndex) ?: JSONObject()
            val entries = section.optJSONArray("entries") ?: JSONArray().put(JSONObject())
            for (entryIndex in 0 until entries.length()) {
                val entry = entries.optJSONObject(entryIndex) ?: JSONObject()
                output += mapOf(
                    "set_list" to record.text("name"), "description" to record.text("description"), "set" to section.text("name"),
                    "set_number" to if (section.length() > 0) (sectionIndex + 1).toString() else "",
                    "song_number" to if (entry.length() > 0) (entryIndex + 1).toString() else "",
                    "song" to entry.text("song_title").ifBlank { entry.text("manual_title") }, "artist" to entry.text("song_artist"),
                    "tempo" to entry.text("song_tempo"), "duration" to duration(entry.opt("song_duration_seconds")),
                    "time_signature" to entry.text("song_time_signature"), "style" to entry.text("song_style"),
                    "starts_by" to entry.text("song_starts_by"), "patch_name" to entry.text("song_patch_name"),
                    "patch_number" to entry.text("song_patch_number"), "set_note" to entry.text("entry_notes"),
                    "song_note" to entry.text("song_notes"), "set_list_notes" to record.text("notes"), "favorite" to record.text("is_favorite"),
                )
            }
        }
    }
}

private fun recordJson(record: CachedRecord) = JSONObject(record.json).put("id", record.entityId)
private fun supportingJson(record: SupportingRecord) = JSONObject(record.json).put("id", record.entityId)
private fun Map<String, List<JSONObject>>.rows(type: String) = get(type).orEmpty()
private fun Map<String, List<JSONObject>>.names(type: String) = rows(type).associate { it.text("id") to it.text("name") }
private fun JSONObject.text(key: String): String = if (isNull(key)) "" else opt(key)?.toString().orEmpty()
private fun JSONObject.number(key: String): Double = text(key).toDoubleOrNull() ?: 0.0
private fun copy(value: JSONObject) = JSONObject(value.toString())
private fun Int?.orZero() = this ?: 0
private fun duration(value: Any?): String {
    val seconds = when (value) {
        is Number -> value.toInt()
        else -> value?.toString()?.toIntOrNull() ?: 0
    }
    return if (seconds > 0) "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}" else ""
}
private fun csvCell(value: String): String = if (value.any { it == ',' || it == '"' || it == '\r' || it == '\n' }) {
    "\"${value.replace("\"", "\"\"")}\""
} else value
private fun xml(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")
