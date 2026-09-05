package com.manoogianmedia.studiorack.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.manoogianmedia.studiorack.data.CachedAttachment
import com.manoogianmedia.studiorack.data.CachedRecord
import com.manoogianmedia.studiorack.data.SupportingRecord
import com.manoogianmedia.studiorack.data.cacheImageFile
import com.manoogianmedia.studiorack.performance.NativeMetronome
import com.manoogianmedia.studiorack.performance.PedalAction
import com.manoogianmedia.studiorack.performance.PerformanceSettings
import com.manoogianmedia.studiorack.performance.mappedPedalAction
import com.manoogianmedia.studiorack.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import java.text.DateFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Date

private val Ink = Color(0xFF07090F)
private val Panel = Color(0xFF121621)
private val PanelRaised = Color(0xFF202635)
private val Amber = Color(0xFFFF9D1E)
private val Cyan = Color(0xFF42D9FF)
private val TextSoft = Color(0xFFAEB8CB)
private val StudioFont = FontFamily(Font(R.font.inter))
private val StudioTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = StudioFont), displayMedium = displayMedium.copy(fontFamily = StudioFont),
        headlineLarge = headlineLarge.copy(fontFamily = StudioFont), headlineMedium = headlineMedium.copy(fontFamily = StudioFont),
        titleLarge = titleLarge.copy(fontFamily = StudioFont), titleMedium = titleMedium.copy(fontFamily = StudioFont),
        bodyLarge = bodyLarge.copy(fontFamily = StudioFont), bodyMedium = bodyMedium.copy(fontFamily = StudioFont),
        labelLarge = labelLarge.copy(fontFamily = StudioFont), labelMedium = labelMedium.copy(fontFamily = StudioFont),
    )
}

@Composable
fun StudioRackApp(model: StudioRackViewModel, hardwareKeys: Flow<Int>, onGigModeActive: (Boolean) -> Unit) {
    val uiState by model.uiState.collectAsState()
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Ink, surface = Panel, surfaceVariant = PanelRaised, primary = Amber, onPrimary = Color(0xFF170E03),
            secondary = Cyan, onSecondary = Color(0xFF06111A), onBackground = Color(0xFFF7F7FB), onSurface = Color(0xFFF7F7FB),
            onSurfaceVariant = TextSoft, outline = Color(0x33FFFFFF), error = Color(0xFFE55757),
        ),
        typography = StudioTypography,
        shapes = Shapes(small = RoundedCornerShape(6.dp), medium = RoundedCornerShape(8.dp), large = RoundedCornerShape(12.dp), extraLarge = RoundedCornerShape(50)),
    ) {
        Surface(Modifier.fillMaxSize(), color = Ink) {
            var selectedEvent by remember { mutableStateOf<String?>(null) }
            when {
                !uiState.signedIn -> LoginScreen(model, uiState)
                selectedEvent != null -> GigModeScreen(model, selectedEvent!!, hardwareKeys, onGigModeActive) { selectedEvent = null }
                else -> MainShell(model, uiState) { selectedEvent = it }
            }
        }
    }
}

private enum class AppSection(val label: String, val mark: String) {
    DASHBOARD("Home", "HOME"),
    EQUIPMENT("Gear", "GEAR"),
    KITS("Kits", "KITS"),
    SESSIONS("Sessions", "LIVE"),
    LIBRARY("Library", "MUSIC"),
    MORE("More", "MORE"),
}

@Composable
private fun MainShell(model: StudioRackViewModel, uiState: StudioRackUiState, openGig: (String) -> Unit) {
    var section by remember { mutableStateOf(AppSection.DASHBOARD) }
    val pending by model.pendingCount.collectAsState()
    val conflicts by model.conflicts.collectAsState()
    Scaffold(
        containerColor = Ink,
        topBar = {
            Surface(modifier = Modifier.statusBarsPadding(), color = Color(0xF20A0D15), shadowElevation = 8.dp) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.studiorack_logo), "StudioRack", Modifier.size(38.dp))
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text("StudioRack", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text(if (pending == 0) "OFFLINE READY" else "$pending CHANGE${if (pending == 1) "" else "S"} QUEUED", color = if (pending == 0) Cyan else Amber, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                    if (conflicts.isNotEmpty()) Surface(color = Color(0xFF8B2F3A), shape = RoundedCornerShape(8.dp)) {
                        Text("${conflicts.size} CONFLICT${if (conflicts.size == 1) "" else "S"}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Panel) {
                AppSection.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = section == destination,
                        onClick = { section = destination },
                        icon = {
                            Surface(
                                modifier = Modifier.width(if (section == destination) 24.dp else 8.dp).height(4.dp),
                                color = if (section == destination) Amber else TextSoft.copy(alpha = 0.38f),
                                shape = RoundedCornerShape(50),
                            ) {}
                        },
                        label = { Text(destination.label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Ink, selectedTextColor = Amber, indicatorColor = Amber.copy(alpha = 0.18f),
                            unselectedIconColor = TextSoft, unselectedTextColor = TextSoft,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(Brush.linearGradient(listOf(Ink, Color(0xFF101A29), Ink)))) {
            when (section) {
                AppSection.DASHBOARD -> DashboardScreen(model, uiState, openGig)
                AppSection.EQUIPMENT -> EquipmentScreen(model)
                AppSection.KITS -> KitsScreen(model)
                AppSection.SESSIONS -> SessionsScreen(model, openGig)
                AppSection.LIBRARY -> LibraryScreen(model)
                AppSection.MORE -> MoreScreen(model, uiState)
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.studiorack_logo), "StudioRack", Modifier.size(54.dp))
            Text("StudioRack", color = Amber, fontSize = 38.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 10.dp))
        }
        Text("Your performance library, available offline.", color = TextSoft)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(code, { code = it }, label = { Text("StudioRack access code") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(mfa, { mfa = it.filter(Char::isDigit).take(6) }, label = { Text("Authenticator code") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(18.dp))
        StudioButton(onClick = { model.signIn(email, code, mfa) }, enabled = !uiState.busy && email.isNotBlank() && code.isNotBlank() && mfa.length == 6) {
            Text("Connect this device", color = Ink, fontWeight = FontWeight.Black)
        }
        if (uiState.busy) CircularProgressIndicator(Modifier.padding(top = 16.dp))
        if (uiState.message.isNotBlank()) Text(uiState.message, color = TextSoft, modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun DashboardScreen(model: StudioRackViewModel, uiState: StudioRackUiState, openGig: (String) -> Unit) {
    val events by model.events.collectAsState()
    val items by model.items.collectAsState()
    val kits by model.kits.collectAsState()
    val brands by model.brands.collectAsState()
    val locations by model.locations.collectAsState()
    val specs by model.itemSpecs.collectAsState()
    val actions by model.buddyActions.collectAsState()
    val entries by model.entries.collectAsState()
    val attachments by model.attachments.collectAsState()
    val cachedAttachments by model.cachedAttachments.collectAsState()
    val state by model.syncState.collectAsState()
    val account = runCatching { JSONObject(state?.accountJson ?: "{}") }.getOrDefault(JSONObject())
    val upcoming = events.map(::recordJson).filter { it.optString("event_status") != "ended" }.sortedBy { it.optString("event_date") + it.optString("start_time") }
    val specRows = specs.map(::supportingJson)
    val purchaseTotal = specRows.filter { it.optString("key") == "purchase_price" }.sumOf { it.optString("value").toDoubleOrNull() ?: 0.0 }
    val careRows = maintenanceRows(specRows, items.map(::supportingJson), brands.map(::supportingJson), locations.map(::supportingJson))
    val tracked = careRows.size
    val openBuddy = actions.map(::supportingJson).count { it.optString("status") !in setOf("handled", "cleared") }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Spacer(Modifier.height(18.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, Color(0xFF343B4D)),
                shape = RoundedCornerShape(8.dp),
            ) {
                Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF2B1B09), Color(0xFF132532), Color(0xFF121621))))) {
                    Column(Modifier.padding(18.dp)) {
                        Text("STUDIO OVERVIEW", color = Amber, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            val logoUrl = account.optString("studio_logo_url")
                            if (logoUrl.isNotBlank()) {
                                CachedNetworkImage(
                                    imageUrl = logoUrl,
                                    contentDescription = "${account.optString("studio_name", "Studio")} logo",
                                    modifier = Modifier.size(width = 92.dp, height = 72.dp).clip(RoundedCornerShape(7.dp)).background(Color(0x6607090F)),
                                    contentScale = ContentScale.Fit,
                                    scale = account.optDouble("studio_logo_scale", 100.0).toFloat().div(100f).coerceIn(.25f, 2f),
                                    positionX = account.optInt("studio_logo_position_x", 50),
                                    positionY = account.optInt("studio_logo_position_y", 50),
                                )
                                Spacer(Modifier.width(13.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(account.optString("studio_name").ifBlank { account.optString("organization", "StudioRack") }, color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                Text(studioAddress(account), color = TextSoft)
                            }
                        }
                        Text("Estimated Studio Value", color = TextSoft, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 14.dp))
                        Text("$${"%,.2f".format(purchaseTotal)}", color = Cyan, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(state?.lastSyncAt?.let { "Synced ${DateFormat.getDateTimeInstance().format(Date(it))}" } ?: "Not synchronized", color = TextSoft, fontSize = 11.sp)
                            StudioButton(onClick = model::sync, enabled = !uiState.busy) { Text(if (uiState.busy) "Syncing" else "Sync now", color = Ink, fontWeight = FontWeight.Black) }
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("Items", items.size.toString(), Modifier.weight(1f))
                MetricCard("Kits", kits.size.toString(), Modifier.weight(1f))
                MetricCard("Events", upcoming.size.toString(), Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("Buddy", openBuddy.toString(), Modifier.weight(1f))
                MetricCard("Care", tracked.toString(), Modifier.weight(1f))
            }
        }
        item { SectionHeading("SCHEDULE", "Coming up next.", eyebrowColor = Color.White) }
        if (upcoming.isEmpty()) item { EmptyCard("No upcoming sessions are stored on this device.") }
        items(upcoming.take(5), key = { it.getString("id") }) { event ->
            val readiness = eventPacketReadiness(event, entries, attachments, cachedAttachments)
            EventCard(event, readiness, open = { if (event.optString("set_list_id").isNotBlank()) openGig(event.getString("id")) })
        }
        item { SectionHeading("CARE READINESS", "What needs hands on it?") }
        item { CareSummary(careRows) }
        item { SectionHeading("STUDIO BUDDY", "Recent activity") }
        if (actions.isEmpty()) item { EmptyCard("No Studio Buddy actions are stored on this device.") }
        items(actions.take(5), key = { it.entityId }) { action -> BuddyActionCard(supportingJson(action)) }
        item { Spacer(Modifier.height(30.dp)) }
    }
}

@Composable
private fun EquipmentScreen(model: StudioRackViewModel) {
    val items by model.items.collectAsState()
    val specs by model.itemSpecs.collectAsState()
    val units by model.itemUnits.collectAsState()
    val categories by model.categories.collectAsState()
    val types by model.itemTypes.collectAsState()
    val locations by model.locations.collectAsState()
    var query by remember { mutableStateOf("") }
    val categoryNames = categories.associate { it.entityId to supportingJson(it).optString("name") }
    val typeNames = types.associate { it.entityId to supportingJson(it).optString("name") }
    val locationNames = locations.associate { it.entityId to supportingJson(it).optString("name") }
    val filtered = items.filter { supportingJson(it).let { row -> query.isBlank() || row.toString().contains(query, true) } }
    RecordListScreen("EQUIPMENT", "Items", query, { query = it }, "Find an item") {
        if (filtered.isEmpty()) item { EmptyCard("No equipment matches this search.") }
        items(filtered, key = { it.entityId }) { record ->
            val row = supportingJson(record)
            val itemSpecs = specs.filter { supportingJson(it).optString("item_id") == record.entityId }.map(::supportingJson)
            val itemUnits = units.filter { supportingJson(it).optString("item_id") == record.entityId }.map(::supportingJson)
            ExpandableRecordCard(
                title = row.optString("display_name", "Unnamed item"),
                subtitle = listOf(categoryNames[row.optString("category_id")], typeNames[row.optString("type_id")]).filterNotNull().filter(String::isNotBlank).joinToString(" / "),
                chips = listOf(row.optString("usage_status"), "Qty ${row.optInt("quantity", 1)}"),
                imageUrl = row.optString("image_url"),
            ) {
                DetailLine("Location", locationNames[row.optString("default_location_id")].orEmpty())
                DetailLine("Notes", row.optString("notes"))
                itemUnits.forEach { unit -> DetailLine(unit.optString("unit_label", "Unit"), listOf(unit.optString("serial_number"), unit.optString("status")).filter(String::isNotBlank).joinToString(" / ")) }
                itemSpecs.sortedBy { it.optString("key") }.forEach { spec -> DetailLine(spec.optString("key").humanize(), spec.optString("value")) }
            }
        }
    }
}

@Composable
private fun KitsScreen(model: StudioRackViewModel) {
    val kits by model.kits.collectAsState()
    val items by model.items.collectAsState()
    val members by model.kitMembers.collectAsState()
    val locations by model.locations.collectAsState()
    var query by remember { mutableStateOf("") }
    val itemNames = items.associate { it.entityId to supportingJson(it).optString("display_name", "Item") }
    val locationNames = locations.associate { it.entityId to supportingJson(it).optString("name") }
    val filtered = kits.filter { query.isBlank() || supportingJson(it).toString().contains(query, true) }
    RecordListScreen("EQUIPMENT", "Kits", query, { query = it }, "Find a kit") {
        if (filtered.isEmpty()) item { EmptyCard("No kits match this search.") }
        items(filtered, key = { it.entityId }) { record ->
            val row = supportingJson(record)
            val kitMembers = members.filter { supportingJson(it).optString("kit_id") == record.entityId }.map(::supportingJson)
            ExpandableRecordCard(row.optString("name", "Unnamed kit"), locationNames[row.optString("location_id")].orEmpty(), listOf("${kitMembers.size} item types"), imageUrl = row.optString("image_url")) {
                DetailLine("Notes", row.optString("notes"))
                kitMembers.forEach { member -> DetailLine(itemNames[member.optString("item_id")].orEmpty(), "Quantity ${member.optInt("quantity", 1)}") }
            }
        }
    }
}

@Composable
private fun SessionsScreen(model: StudioRackViewModel, openGig: (String) -> Unit) {
    val events by model.events.collectAsState()
    val entries by model.entries.collectAsState()
    val attachments by model.attachments.collectAsState()
    val cachedAttachments by model.cachedAttachments.collectAsState()
    var query by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("All") }
    var editingEvent by remember { mutableStateOf<EditorTarget?>(null) }
    val rows = events.map(::recordJson).filter {
        (type == "All" || it.optString("event_type").humanize() == type) && (query.isBlank() || it.toString().contains(query, true))
    }.sortedBy { it.optString("event_date") + it.optString("start_time") }
    Box(Modifier.fillMaxSize()) {
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeading("SESSIONS", "Schedule") }
        item { StudioButton(onClick = { editingEvent = EditorTarget(null, JSONObject()) }, modifier = Modifier.fillMaxWidth()) { Text("Add Scheduled Event", color = Ink, fontWeight = FontWeight.Black) } }
        item { OutlinedTextField(query, { query = it }, label = { Text("Find scheduled work") }, modifier = Modifier.fillMaxWidth()) }
        item { ChoiceStrip(listOf("All", "Performance", "Rehearsal", "Studio Session", "Other"), type) { type = it } }
        if (rows.isEmpty()) item { EmptyCard("No scheduled work matches these filters.") }
        items(rows, key = { it.getString("id") }) { event ->
            EventCard(
                event,
                eventPacketReadiness(event, entries, attachments, cachedAttachments),
                open = { if (event.optString("set_list_id").isNotBlank()) openGig(event.getString("id")) },
                edit = { editingEvent = EditorTarget(event.optString("id"), event) },
            )
        }
    }
    editingEvent?.let { target -> EventEditor(target, model, close = { editingEvent = null }) }
    }
}

@Composable
private fun LibraryScreen(model: StudioRackViewModel) {
    val context = LocalContext.current
    val songs by model.songs.collectAsState()
    val setLists by model.setLists.collectAsState()
    val sections by model.sections.collectAsState()
    val entries by model.entries.collectAsState()
    val attachments by model.attachments.collectAsState()
    var tab by remember { mutableStateOf("Songs") }
    var query by remember { mutableStateOf("") }
    var editingSong by remember { mutableStateOf<EditorTarget?>(null) }
    var editingSetList by remember { mutableStateOf<CachedRecord?>(null) }
    var creatingSetList by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeading("LIBRARY", "Songs and Set Lists") }
        item { ChoiceStrip(listOf("Songs", "Set Lists"), tab) { tab = it; query = "" } }
        item {
            StudioButton(
                onClick = { if (tab == "Songs") editingSong = EditorTarget(null, JSONObject()) else creatingSetList = true },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (tab == "Songs") "Add Song" else "Create Set List", color = Ink, fontWeight = FontWeight.Black) }
        }
        item { OutlinedTextField(query, { query = it }, label = { Text(if (tab == "Songs") "Find a song" else "Find a set list") }, modifier = Modifier.fillMaxWidth()) }
        if (tab == "Songs") {
            val filtered = songs.filter { query.isBlank() || recordJson(it).toString().contains(query, true) }
            items(filtered, key = { it.entityId }) { record ->
                val song = recordJson(record)
                val songAttachments = attachments.filter { recordJson(it).optString("song_id") == record.entityId }
                ExpandableRecordCard(
                    song.optString("title", "Untitled song"), song.optString("artist"),
                    listOf(song.optString("style"), song.optString("tempo"), song.optString("time_signature"), if (song.optInt("is_favorite") == 1) "Favorite" else "").filter(String::isNotBlank),
                    actionLabel = if (normalizedMediaLink(song.optString("media_ref")) != null) "Listen" else null,
                    action = normalizedMediaLink(song.optString("media_ref"))?.let { link -> { openMediaLink(context, link) } },
                ) {
                    DetailLine("Starts", song.optString("starts_by"))
                    DetailLine("Patch", listOf(song.optString("patch_name"), song.optString("patch_number")).filter(String::isNotBlank).joinToString(" / "))
                    DetailLine("Notes", song.optString("notes"))
                    normalizedMediaLink(song.optString("media_ref"))?.let { link ->
                        Row(Modifier.fillMaxWidth()) { GigPill("Listen", onClick = { openMediaLink(context, link) }) }
                    }
                    songAttachments.forEach { DetailLine("Attachment", attachmentLabel(recordJson(it))) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { editingSong = EditorTarget(record.entityId, song) }) { Text("Edit", color = Amber) }
                    }
                }
            }
        } else {
            val songNames = songs.associate { it.entityId to recordJson(it).optString("title", "Song") }
            val filtered = setLists.filter { query.isBlank() || recordJson(it).toString().contains(query, true) }
            items(filtered, key = { it.entityId }) { record ->
                val row = recordJson(record)
                val setSections = sections.filter { recordJson(it).optString("set_list_id") == record.entityId }
                val setEntries = entries.filter { recordJson(it).optString("set_list_id") == record.entityId }
                ExpandableRecordCard(
                    row.optString("name", "Unnamed set list"), row.optString("description"), listOf("${setSections.size} sets", "${setEntries.size} songs"),
                    actionLabel = "Edit", action = { editingSetList = record },
                ) {
                    setSections.sortedBy { recordJson(it).optInt("position") }.forEach { section ->
                        val sectionJson = recordJson(section)
                        Text(sectionJson.optString("name", "Set"), color = Amber, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        setEntries.filter { recordJson(it).optString("section_id") == section.entityId }.sortedBy { recordJson(it).optInt("position") }.forEach { entry ->
                            val entryJson = recordJson(entry)
                            DetailLine((entryJson.optInt("position") + 1).toString(), songNames[entryJson.optString("song_id")].orEmpty().ifBlank { entryJson.optString("manual_title") })
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { editingSetList = record }) { Text("Edit Set List", color = Amber) }
                    }
                }
            }
        }
    }
    editingSong?.let { target ->
        SongEditor(target, model, close = { editingSong = null })
    }
    if (creatingSetList || editingSetList != null) {
        Dialog(onDismissRequest = { creatingSetList = false; editingSetList = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            SetListEditor(editingSetList, sections, entries, songs, attachments, model) { creatingSetList = false; editingSetList = null }
        }
    }
    }
}

@Composable
private fun MoreScreen(model: StudioRackViewModel, uiState: StudioRackUiState) {
    var tab by remember { mutableStateOf("Reports") }
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeading("STUDIORACK", "More") }
        item { ChoiceStrip(listOf("Reports", "Studio Buddy", "Reference", "Sync", "Settings"), tab) { tab = it } }
        when (tab) {
            "Reports" -> reportsContent(model)
            "Studio Buddy" -> buddyContent(model)
            "Reference" -> referenceContent(model)
            "Sync" -> syncContent(model, uiState)
            else -> settingsContent(model, uiState)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.syncContent(model: StudioRackViewModel, uiState: StudioRackUiState) {
    item {
        val pending by model.pendingCount.collectAsState()
        val conflicts by model.conflicts.collectAsState()
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Offline Changes", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("Queued", pending.toString(), Modifier.weight(1f)); MetricCard("Conflicts", conflicts.size.toString(), Modifier.weight(1f))
            }
            StudioButton(onClick = model::sync, enabled = !uiState.busy, modifier = Modifier.fillMaxWidth()) { Text("Sync Now", color = Ink, fontWeight = FontWeight.Black) }
            if (conflicts.isEmpty()) Text("No synchronization conflicts.", color = TextSoft)
            conflicts.forEach { conflict ->
                InfoCard {
                    Text(conflict.entityType.humanize(), color = Amber, fontWeight = FontWeight.Bold)
                    Text(conflict.entityId, color = TextSoft, fontSize = 11.sp)
                    Text("This record changed both here and on the server.", color = Color.White)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StudioButton(onClick = { model.resolveConflict(conflict, false) }, kind = StudioButtonKind.Secondary) { Text("Use Server", color = Color.White) }
                        StudioButton(onClick = { model.resolveConflict(conflict, true) }) { Text("Keep Device", color = Ink, fontWeight = FontWeight.Black) }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.reportsContent(model: StudioRackViewModel) {
    item { ReportsPanel(model) }
}

@Composable
private fun ReportsPanel(model: StudioRackViewModel) {
    val context = LocalContext.current
    val items by model.items.collectAsState()
    val kits by model.kits.collectAsState()
    val events by model.events.collectAsState()
    val specs by model.itemSpecs.collectAsState()
    val units by model.itemUnits.collectAsState()
    val brands by model.brands.collectAsState()
    val categories by model.categories.collectAsState()
    val types by model.itemTypes.collectAsState()
    val locations by model.locations.collectAsState()
    val statuses by model.statuses.collectAsState()
    val runs by model.reportRuns.collectAsState()
    val reportState by model.reportState.collectAsState()
    val online = deviceHasInternet(context)
    val itemRows = items.map(::supportingJson)
    val specRows = specs.map(::supportingJson)
    val careRows = maintenanceRows(specRows, itemRows, brands.map(::supportingJson), locations.map(::supportingJson), windowDays = 36500)
    var tab by remember { mutableStateOf("Overview") }
    var question by remember { mutableStateOf("") }

    LaunchedEffect(online) {
        if (online && reportState.onlineOverview == null && !reportState.busy) model.refreshReportOverview()
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Reports", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Surface(
            color = if (online) Cyan.copy(alpha = 0.10f) else Amber.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, if (online) Cyan.copy(alpha = 0.35f) else Amber.copy(alpha = 0.45f)),
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(
                if (online) "ONLINE - Server and AI reports are available." else "OFFLINE MODE - Synchronized reports remain available; server and AI report runs require a connection.",
                color = if (online) Cyan else Amber,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(11.dp),
            )
        }
        ChoiceStrip(listOf("Overview", "Equipment", "Maintenance", "Schedule", "AI"), tab) { tab = it }
        when (tab) {
            "Overview" -> ReportOverviewTab(itemRows, kits, events.map(::recordJson), units, specRows, categories, statuses, locations, careRows, reportState, online, model)
            "Equipment" -> EquipmentReportTab(itemRows, specRows, categories, types, statuses, locations)
            "Maintenance" -> MaintenanceReport(careRows)
            "Schedule" -> ScheduleReportTab(events.map(::recordJson))
            else -> AiReportTab(question, { question = it }, online, reportState, model, runs)
        }
    }
}

@Composable
private fun ReportOverviewTab(
    items: List<JSONObject>,
    kits: List<SupportingRecord>,
    events: List<JSONObject>,
    units: List<SupportingRecord>,
    specs: List<JSONObject>,
    categories: List<SupportingRecord>,
    statuses: List<SupportingRecord>,
    locations: List<SupportingRecord>,
    careRows: List<MaintenanceRow>,
    reportState: ReportUiState,
    online: Boolean,
    model: StudioRackViewModel,
) {
    val valuesByItem = specs.groupBy { it.optString("item_id") }.mapValues { entry -> entry.value.associate { it.optString("key") to it.optString("value") } }
    val purchase = valuesByItem.values.sumOf { it["purchase_price"]?.toDoubleOrNull() ?: 0.0 }
    val replacement = valuesByItem.values.sumOf { it["replacement_value"]?.toDoubleOrNull() ?: 0.0 }
    val estimated = valuesByItem.values.sumOf(::estimatedItemValue)
    val serverTotals = reportState.onlineOverview?.optJSONObject("totals")
    val serverValues = reportState.onlineOverview?.optJSONObject("values")
    val itemCount = serverTotals?.optInt("items", items.size) ?: items.size
    val unitCount = serverTotals?.optInt("units", units.size) ?: units.size
    val kitCount = serverTotals?.optInt("kits", kits.size) ?: kits.size
    val eventCount = serverTotals?.optInt("events", events.size) ?: events.size
    val careCount = serverTotals?.optInt("maintenance_tracked", careRows.size) ?: careRows.size
    val purchaseValue = serverValues?.optDouble("purchase", purchase) ?: purchase
    val replacementValue = serverValues?.optDouble("replacement", replacement) ?: replacement
    val estimatedValue = serverValues?.optDouble("estimated", estimated) ?: estimated
    val categoryNames = categories.associate { it.entityId to supportingJson(it).optString("name") }
    val statusNames = statuses.associate { it.entityId to supportingJson(it).optString("name") }
    val locationNames = locations.associate { it.entityId to supportingJson(it).optString("name") }
    val categoryCounts = items.groupingBy { categoryNames[it.optString("category_id")].orEmpty().ifBlank { "Unassigned" } }.eachCount()
    val statusCounts = items.groupingBy { statusNames[it.optString("usage_status")].orEmpty().ifBlank { it.optString("usage_status", "Unspecified").humanize() } }.eachCount()
    val locationCounts = items.groupingBy { locationNames[it.optString("default_location_id")].orEmpty().ifBlank { "No location" } }.eachCount()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Full Overview", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            MetricCard("Items", itemCount.toString(), Modifier.weight(1f))
            MetricCard("Units", unitCount.toString(), Modifier.weight(1f))
            MetricCard("Kits", kitCount.toString(), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            MetricCard("Events", eventCount.toString(), Modifier.weight(1f))
            MetricCard("Care", careCount.toString(), Modifier.weight(1f))
        }
        ReportCard("Asset values", "Estimated $${"%,.2f".format(estimatedValue)}", "Purchase $${"%,.2f".format(purchaseValue)}  |  Replacement $${"%,.2f".format(replacementValue)}")
        DistributionCard("Equipment by category", categoryCounts)
        DistributionCard("Equipment by status", statusCounts)
        DistributionCard("Equipment by location", locationCounts)
        if (online) StudioButton(onClick = model::refreshReportOverview, enabled = !reportState.busy, modifier = Modifier.fillMaxWidth()) {
            Text(if (reportState.busy) "Refreshing Server Report" else "Refresh From Server", color = Ink, fontWeight = FontWeight.Black)
        }
        reportState.onlineOverview?.optString("generated_utc")?.takeIf(String::isNotBlank)?.let {
            Text("Server verified $it", color = TextSoft, fontSize = 11.sp)
        }
    }
}

@Composable
private fun DistributionCard(title: String, values: Map<String, Int>) {
    InfoCard {
        Text(title, color = Amber, fontWeight = FontWeight.Bold)
        val maximum = values.values.maxOrNull()?.coerceAtLeast(1) ?: 1
        values.entries.sortedByDescending(Map.Entry<String, Int>::value).forEach { (label, count) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, color = Color.White, fontSize = 12.sp)
                Text(count.toString(), color = TextSoft, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.fillMaxWidth().height(5.dp).background(Color(0xFF303747), RoundedCornerShape(50))) {
                Box(Modifier.fillMaxWidth(count.toFloat() / maximum).height(5.dp).background(Brush.horizontalGradient(listOf(Amber, Cyan)), RoundedCornerShape(50)))
            }
        }
    }
}

@Composable
private fun EquipmentReportTab(
    items: List<JSONObject>, specs: List<JSONObject>, categories: List<SupportingRecord>, types: List<SupportingRecord>,
    statuses: List<SupportingRecord>, locations: List<SupportingRecord>,
) {
    var query by remember { mutableStateOf("") }
    val specMap = specs.groupBy { it.optString("item_id") }.mapValues { it.value.associate { row -> row.optString("key") to row.optString("value") } }
    val categoryNames = categories.associate { it.entityId to supportingJson(it).optString("name") }
    val typeNames = types.associate { it.entityId to supportingJson(it).optString("name") }
    val statusNames = statuses.associate { it.entityId to supportingJson(it).optString("name") }
    val locationNames = locations.associate { it.entityId to supportingJson(it).optString("name") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Equipment Report", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(query, { query = it }, label = { Text("Filter equipment report") }, modifier = Modifier.fillMaxWidth())
        val filtered = items.filter { query.isBlank() || it.toString().contains(query, true) }
        Text("${filtered.size} matching items", color = TextSoft, fontSize = 12.sp)
        filtered.sortedBy { it.optString("display_name").lowercase() }.forEach { item ->
            val values = specMap[item.optString("id")].orEmpty()
            ExpandableRecordCard(
                item.optString("display_name", "Unnamed item"),
                listOf(categoryNames[item.optString("category_id")], typeNames[item.optString("type_id")]).filterNotNull().filter(String::isNotBlank).joinToString(" / "),
                listOf(statusNames[item.optString("usage_status")].orEmpty(), values["asset_number"].orEmpty()),
                imageUrl = item.optString("image_url"),
            ) {
                DetailLine("Location", locationNames[item.optString("default_location_id")].orEmpty())
                DetailLine("Purchase date", values["purchase_date"].orEmpty())
                DetailLine("Purchase value", moneyValue(values["purchase_price"]))
                DetailLine("Replacement value", moneyValue(values["replacement_value"]))
                DetailLine("Maintenance due", values["next_service_due"].orEmpty())
            }
        }
    }
}

@Composable
private fun MaintenanceReport(rows: List<MaintenanceRow>) {
    var status by remember { mutableStateOf("All") }
    val filtered = rows.filter { status == "All" || it.statusLabel == status }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Maintenance Report", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        ChoiceStrip(listOf("All", "Past due", "Due today", "Upcoming", "Scheduled"), status) { status = it }
        Text("${filtered.size} matching maintenance records", color = TextSoft, fontSize = 12.sp)
        if (filtered.isEmpty()) Text("No equipment matches this care status.", color = TextSoft)
        filtered.forEach { row ->
            ExpandableRecordCard(
                listOf(row.brand, row.name).filter(String::isNotBlank).joinToString(" "),
                "${row.statusLabel} - ${row.dueDate}", listOf(row.careItem, row.careStatus), imageUrl = row.imageUrl,
            ) {
                DetailLine("Due", row.dueDate)
                DetailLine("Status", row.statusLabel)
                DetailLine("Care item", row.careItem)
                DetailLine("Location", row.location)
                DetailLine("Notes", row.notes)
            }
        }
    }
}

@Composable
private fun ScheduleReportTab(events: List<JSONObject>) {
    var query by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("All") }
    val filtered = events.filter { event ->
        (type == "All" || event.optString("event_type").humanize() == type) &&
            (query.isBlank() || event.toString().contains(query, true))
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Schedule Report", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(query, { query = it }, label = { Text("Filter schedule report") }, modifier = Modifier.fillMaxWidth())
        ChoiceStrip(listOf("All", "Performance", "Rehearsal", "Studio Session", "Other"), type) { type = it }
        Text("${filtered.size} matching schedule records", color = TextSoft, fontSize = 12.sp)
        if (filtered.isEmpty()) Text("No scheduled records match these filters.", color = TextSoft)
        filtered.sortedBy { it.optString("event_date") + it.optString("start_time") }.forEach { event ->
            ExpandableRecordCard(event.optString("title", "Untitled event"), listOf(event.optString("event_date"), event.optString("start_time")).filter(String::isNotBlank).joinToString(" / "), listOf(event.optString("event_type").humanize(), event.optString("event_status").humanize())) {
                DetailLine("Location", event.optString("location"))
                DetailLine("Notes", event.optString("notes"))
                DetailLine("Reminder", if (event.optInt("reminder_enabled") == 1) "${event.optInt("reminder_lead_value")} ${event.optString("reminder_lead_unit")} before" else "Disabled")
            }
        }
    }
}

@Composable
private fun AiReportTab(
    question: String,
    changeQuestion: (String) -> Unit,
    online: Boolean,
    state: ReportUiState,
    model: StudioRackViewModel,
    runs: List<SupportingRecord>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Ask Studio Buddy", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Describe the equipment report you need in ordinary language.", color = TextSoft)
        OutlinedTextField(question, changeQuestion, label = { Text("Report question") }, enabled = online && !state.busy, modifier = Modifier.fillMaxWidth())
        StudioButton(onClick = { model.runAiReport(question) }, enabled = online && question.isNotBlank() && !state.busy, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.busy) "Running Report" else "Run AI Report", color = Ink, fontWeight = FontWeight.Black)
        }
        if (state.message.isNotBlank()) Text(state.message, color = if (state.aiResult != null) Cyan else TextSoft)
        state.aiResult?.let { result ->
            Text(result.optString("title", "Report Results"), color = Amber, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("${result.optInt("row_count")} matching items", color = TextSoft)
            result.optJSONArray("rows").jsonObjects().forEach { row ->
                ExpandableRecordCard(row.optString("name", "Item"), listOf(row.optString("brand"), row.optString("category"), row.optString("type")).filter(String::isNotBlank).joinToString(" / "), listOf(row.optString("status"), row.optString("maintenance_label"))) {
                    DetailLine("Asset number", row.optString("asset_number"))
                    DetailLine("Location", row.optString("location"))
                    DetailLine("Age", row.opt("age_years")?.toString().orEmpty())
                    DetailLine("Replacement value", moneyValue(row.opt("replacement_value")?.toString()))
                    DetailLine("Maintenance due", row.optString("maintenance_due"))
                }
            }
        }
        Text("Previous AI Reports", color = Amber, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        if (runs.isEmpty()) Text("No saved AI reports are synchronized.", color = TextSoft)
        runs.take(12).forEach { run ->
            val row = supportingJson(run)
            ReportCard(row.optString("title", "Report"), row.optString("question"), "${row.optInt("row_count")} rows - ${row.optString("created_utc")}")
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.buddyContent(model: StudioRackViewModel) {
    item { Text("Available Skills", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
    item {
        val skills by model.buddySkills.collectAsState()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (skills.isEmpty()) Text("No skills are stored offline.", color = TextSoft)
            skills.forEach { skill ->
                val row = supportingJson(skill)
                ExpandableRecordCard(row.optString("name", "Skill"), row.optString("description"), listOf(row.optString("trigger_type").humanize(), if (row.optInt("account_enabled") == 1) "Enabled" else "Disabled")) {
                    DetailLine("Prompt", row.optString("prompt")); DetailLine("Teaching notes", row.optString("custom_instructions")); DetailLine("Last run", row.optString("account_last_run_utc"))
                }
            }
        }
    }
    item { Text("Action History", color = Amber, fontWeight = FontWeight.Bold) }
    item {
        val actions by model.buddyActions.collectAsState()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { actions.take(30).forEach { BuddyActionCard(supportingJson(it)) } }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.referenceContent(model: StudioRackViewModel) {
    item {
        val categories by model.categories.collectAsState(); val types by model.itemTypes.collectAsState(); val locations by model.locations.collectAsState(); val statuses by model.statuses.collectAsState()
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Reference Data", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            ReferenceGroup("Categories", categories); ReferenceGroup("Equipment Types", types); ReferenceGroup("Locations", locations); ReferenceGroup("Statuses", statuses)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.settingsContent(model: StudioRackViewModel, uiState: StudioRackUiState) {
    item {
        val state by model.syncState.collectAsState(); val account = runCatching { JSONObject(state?.accountJson ?: "{}") }.getOrDefault(JSONObject())
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Account and Device", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            InfoCard { DetailLine("Studio", account.optString("studio_name")); DetailLine("Account", account.optString("email")); DetailLine("Address", studioAddress(account)); DetailLine("Phone", account.optString("phone")); DetailLine("Contact", account.optString("contact_email")); DetailLine("Last sync", state?.lastSyncAt?.let { DateFormat.getDateTimeInstance().format(Date(it)) }.orEmpty()) }
            StudioButton(onClick = model::sync, enabled = !uiState.busy, modifier = Modifier.fillMaxWidth()) { Text(if (uiState.busy) "Synchronizing" else "Synchronize StudioRack", color = Ink, fontWeight = FontWeight.Black) }
            Text("Changes made on the web are copied here automatically when the device reconnects.", color = TextSoft, fontSize = 12.sp)
        }
    }
}

@Composable
private fun RecordListScreen(
    eyebrow: String,
    title: String,
    query: String,
    onQuery: (String) -> Unit,
    placeholder: String,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SectionHeading(eyebrow, title) }
        item { OutlinedTextField(query, onQuery, label = { Text(placeholder) }, modifier = Modifier.fillMaxWidth()) }
        content()
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun SectionHeading(eyebrow: String, title: String, eyebrowColor: Color = Amber) {
    Column(Modifier.padding(top = 4.dp, bottom = 2.dp)) {
        Text(eyebrow, color = eyebrowColor, fontSize = 11.sp, fontWeight = FontWeight.Black)
        Text(title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(Brush.linearGradient(listOf(Color(0xFF242A38), Color(0xFF1B2C36))), RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, Color(0x263CD9FF)), RoundedCornerShape(8.dp)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(label.uppercase(), color = TextSoft, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Text(value, color = Amber, fontSize = 25.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ChoiceStrip(options: List<String>, selected: String, choose: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        options.forEach { option ->
            StudioButton(
                onClick = { choose(option) },
                kind = if (option == selected) StudioButtonKind.Primary else StudioButtonKind.Secondary,
            ) { Text(option, color = if (option == selected) Ink else Color.White, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun ExpandableRecordCard(
    title: String,
    subtitle: String,
    chips: List<String>,
    actionLabel: String? = null,
    action: (() -> Unit)? = null,
    imageUrl: String = "",
    details: @Composable ColumnScope.() -> Unit,
) {
    var expanded by remember(title, subtitle) { mutableStateOf(false) }
    Card(
        Modifier.fillMaxWidth().clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = Color(0xE6202635)),
        border = BorderStroke(1.dp, Color(0xFF343B4D)),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (imageUrl.isNotBlank()) {
                    CachedNetworkImage(
                        imageUrl = imageUrl,
                        contentDescription = title,
                        modifier = Modifier.size(58.dp).clip(RoundedCornerShape(6.dp)).background(Ink),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(Modifier.width(11.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    if (subtitle.isNotBlank()) Text(subtitle, color = TextSoft, fontSize = 13.sp)
                }
                if (actionLabel != null && action != null) TextButton(onClick = action) { Text(actionLabel, color = Amber, fontWeight = FontWeight.Bold) }
                Text(if (expanded) "-" else "+", color = Amber, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                chips.filter(String::isNotBlank).forEach { chip ->
                    Surface(color = Color(0xFF332A1D), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFF674B16))) {
                        Text(chip, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
                    }
                }
            }
            if (expanded) Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(7.dp), content = details)
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    if (value.isBlank()) return
    Column(Modifier.fillMaxWidth()) {
        Text(label, color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
        Text(value, color = Color.White, fontSize = 14.sp)
    }
}

@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = PanelRaised), shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun ReportCard(title: String, primary: String, secondary: String) {
    InfoCard {
        Text(title, color = Amber, fontWeight = FontWeight.Bold)
        if (primary.isNotBlank()) Text(primary, color = Color.White, fontSize = 18.sp)
        if (secondary.isNotBlank()) Text(secondary, color = TextSoft, fontSize = 12.sp)
    }
}

@Composable
private fun BuddyActionCard(row: JSONObject) {
    ExpandableRecordCard(row.optString("subject", "Studio Buddy action"), row.optString("updated_utc"), listOf(row.optString("priority").humanize(), row.optString("status").humanize())) {
        DetailLine("Recipient", row.optString("recipient")); DetailLine("Due", row.optString("source_due_date")); DetailLine("Draft", row.optString("body")); DetailLine("Last reply", row.optString("last_reply_body"))
    }
}

@Composable
private fun CareSummary(rows: List<MaintenanceRow>) {
    val attention = rows.filter { it.status in setOf("overdue", "due", "soon") }
    val counts = listOf(
        "Past due" to rows.count { it.status == "overdue" },
        "Due today" to rows.count { it.status == "due" },
        "Upcoming" to rows.count { it.status == "soon" },
    )
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            counts.forEach { (label, count) ->
                Column(Modifier.weight(1f)) {
                    Text(label.uppercase(), color = TextSoft, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    Text(count.toString(), color = if (label == "Past due") Color(0xFFE55757) else Amber, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        if (attention.isEmpty()) {
            Text("Nothing is past due, due today, or coming up inside the current care window.", color = TextSoft)
        } else {
            attention.take(8).forEach { row ->
                ExpandableRecordCard(
                    title = listOf(row.brand, row.name).filter(String::isNotBlank).joinToString(" "),
                    subtitle = "${row.statusLabel} - ${row.careItem} - ${row.dueDate}",
                    chips = listOf(row.statusLabel, row.careItem),
                    imageUrl = row.imageUrl,
                ) {
                    DetailLine("Due", row.dueDate)
                    DetailLine("Status", row.statusLabel)
                    DetailLine("Care item", row.careItem)
                    DetailLine("Location", row.location)
                    DetailLine("Care status", row.careStatus)
                    DetailLine("Notes", row.notes)
                }
            }
        }
    }
}

internal data class MaintenanceRow(
    val itemId: String,
    val name: String,
    val brand: String,
    val imageUrl: String,
    val dueDate: String,
    val status: String,
    val statusLabel: String,
    val careItem: String,
    val location: String,
    val careStatus: String,
    val notes: String,
)

internal fun maintenanceRows(
    specs: List<JSONObject>,
    items: List<JSONObject>,
    brands: List<JSONObject>,
    locations: List<JSONObject>,
    today: LocalDate = LocalDate.now(),
    windowDays: Long = 30,
): List<MaintenanceRow> {
    val specsByItem = specs.groupBy { it.optString("item_id") }.mapValues { (_, rows) ->
        rows.associate { it.optString("key") to it.optString("value") }
    }
    val brandNames = brands.associate { it.optString("id") to it.optString("name") }
    val locationNames = locations.associate { it.optString("id") to it.optString("name") }
    return items.mapNotNull { item ->
        val itemId = item.optString("id")
        val values = specsByItem[itemId].orEmpty()
        val explicitDue = parseLocalDate(values["next_service_due"])
        val intervalDue = parseLocalDate(values["last_service_date"])?.let { last ->
            values["service_interval_days"]?.toLongOrNull()?.takeIf { it >= 0 }?.let(last::plusDays)
        }
        val due = explicitDue ?: intervalDue ?: return@mapNotNull null
        val days = ChronoUnit.DAYS.between(today, due)
        val (status, label) = when {
            days < 0 -> "overdue" to "Past due"
            days == 0L -> "due" to "Due today"
            days <= windowDays -> "soon" to "Upcoming"
            else -> "scheduled" to "Scheduled"
        }
        val summary = listOf(
            values["maintenance_schedule"], values["service_notes"], values["last_service_notes"],
        ).filterNotNull().firstOrNull(String::isNotBlank).orEmpty()
        MaintenanceRow(
            itemId = itemId,
            name = item.optString("display_name", "Unnamed item"),
            brand = brandNames[item.optString("brand_id")].orEmpty(),
            imageUrl = item.optString("image_url"),
            dueDate = due.toString(),
            status = status,
            statusLabel = label,
            careItem = values["consumables_tracked"].orEmpty().ifBlank { "Service" },
            location = locationNames[item.optString("default_location_id")].orEmpty().ifBlank { "No location" },
            careStatus = values["care_status"].orEmpty().humanize().ifBlank { "Not set" },
            notes = values["bot_notes"].orEmpty().ifBlank { summary },
        )
    }.sortedWith(compareBy({ it.dueDate }, { it.name.lowercase() }))
}

private fun parseLocalDate(value: String?): LocalDate? = value?.trim()?.takeIf(String::isNotBlank)?.let { clean ->
    runCatching { LocalDate.parse(clean) }.getOrNull()
}

@Composable
private fun CachedNetworkImage(
    imageUrl: String,
    contentDescription: String,
    modifier: Modifier,
    contentScale: ContentScale,
    scale: Float = 1f,
    positionX: Int = 50,
    positionY: Int = 50,
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, imageUrl) {
        value = withContext(Dispatchers.IO) { loadCachedImage(context, imageUrl) }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = scale, scaleY = scale),
                contentScale = contentScale,
                alignment = BiasAlignment(
                    horizontalBias = ((positionX.coerceIn(0, 100) - 50) / 50f),
                    verticalBias = ((positionY.coerceIn(0, 100) - 50) / 50f),
                ),
            )
        } else {
            Text("SR", color = Amber, fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
    }
}

private fun loadCachedImage(context: Context, imageUrl: String): Bitmap? {
    return cacheImageFile(context, imageUrl)?.let { BitmapFactory.decodeFile(it.absolutePath) }
}

@Composable
private fun ReferenceGroup(title: String, rows: List<SupportingRecord>) {
    ExpandableRecordCard(title, "${rows.size} available", emptyList()) {
        rows.forEach { DetailLine(supportingJson(it).optString("name", it.entityId), supportingJson(it).optString("asset_code")) }
    }
}

private fun supportingJson(record: SupportingRecord): JSONObject = runCatching { JSONObject(record.json) }.getOrDefault(JSONObject())

private fun deviceHasInternet(context: Context): Boolean {
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

private fun JSONArray?.jsonObjects(): List<JSONObject> = buildList {
    val source = this@jsonObjects ?: return@buildList
    for (index in 0 until source.length()) source.optJSONObject(index)?.let(::add)
}

private fun estimatedItemValue(specs: Map<String, String>): Double {
    if (specs["curated_artifact"].equals("yes", true)) return 0.0
    val purchase = specs["purchase_price"]?.toDoubleOrNull() ?: return 0.0
    val purchased = parseLocalDate(specs["purchase_date"])
    val years = purchased?.let { ChronoUnit.DAYS.between(it, LocalDate.now()).coerceAtLeast(0) / 365.25 } ?: 0.0
    return purchase * (1.0 - years / 5.0).coerceIn(0.0, 1.0)
}

private fun moneyValue(value: String?): String = value?.toDoubleOrNull()?.let { "$${"%,.2f".format(it)}" }.orEmpty()

private fun String.humanize(): String = replace('_', ' ').trim().split(' ').joinToString(" ") { word ->
    word.lowercase().replaceFirstChar { it.uppercase() }
}

private fun studioAddress(account: JSONObject): String = listOf(
    account.optString("location_name"), account.optString("address_line1"), account.optString("address_line2"),
    listOf(account.optString("city"), account.optString("region"), account.optString("postal_code")).filter(String::isNotBlank).joinToString(" "),
).filter(String::isNotBlank).joinToString(" | ").ifBlank { "Offline StudioRack workspace" }

private data class EditorTarget(val id: String?, val data: JSONObject)

@Composable
private fun SongEditor(target: EditorTarget, model: StudioRackViewModel, close: () -> Unit) {
    val original = target.data
    var title by remember { mutableStateOf(original.optString("title")) }
    var artist by remember { mutableStateOf(original.optString("artist")) }
    var style by remember { mutableStateOf(original.optString("style")) }
    var tempo by remember { mutableStateOf(original.optString("tempo")) }
    var signature by remember { mutableStateOf(original.optString("time_signature", "4/4")) }
    var starts by remember { mutableStateOf(original.optString("starts_by")) }
    var patchName by remember { mutableStateOf(original.optString("patch_name")) }
    var patchNumber by remember { mutableStateOf(original.optString("patch_number")) }
    var media by remember { mutableStateOf(original.optString("media_ref")) }
    var notes by remember { mutableStateOf(original.optString("notes")) }
    var favorite by remember { mutableStateOf(original.optInt("is_favorite") == 1) }
    EditorDialog(if (target.id == null) "Add Song" else "Edit Song", close) {
        StudioField("Song title", title) { title = it }
        StudioField("Artist", artist) { artist = it }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) { StudioField("Style", style) { style = it } }
            Box(Modifier.weight(1f)) { StudioField("Tempo", tempo) { tempo = it.filter(Char::isDigit).take(3) } }
            Box(Modifier.weight(1f)) { StudioField("Time signature", signature) { signature = it.take(12) } }
        }
        StudioField("Who starts", starts) { starts = it }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) { StudioField("Patch name", patchName) { patchName = it } }
            Box(Modifier.weight(1f)) { StudioField("Patch number", patchNumber) { patchNumber = it } }
        }
        StudioField("Listen / media URL", media) { media = it }
        StudioField("Notes", notes, singleLine = false) { notes = it }
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(favorite, { favorite = it }); Text("Favorite", color = Color.White) }
        EditorActions(
            canSave = title.isNotBlank(),
            save = {
                model.saveSong(target.id, JSONObject()
                    .put("title", title.trim()).put("artist", artist.trim()).put("style", style.trim())
                    .put("tempo", tempo.trim()).put("time_signature", signature.trim()).put("starts_by", starts.trim())
                    .put("patch_name", patchName.trim()).put("patch_number", patchNumber.trim())
                    .put("media_ref", media.trim()).put("notes", notes.trim()).put("is_favorite", if (favorite) 1 else 0), close)
            },
            delete = target.id?.let { id -> { model.deleteSong(id, close) } },
        )
    }
}

@Composable
private fun EventEditor(target: EditorTarget, model: StudioRackViewModel, close: () -> Unit) {
    val original = target.data
    val setLists by model.setLists.collectAsState()
    var title by remember { mutableStateOf(original.optString("title")) }
    var type by remember { mutableStateOf(original.optString("event_type", "performance")) }
    var status by remember { mutableStateOf(original.optString("event_status", "scheduled")) }
    var date by remember { mutableStateOf(original.optString("event_date")) }
    var time by remember { mutableStateOf(original.optString("start_time")) }
    var location by remember { mutableStateOf(original.optString("location")) }
    var setListId by remember { mutableStateOf(original.optString("set_list_id")) }
    var notes by remember { mutableStateOf(original.optString("notes")) }
    var reminder by remember { mutableStateOf(original.optInt("reminder_enabled", 1) == 1) }
    var lead by remember { mutableStateOf(original.optString("reminder_lead_value", "2")) }
    var unit by remember { mutableStateOf(original.optString("reminder_lead_unit", "days")) }
    EditorDialog(if (target.id == null) "Add Scheduled Event" else "Edit Scheduled Event", close) {
        StudioField("Name", title) { title = it }
        Text("Type", color = TextSoft, fontWeight = FontWeight.Bold); ChoiceStrip(listOf("performance", "rehearsal", "studio_session", "other"), type) { type = it }
        Text("Status", color = TextSoft, fontWeight = FontWeight.Bold); ChoiceStrip(listOf("scheduled", "ended"), status) { status = it }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) { StudioField("Date (YYYY-MM-DD)", date) { date = it.take(10) } }
            Box(Modifier.weight(1f)) { StudioField("Time", time) { time = it.take(8) } }
        }
        StudioField("Location", location) { location = it }
        Text("Set list", color = TextSoft, fontWeight = FontWeight.Bold)
        ChoiceStrip(listOf("None") + setLists.map { recordJson(it).optString("name") }, setLists.firstOrNull { it.entityId == setListId }?.let { recordJson(it).optString("name") } ?: "None") { picked ->
            setListId = setLists.firstOrNull { recordJson(it).optString("name") == picked }?.entityId.orEmpty()
        }
        StudioField("Notes", notes, singleLine = false) { notes = it }
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(reminder, { reminder = it }); Text("Studio Buddy reminder", color = Color.White) }
        if (reminder) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) { StudioField("How close", lead) { lead = it.filter(Char::isDigit).take(3) } }
            Box(Modifier.weight(1f)) { Text("Unit", color = TextSoft); ChoiceStrip(listOf("hours", "days", "weeks"), unit) { unit = it } }
        }
        EditorActions(
            canSave = title.isNotBlank(),
            save = {
                model.saveEvent(target.id, JSONObject()
                    .put("event_type", type).put("event_status", status).put("title", title.trim())
                    .put("event_date", date.trim()).put("start_time", time.trim()).put("location", location.trim())
                    .put("set_list_id", setListId.ifBlank { JSONObject.NULL }).put("notes", notes.trim())
                    .put("reminder_enabled", if (reminder) 1 else 0).put("reminder_lead_value", lead.toIntOrNull() ?: 2)
                    .put("reminder_lead_unit", unit), close)
            },
            delete = target.id?.let { id -> { model.deleteEvent(id, close) } },
        )
    }
}

@Composable
private fun EditorDialog(title: String, close: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Ink) {
            LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text("CREATE / EDIT", color = Amber, fontSize = 11.sp, fontWeight = FontWeight.Black); Text(title, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold) }
                        TextButton(onClick = close) { Text("Close", color = Cyan) }
                    }
                }
                item { InfoCard(content) }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
private fun StudioField(label: String, value: String, singleLine: Boolean = true, update: (String) -> Unit) {
    OutlinedTextField(value, update, label = { Text(label) }, singleLine = singleLine, minLines = if (singleLine) 1 else 3, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun EditorActions(canSave: Boolean, save: () -> Unit, delete: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StudioButton(onClick = save, enabled = canSave, modifier = Modifier.weight(1f)) { Text("Save Offline", color = Ink, fontWeight = FontWeight.Black) }
        if (delete != null) StudioButton(onClick = delete, kind = StudioButtonKind.Danger) { Text("Delete", color = Color.White, fontWeight = FontWeight.Bold) }
    }
    Text("This change is stored on this device immediately and synchronized when a connection is available.", color = TextSoft, fontSize = 11.sp)
}

@Composable
private fun EventCard(event: JSONObject, readiness: PacketReadiness, open: () -> Unit, edit: (() -> Unit)? = null) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = open),
        colors = CardDefaults.cardColors(containerColor = PanelRaised),
        border = BorderStroke(1.dp, Color(0xFF343B4D)),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp)) {
            Box(Modifier.width(4.dp).height(82.dp).background(Brush.verticalGradient(listOf(Amber, Cyan)), RoundedCornerShape(50)))
            Column(Modifier.weight(1f).padding(start = 12.dp, end = 4.dp)) {
                Text(listOf(event.optString("event_date"), event.optString("start_time")).filter(String::isNotBlank).joinToString(" / "), color = TextSoft, fontSize = 13.sp)
                Text(event.optString("title", "Untitled session"), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(listOf(event.optString("event_type").humanize(), event.optString("location")).filter(String::isNotBlank).joinToString(" - "), color = TextSoft)
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
                if (edit != null) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = edit) { Text("Edit", color = Amber) }
                }
            }
        }
    }
}

@Composable
private fun GigModeScreen(
    model: StudioRackViewModel,
    eventId: String,
    hardwareKeys: Flow<Int>,
    onGigModeActive: (Boolean) -> Unit,
    back: () -> Unit,
) {
    val events by model.events.collectAsState()
    val songs by model.songs.collectAsState()
    val setLists by model.setLists.collectAsState()
    val sections by model.sections.collectAsState()
    val entries by model.entries.collectAsState()
    val attachments by model.attachments.collectAsState()
    val cachedAttachments by model.cachedAttachments.collectAsState()
    val syncState by model.syncState.collectAsState()
    val settings = remember(syncState?.performanceSettingsJson) {
        PerformanceSettings.fromJson(syncState?.performanceSettingsJson ?: "{}")
    }
    val metronome = remember { NativeMetronome() }
    val metronomeState by metronome.state.collectAsState()
    val listState = rememberLazyListState()
    val event = events.firstOrNull { it.entityId == eventId }?.let(::recordJson) ?: JSONObject()
    val setListId = event.optString("set_list_id")
    val setList = setLists.firstOrNull { it.entityId == setListId }?.let(::recordJson)
    val songMap = songs.associate { it.entityId to recordJson(it) }
    val sectionRows = sections.map(::recordJson).filter { it.optString("set_list_id") == setListId }.sortedBy { it.optInt("position") }
    val entryRows = entries.map(::recordJson).filter { it.optString("set_list_id") == setListId }.groupBy { it.optString("section_id") }
    val attachmentsBySong = attachments.map(::recordJson).groupBy { it.optString("song_id") }
    val cacheById = cachedAttachments.associateBy(CachedAttachment::attachmentId)
    val performanceSongs = sectionRows.flatMap { section ->
        entryRows[section.optString("id")].orEmpty().sortedBy { it.optInt("position") }.map { entry ->
            val song = songMap[entry.optString("song_id")]
            val attachment = selectPerformanceAttachment(entry, attachmentsBySong[entry.optString("song_id")].orEmpty())
            GigSong(section.optString("name", "Set"), entry, song, attachment, attachment?.optString("id")?.let(cacheById::get))
        }
    }
    var currentSong by remember(eventId) { mutableIntStateOf(0) }
    var detailOpen by remember(eventId) { mutableStateOf(false) }

    DisposableEffect(settings.pedalEnabled) {
        onGigModeActive(settings.pedalEnabled)
        onDispose {
            onGigModeActive(false)
            metronome.close()
        }
    }
    LaunchedEffect(settings.metronomeMuted) { metronome.setMuted(settings.metronomeMuted) }
    LaunchedEffect(currentSong, performanceSongs.size, settings.metronomeMode, settings.metronomeSound) {
        performanceSongs.getOrNull(currentSong)?.song?.let { song ->
            metronome.configure(song.optString("tempo"), song.optString("time_signature"), settings.metronomeMode, settings.metronomeSound)
            if (settings.metronomeAutostart) metronome.start()
        }
    }
    LaunchedEffect(settings, detailOpen, performanceSongs.size) {
        hardwareKeys.collect { keyCode ->
            if (!settings.pedalEnabled) return@collect
            when (mappedPedalAction(keyCode, settings)) {
                PedalAction.METRONOME -> metronome.toggle()
                PedalAction.MUTE -> metronome.toggleMuted()
                PedalAction.PREVIOUS, PedalAction.NEXT -> {
                    val action = mappedPedalAction(keyCode, settings) ?: return@collect
                    val direction = if (action == PedalAction.PREVIOUS) -1 else 1
                    if (!detailOpen && settings.pedalMode == "scroll") {
                        val fraction = when (settings.pedalScrollAmount) { "small" -> 0.2f; "full" -> 0.85f; else -> 0.5f }
                        listState.scrollBy(listState.layoutInfo.viewportSize.height * fraction * direction)
                    } else if (performanceSongs.isNotEmpty()) {
                        currentSong = (currentSong + direction).coerceIn(0, performanceSongs.lastIndex)
                        if (!detailOpen) listState.animateScrollToItem(gigListItemIndex(currentSong, performanceSongs))
                    }
                }
                null -> Unit
            }
        }
    }

    if (detailOpen && performanceSongs.isNotEmpty()) {
        PerformanceSongScreen(
            item = performanceSongs[currentSong],
            position = currentSong,
            total = performanceSongs.size,
            metronome = metronome,
            metronomeState = metronomeState,
            previousItem = performanceSongs.getOrNull(currentSong - 1),
            nextItem = performanceSongs.getOrNull(currentSong + 1),
            close = { detailOpen = false },
            previous = { currentSong = (currentSong - 1).coerceAtLeast(0) },
            next = { currentSong = (currentSong + 1).coerceAtMost(performanceSongs.lastIndex) },
        )
        return
    }
    LazyColumn(
        Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF120D08), Ink, Color(0xFF07131B)))).statusBarsPadding().navigationBarsPadding().padding(horizontal = 14.dp),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Surface(color = Color(0xF207090F), shape = RoundedCornerShape(bottomStart = 7.dp, bottomEnd = 7.dp), border = BorderStroke(1.dp, Color(0x2EFF9D1E))) {
                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    GigCircleButton("<", back)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text("SET LIST", color = TextSoft, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        Text(setList?.optString("name")?.ifBlank { null } ?: event.optString("title", "Set List"), color = Color.White, fontFamily = FontFamily.Serif, fontSize = 22.sp, maxLines = 1)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(event.optString("location"), color = TextSoft, fontSize = 11.sp, maxLines = 1)
                        Text(listOf(event.optString("event_date"), event.optString("start_time")).filter(String::isNotBlank).joinToString("  "), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                GigPill("List", active = true)
                Spacer(Modifier.width(7.dp))
                GigPill("Chart", onClick = { if (performanceSongs.isNotEmpty()) detailOpen = true })
            }
        }
        sectionRows.forEach { section ->
            item {
                Text(
                    section.optString("name", "Set"), color = Amber, fontFamily = FontFamily.Serif, fontSize = 34.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
            items(entryRows[section.optString("id")].orEmpty().sortedBy { it.optInt("position") }) { entry ->
                val song = songMap[entry.optString("song_id")]
                val attachment = selectPerformanceAttachment(entry, attachmentsBySong[entry.optString("song_id")].orEmpty())
                val cached = attachment?.optString("id")?.let(cacheById::get)
                SongRow(entry, song, attachment, cached) {
                    currentSong = performanceSongs.indexOfFirst { it.entry.optString("id") == entry.optString("id") }.coerceAtLeast(0)
                    detailOpen = true
                }
            }
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun SongRow(entry: JSONObject, song: JSONObject?, attachment: JSONObject?, cached: CachedAttachment?, openAttachment: () -> Unit) {
    val context = LocalContext.current
    val availableOffline = cached?.status == "ready" && cached.localPath != null
    val mediaLink = normalizedMediaLink(song?.optString("media_ref").orEmpty())
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xE8202635)),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = openAttachment),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val compact = maxWidth < 650.dp
                Column {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text((entry.optInt("position") + 1).toString(), color = Color.White, fontSize = 18.sp, modifier = Modifier.padding(end = 12.dp))
                        Text(song?.optString("title")?.takeIf(String::isNotBlank) ?: entry.optString("manual_title", "Untitled"), color = Amber, fontFamily = FontFamily.Serif, fontSize = 27.sp, modifier = Modifier.weight(1f))
                        if (!compact) GigSongCues(song)
                    }
                    if (compact) Row(Modifier.fillMaxWidth().padding(top = 7.dp), horizontalArrangement = Arrangement.End) { GigSongCues(song) }
                }
            }
            Text(song?.optString("artist").orEmpty(), color = TextSoft, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 30.dp))
            val patch = listOf(song?.optString("patch_name"), song?.optString("patch_number")).filterNotNull().filter(String::isNotBlank).joinToString(" / ")
            if (patch.isNotBlank()) Text("Patch: $patch", color = TextSoft, fontSize = 12.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, modifier = Modifier.align(Alignment.End).padding(top = 4.dp))
            if (attachment != null || mediaLink != null) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                    if (attachment != null) {
                        GigPill(if (availableOffline) attachmentLabel(attachment) else "${attachmentLabel(attachment)} unavailable", onClick = openAttachment)
                    }
                    if (attachment != null && mediaLink != null) Spacer(Modifier.width(7.dp))
                    mediaLink?.let { link -> GigPill("Listen", onClick = { openMediaLink(context, link) }) }
                }
            }
        }
    }
}

@Composable
private fun GigSongCues(song: JSONObject?) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(song?.optString("starts_by"), song?.optString("style")).filterNotNull().filter(String::isNotBlank).forEach {
            Text(it, color = Color.White, fontSize = 12.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
        }
        listOf(song?.optString("tempo"), song?.optString("time_signature")).filterNotNull().filter(String::isNotBlank).forEach { GigValueChip(it) }
    }
}

@Composable
private fun PerformanceSongScreen(
    item: GigSong,
    position: Int,
    total: Int,
    metronome: NativeMetronome,
    metronomeState: com.manoogianmedia.studiorack.performance.MetronomeState,
    previousItem: GigSong?,
    nextItem: GigSong?,
    close: () -> Unit,
    previous: () -> Unit,
    next: () -> Unit,
) {
    val context = LocalContext.current
    val mediaLink = normalizedMediaLink(item.song?.optString("media_ref").orEmpty())
    val path = item.cache?.localPath.orEmpty()
    val isPdf = item.cache?.mimeType == "application/pdf" || path.endsWith(".pdf", true)
    val pageCount = remember(path, isPdf) { if (isPdf) pdfPageCount(path) else 1 }
    var page by remember(path) { mutableIntStateOf(0) }
    val rendered by produceState(initialValue = AttachmentRender(), path, page) {
        val bitmap = withContext(Dispatchers.IO) {
            if (path.isBlank()) null else if (isPdf) renderPdfPage(path, page) else decodeAttachmentImage(path)
        }
        value = AttachmentRender(bitmap = bitmap, complete = true)
    }
    Column(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF120D08), Ink, Color(0xFF07131B)))).statusBarsPadding().navigationBarsPadding().padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            GigCircleButton("<", close)
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GigCircleButton("<", previous, position > 0)
                    Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                        Text(item.sectionName, color = Color.White, fontWeight = FontWeight.Bold)
                        Text("SONG ${position + 1} OF $total", color = TextSoft, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        nextItem?.let { Text("> ${gigSongTitle(it)}  ${gigSongCue(it)}", color = Amber, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
                        previousItem?.let { Text("< ${gigSongTitle(it)}", color = TextSoft, fontSize = 9.sp, maxLines = 1) }
                    }
                    GigCircleButton(">", next, position < total - 1)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                mediaLink?.let { link -> GigPill("Listen", onClick = { openMediaLink(context, link) }) }
                GigCircleButton(if (metronomeState.running) "||" else "♪", metronome::toggle)
            }
        }
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(item.song?.optString("title") ?: item.entry.optString("manual_title", "Untitled"), color = Color.White, fontFamily = FontFamily.Serif, fontSize = 34.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text(item.song?.optString("artist").orEmpty(), color = TextSoft, fontFamily = FontFamily.Serif, fontSize = 21.sp)
        }
        Box(
            Modifier.fillMaxWidth().height(5.dp).padding(top = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                Modifier.fillMaxWidth().height(if (metronomeState.pulse) 5.dp else 2.dp),
                color = if (metronomeState.downbeat) Cyan else Amber,
            ) {}
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GigDetail("Starts", item.song?.optString("starts_by").orEmpty())
            GigDetail("Tempo", item.song?.optString("tempo").orEmpty())
            GigDetail("Time", item.song?.optString("time_signature").orEmpty())
            GigDetail("Style", item.song?.optString("style").orEmpty())
            val patch = listOf(item.song?.optString("patch_name"), item.song?.optString("patch_number")).filterNotNull().filter(String::isNotBlank).joinToString(" / ")
            GigDetail("Patch", patch)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            GigPill(if (metronomeState.muted) "Unmute" else "Mute", onClick = metronome::toggleMuted)
        }
        val entryNote = item.entry.optString("entry_notes")
        val songNote = item.song?.optString("notes").orEmpty()
        if (entryNote.isNotBlank() || songNote.isNotBlank()) {
            Surface(color = Color(0x0FFFFFFF), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Amber.copy(alpha = 0.16f)), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(Modifier.padding(12.dp)) {
                    if (entryNote.isNotBlank()) Text("Set Note: $entryNote", color = TextSoft)
                    if (songNote.isNotBlank()) Text("Song Note: $songNote", color = TextSoft)
                }
            }
        }
        if (item.attachment != null) {
            Text(attachmentLabel(item.attachment) + if (pageCount > 1) "  |  Page ${page + 1} of $pageCount" else "", color = TextSoft, fontSize = 12.sp)
        }
        if (pageCount > 1) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                StudioButton(onClick = { page = (page - 1).coerceAtLeast(0) }, enabled = page > 0, kind = StudioButtonKind.Secondary) { Text("Previous page", color = Color.White) }
                Spacer(Modifier.size(6.dp))
                StudioButton(onClick = { page = (page + 1).coerceAtMost(pageCount - 1) }, enabled = page < pageCount - 1) { Text("Next page", color = Ink, fontWeight = FontWeight.Black) }
            }
        }
        Box(Modifier.fillMaxSize().padding(top = 10.dp), contentAlignment = Alignment.Center) {
            val renderedBitmap = rendered.bitmap
            when {
                !rendered.complete -> CircularProgressIndicator()
                renderedBitmap == null -> SongDetailFallback(item)
                else -> Image(renderedBitmap.asImageBitmap(), contentDescription = item.attachment?.let(::attachmentLabel), modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
        }
    }
}

@Composable
private fun GigCircleButton(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    Surface(
        color = Amber.copy(alpha = if (enabled) 1f else 0.28f), contentColor = Ink, shape = RoundedCornerShape(50),
        modifier = Modifier.size(46.dp).clickable(enabled = enabled, onClick = onClick),
    ) { Box(contentAlignment = Alignment.Center) { Text(label, fontSize = if (label == "♪") 24.sp else 20.sp, fontWeight = FontWeight.Black) } }
}

@Composable
private fun GigValueChip(value: String) {
    Surface(color = Amber.copy(alpha = 0.13f), shape = RoundedCornerShape(6.dp), border = BorderStroke(1.dp, Color(0x2FFFFFFF))) {
        Text(value, color = Color.White, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
    }
}

@Composable
private fun GigPill(label: String, active: Boolean = false, onClick: (() -> Unit)? = null) {
    Surface(
        color = if (active) Amber else Amber.copy(alpha = 0.14f), contentColor = if (active) Ink else Amber,
        shape = RoundedCornerShape(50), border = BorderStroke(1.dp, Amber.copy(alpha = if (active) 1f else 0.52f)),
        modifier = if (onClick == null) Modifier else Modifier.clickable(onClick = onClick),
    ) { Text(label.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)) }
}

@Composable
private fun GigDetail(label: String, value: String) {
    Column(Modifier.width(118.dp).padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label.uppercase(), color = TextSoft, fontSize = 9.sp, fontWeight = FontWeight.Black)
        Text(value.ifBlank { "Not set" }, color = Color.White, fontFamily = FontFamily.Serif, fontSize = 20.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

private fun gigSongTitle(item: GigSong) = item.song?.optString("title")?.takeIf(String::isNotBlank) ?: item.entry.optString("manual_title", "Untitled")
private fun gigSongCue(item: GigSong) = listOf(item.song?.optString("starts_by"), item.song?.optString("tempo"), item.song?.optString("time_signature")).filterNotNull().filter(String::isNotBlank).joinToString(" / ")

internal fun normalizedMediaLink(value: String): String? {
    val link = value.trim()
    if (link.isBlank()) return null
    val scheme = runCatching { java.net.URI(link).scheme?.lowercase() }.getOrNull()
    return link.takeIf { scheme in setOf("http", "https", "spotify") }
}

private fun openMediaLink(context: Context, link: String) {
    val intent = mediaIntent(link) ?: return
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No application is available to open this media link.", Toast.LENGTH_LONG).show()
    } catch (_: SecurityException) {
        Toast.makeText(context, "StudioRack could not open this media link.", Toast.LENGTH_LONG).show()
    }
}

internal fun mediaIntent(link: String): Intent? = normalizedMediaLink(link)?.let { safeLink ->
    Intent(Intent.ACTION_VIEW, Uri.parse(safeLink))
}

@Composable
private fun SongDetailFallback(item: GigSong) {
    Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(item.song?.optString("artist").orEmpty(), color = Cyan, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(listOf("Starts: ${item.song?.optString("starts_by").orEmpty()}", "Tempo: ${item.song?.optString("tempo").orEmpty()}", "Time: ${item.song?.optString("time_signature").orEmpty()}", "Style: ${item.song?.optString("style").orEmpty()}").joinToString("  |  "), color = Color.White)
        val patch = listOf(item.song?.optString("patch_name"), item.song?.optString("patch_number")).filterNotNull().filter(String::isNotBlank).joinToString(" / ")
        if (patch.isNotBlank()) Text("Patch: $patch", color = Amber)
        item.song?.optString("notes")?.takeIf(String::isNotBlank)?.let { Text(it, color = TextSoft) }
        if (item.attachment != null && item.cache?.status != "ready") Text("${attachmentLabel(item.attachment)} is not available offline.", color = TextSoft)
    }
}

@Composable
private fun EmptyCard(text: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) { Text(text, color = TextSoft) }
}

private fun recordJson(record: CachedRecord): JSONObject = runCatching { JSONObject(record.json) }.getOrDefault(JSONObject())

private data class GigSong(
    val sectionName: String,
    val entry: JSONObject,
    val song: JSONObject?,
    val attachment: JSONObject?,
    val cache: CachedAttachment?,
)
private data class PacketReadiness(val ready: Int, val total: Int)
private data class AttachmentRender(val bitmap: Bitmap? = null, val complete: Boolean = false)

private fun gigListItemIndex(songIndex: Int, songs: List<GigSong>): Int {
    val sectionHeaders = songs.take(songIndex + 1).map(GigSong::sectionName).distinct().size
    return 1 + sectionHeaders + songIndex
}

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
