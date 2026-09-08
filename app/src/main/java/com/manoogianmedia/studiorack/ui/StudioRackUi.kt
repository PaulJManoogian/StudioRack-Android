package com.manoogianmedia.studiorack.ui

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Network
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.Build
import android.content.pm.PackageManager
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.res.stringResource
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
import com.manoogianmedia.studiorack.data.DataExport
import com.manoogianmedia.studiorack.data.SupportingRecord
import com.manoogianmedia.studiorack.data.SongAttachmentInput
import com.manoogianmedia.studiorack.data.NotificationRoute
import com.manoogianmedia.studiorack.data.cacheImageFile
import com.manoogianmedia.studiorack.performance.NativeMetronome
import com.manoogianmedia.studiorack.performance.PedalAction
import com.manoogianmedia.studiorack.performance.PerformanceSettings
import com.manoogianmedia.studiorack.performance.mappedPedalAction
import com.manoogianmedia.studiorack.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import java.text.DateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
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
fun StudioRackApp(
    model: StudioRackViewModel,
    hardwareKeys: Flow<Int>,
    notificationRoutes: Flow<NotificationRoute>,
    onGigModeActive: (Boolean) -> Unit,
) {
    val uiState by model.uiState.collectAsState()
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) model.refreshNotifications()
    }
    LaunchedEffect(uiState.signedIn) {
        if (uiState.signedIn && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
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
                uiState.starting -> StudioRackSplash()
                !uiState.signedIn -> LoginScreen(model, uiState)
                selectedEvent != null -> GigModeScreen(model, selectedEvent!!, hardwareKeys, onGigModeActive) { selectedEvent = null }
                else -> MainShell(model, uiState, notificationRoutes) { selectedEvent = it }
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
private fun MainShell(
    model: StudioRackViewModel,
    uiState: StudioRackUiState,
    notificationRoutes: Flow<NotificationRoute>,
    openGig: (String) -> Unit,
) {
    var section by remember { mutableStateOf(AppSection.DASHBOARD) }
    val pending by model.pendingCount.collectAsState()
    val conflicts by model.conflicts.collectAsState()
    val syncHealth by model.syncHealth.collectAsState()
    val notificationCount by model.notificationCount.collectAsState()
    val productName = stringResource(R.string.app_name)
    LaunchedEffect(notificationRoutes) {
        notificationRoutes.collect { route ->
            section = when (route.destination) {
                "equipment" -> AppSection.EQUIPMENT
                "sessions" -> AppSection.SESSIONS
                else -> AppSection.DASHBOARD
            }
        }
    }
    val online = rememberNetworkConnected()
    val connection = connectionBanner(online, uiState.busy || syncHealth.running, uiState.syncError || syncHealth.error != null, pending)
    Scaffold(
        containerColor = Ink,
        topBar = {
            Surface(modifier = Modifier.statusBarsPadding(), color = Color(0xF20A0D15), shadowElevation = 8.dp) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.brand_logo), productName, Modifier.size(38.dp))
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(productName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text(connection.label, color = when (connection.kind) { ConnectionKind.ONLINE -> Color(0xFF63E6A4); ConnectionKind.OFFLINE -> Cyan; ConnectionKind.WARNING -> Amber; ConnectionKind.ERROR -> Color(0xFFFF6B6B) }, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                    if (conflicts.isNotEmpty()) Surface(color = Color(0xFF8B2F3A), shape = RoundedCornerShape(8.dp)) {
                        Text("${conflicts.size} CONFLICT${if (conflicts.size == 1) "" else "S"}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
                    }
                    if (notificationCount > 0) Surface(
                        modifier = Modifier.padding(start = 7.dp).clickable { section = AppSection.DASHBOARD },
                        color = Amber,
                        contentColor = Ink,
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            "$notificationCount ALERT${if (notificationCount == 1) "" else "S"}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        },
        bottomBar = {
            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xF20A0D15))
                    .navigationBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
            ) {
                val rows = if (maxWidth >= 600.dp) listOf(AppSection.entries.toList()) else AppSection.entries.toList().chunked(3)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rows.forEach { destinations ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            destinations.forEach { destination ->
                                StudioNavPill(
                                    destination = destination,
                                    selected = section == destination,
                                    onClick = { section = destination },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
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
private fun StudioNavPill(destination: AppSection, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(48.dp).clickable(onClick = onClick),
        color = if (selected) Amber else PanelRaised,
        contentColor = if (selected) Ink else Color.White,
        border = BorderStroke(1.dp, if (selected) Amber else Color(0x33FFFFFF)),
        shape = RoundedCornerShape(50),
        shadowElevation = if (selected) 5.dp else 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(destination.label, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun StudioRackSplash() {
    val productName = stringResource(R.string.app_name)
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(Color(0xFF183246), Ink), radius = 1100f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(112.dp),
                color = Panel.copy(alpha = 0.92f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Amber.copy(alpha = 0.7f)),
                shadowElevation = 12.dp,
            ) {
                Image(
                    painterResource(R.drawable.brand_logo),
                    productName,
                    Modifier.padding(20.dp).fillMaxSize(),
                )
            }
            Spacer(Modifier.height(22.dp))
            Text(productName, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
            Text("SYNCING YOUR STUDIO", color = Amber, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(22.dp))
            CircularProgressIndicator(color = Cyan, strokeWidth = 3.dp, modifier = Modifier.size(34.dp))
        }
    }
}

@Composable
private fun LoginScreen(model: StudioRackViewModel, uiState: StudioRackUiState) {
    val productName = stringResource(R.string.app_name)
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var mfa by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.brand_logo), productName, Modifier.size(54.dp))
            Text(productName, color = Amber, fontSize = 38.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 10.dp))
        }
        Text("Your performance library, available offline.", color = TextSoft)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(code, { code = it }, label = { Text("$productName access code") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
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
    val productName = stringResource(R.string.app_name)
    val agentName = stringResource(R.string.agent_name)
    val events by model.events.collectAsState()
    val items by model.items.collectAsState()
    val kits by model.kits.collectAsState()
    val brands by model.brands.collectAsState()
    val locations by model.locations.collectAsState()
    val specs by model.itemSpecs.collectAsState()
    val actions by model.buddyActions.collectAsState()
    val maintenanceNotes by model.maintenanceNotes.collectAsState()
    val entries by model.entries.collectAsState()
    val attachments by model.attachments.collectAsState()
    val cachedAttachments by model.cachedAttachments.collectAsState()
    val state by model.syncState.collectAsState()
    val account = runCatching { JSONObject(state?.accountJson ?: "{}") }.getOrDefault(JSONObject())
    val upcoming = events.map(::recordJson).filter { it.optString("event_status") != "ended" }.sortedBy { it.optString("event_date") + it.optString("start_time") }
    val specRows = specs.map(::supportingJson)
    val purchaseTotal = specRows.filter { it.optString("key") == "purchase_price" }.sumOf { it.optString("value").toDoubleOrNull() ?: 0.0 }
    val maintenanceHistory by model.maintenanceHistory.collectAsState()
    val careRows = maintenanceRows(specRows, items.map(::supportingJson), brands.map(::supportingJson), locations.map(::supportingJson), fieldNotes = maintenanceNotes.map(::recordJson), completions = maintenanceHistory.filter { it.revision == 0 }.map(::recordJson))
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
                                Text(account.optString("studio_name").ifBlank { account.optString("organization", productName) }, color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
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
        item { CareSummary(careRows, model) }
        item { SectionHeading(agentName.uppercase(), "Recent activity") }
        if (actions.isEmpty()) item { EmptyCard("No $agentName actions are stored on this device.") }
        items(actions.take(5), key = { it.entityId }) { action -> BuddyActionCard(supportingJson(action)) }
        item { Spacer(Modifier.height(30.dp)) }
    }
}

@Composable
private fun EquipmentScreen(model: StudioRackViewModel) {
    val items by model.items.collectAsState()
    val specs by model.itemSpecs.collectAsState()
    val units by model.itemUnits.collectAsState()
    val maintenanceNotes by model.maintenanceNotes.collectAsState()
    val categories by model.categories.collectAsState()
    val types by model.itemTypes.collectAsState()
    val locations by model.locations.collectAsState()
    val reportState by model.reportState.collectAsState()
    val online = rememberNetworkConnected()
    var query by remember { mutableStateOf("") }
    var addingMaintenance by remember { mutableStateOf(false) }
    var exportTarget by remember { mutableStateOf<ExportTarget?>(null) }
    val categoryNames = categories.associate { it.entityId to supportingJson(it).optString("name") }
    val typeNames = types.associate { it.entityId to supportingJson(it).optString("name") }
    val locationNames = locations.associate { it.entityId to supportingJson(it).optString("name") }
    val filtered = items.filter { supportingJson(it).let { row -> query.isBlank() || row.toString().contains(query, true) } }
    Box(Modifier.fillMaxSize()) {
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SectionHeading("EQUIPMENT", "Items") }
        item {
            StudioButton(onClick = { addingMaintenance = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Add Maintenance Note", color = Ink, fontWeight = FontWeight.Black)
            }
        }
        item { DictationTextField(query, { query = it }, "Find an item") }
        item {
            StudioButton(
                onClick = { exportTarget = ExportTarget("items", "Visible equipment", filtered.map { it.entityId }) },
                enabled = filtered.isNotEmpty() && !reportState.busy,
                modifier = Modifier.fillMaxWidth(),
                kind = StudioButtonKind.Secondary,
            ) { Text("Export visible equipment (${filtered.size})", color = Color.White, fontWeight = FontWeight.Bold) }
        }
        if (filtered.isEmpty()) item { EmptyCard("No equipment matches this search.") }
        items(filtered, key = { it.entityId }) { record ->
            val row = supportingJson(record)
            val itemSpecs = specs.filter { supportingJson(it).optString("item_id") == record.entityId }.map(::supportingJson)
            val itemUnits = units.filter { supportingJson(it).optString("item_id") == record.entityId }.map(::supportingJson)
            val fieldNotes = maintenanceNotes.filter { recordJson(it).optString("item_id") == record.entityId }.map(::recordJson)
            ExpandableRecordCard(
                title = row.optString("display_name", "Unnamed item"),
                subtitle = listOf(categoryNames[row.optString("category_id")], typeNames[row.optString("type_id")]).filterNotNull().filter(String::isNotBlank).joinToString(" / "),
                chips = listOf(row.optString("usage_status"), "Qty ${row.optInt("quantity", 1)}"),
                imageUrl = row.optString("image_url"),
            ) {
                DetailLine("Location", locationNames[row.optString("default_location_id")].orEmpty())
                DetailLine("Notes", row.optString("notes"))
                MaintenanceHistoryControl(record.entityId, row.optString("display_name", "Item"), model)
                fieldNotes.forEach { note -> DetailLine("Field note - ${note.optString("status", "pending").humanize()}", note.optString("note")) }
                itemUnits.forEach { unit -> DetailLine(unit.optString("unit_label", "Unit"), listOf(unit.optString("serial_number"), unit.optString("status")).filter(String::isNotBlank).joinToString(" / ")) }
                itemSpecs.sortedBy { it.optString("key") }.forEach { spec -> DetailLine(spec.optString("key").humanize(), spec.optString("value")) }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
    if (addingMaintenance) MaintenanceNoteEditor(items, model) { addingMaintenance = false }
    exportTarget?.let { target -> ContextExportDialog(target, online, reportState, model) { exportTarget = null } }
    }
}

@Composable
private fun MaintenanceHistoryControl(itemId: String, itemName: String, model: StudioRackViewModel) {
    var open by remember(itemId) { mutableStateOf(false) }
    StudioButton(onClick = { open = true }) { Text("Maintenance / History", color = Ink, fontWeight = FontWeight.Bold) }
    if (open) MaintenanceHistoryDialog(itemId, itemName, model) { open = false }
}

@Composable
private fun MaintenanceHistoryDialog(itemId: String, itemName: String, model: StudioRackViewModel, close: () -> Unit) {
    val history by model.maintenanceHistory.collectAsState()
    val notes by model.maintenanceNotes.collectAsState()
    val specs by model.itemSpecs.collectAsState()
    val itemHistory = history.filter { recordJson(it).optString("item_id") == itemId }
    val pending = itemHistory.filter { it.revision == 0 }.map(::recordJson)
    val values = projectedMaintenanceSpecs(specs.map(::supportingJson).filter { it.optString("item_id") == itemId }.associate { it.optString("key") to it.optString("value") }, pending)
    val activeNotes = notes.map(::recordJson).filter { it.optString("item_id") == itemId && it.optString("status") in setOf("pending", "notified", "rescheduled") && it.optString("id") !in resolvedNoteIds(pending) }
    var tab by remember { mutableStateOf("History") }
    var summary by remember { mutableStateOf("") }
    var performedBy by remember { mutableStateOf("") }
    var completedOn by remember { mutableStateOf(LocalDate.now().toString()) }
    var nextDue by remember { mutableStateOf("") }
    var selectedNotes by remember { mutableStateOf<Set<String>>(emptySet()) }
    var expectedDue by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    EditorDialog(itemName, close) {
        ChoiceStrip(listOf("History", "Complete Service"), tab) {
            if (it == "Complete Service" && tab != it) {
                selectedNotes = activeNotes.map { note -> note.optString("id") }.toSet()
                expectedDue = serviceDue(values)
            }
            tab = it
        }
        if (tab == "History") {
            if (itemHistory.isEmpty()) Text("No completed service records yet.", color = TextSoft)
            itemHistory.sortedWith(compareByDescending<CachedRecord> { recordJson(it).optString("completed_on") }.thenByDescending { recordJson(it).optString("created_utc") }).forEach { record ->
                val row = recordJson(record)
                DetailLine(row.optString("completed_on") + if (record.revision == 0) " - Pending sync" else "", row.optString("summary"))
                DetailLine("Performed by", row.optString("performed_by"))
            }
            activeNotes.forEach { DetailLine("Open reminder", it.optString("note")) }
            DetailLine("Last service", values["last_service_date"].orEmpty())
            DetailLine("Last service notes", values["last_service_notes"].orEmpty())
        } else {
            StudioField("Completed on (YYYY-MM-DD)", completedOn, dictation = false) { completedOn = it }
            StudioField("Work completed", summary, singleLine = false) { summary = it }
            StudioField("Performed by (optional)", performedBy) { performedBy = it }
            StudioField("Next service date (optional, YYYY-MM-DD)", nextDue, dictation = false) { nextDue = it }
            Text("Leave the next date blank to use the service interval, or clear a one-time reminder.", color = TextSoft)
            if (expectedDue.isNotBlank()) DetailLine("Scheduled service being completed", expectedDue)
            activeNotes.forEach { note ->
                val id = note.optString("id")
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = id in selectedNotes, onCheckedChange = { selectedNotes = if (it) selectedNotes + id else selectedNotes - id })
                    Text(note.optString("note"), color = Color.White, modifier = Modifier.weight(1f))
                }
            }
            val date = parseLocalDate(completedOn)
            val next = parseLocalDate(nextDue)
            val valid = date != null && !date.isAfter(LocalDate.now()) && (nextDue.isBlank() || (next != null && next.isAfter(date)))
            if (!valid) Text("Use a valid completion date, and a later date for the next service.", color = Amber)
            EditorActions(canSave = !saving && valid && summary.trim().isNotEmpty() && summary.length <= 4000, save = {
                saving = true
                model.completeMaintenance(JSONObject().put("item_id", itemId).put("completed_on", completedOn.trim())
                    .put("summary", summary.trim()).put("performed_by", performedBy.trim()).put("next_due", nextDue.trim())
                    .put("expected_due", expectedDue).put("resolved_note_ids", org.json.JSONArray(selectedNotes.toList()).toString())
                    .put("created_utc", java.time.Instant.now().toString()).put("source", "android")) { success -> saving = false; if (success) close() }
            }, delete = null)
        }
    }
}

@Composable
private fun MaintenanceNoteEditor(items: List<SupportingRecord>, model: StudioRackViewModel, close: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val matches = items.filter { query.isBlank() || supportingJson(it).optString("display_name").contains(query, true) }.take(12)
    EditorDialog("Add Maintenance Note", close) {
        Text("Equipment", color = TextSoft, fontWeight = FontWeight.Bold)
        StudioField("Find equipment", query) { query = it }
        Column(Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            matches.forEach { item ->
                val label = supportingJson(item).optString("display_name", "Unnamed item")
                val selected = selectedId == item.entityId
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { selectedId = item.entityId },
                    color = if (selected) Amber else Panel,
                    contentColor = if (selected) Ink else Color.White,
                    border = BorderStroke(1.dp, if (selected) Amber else Color(0x33FFFFFF)),
                    shape = RoundedCornerShape(8.dp),
                ) { Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp)) }
            }
        }
        StudioField("What needs attention?", note, singleLine = false) { note = it }
        EditorActions(
            canSave = selectedId.isNotBlank() && note.isNotBlank(),
            save = { model.saveMaintenanceNote(selectedId, note, close) },
            delete = null,
        )
    }
}

@Composable
private fun KitsScreen(model: StudioRackViewModel) {
    val kits by model.kits.collectAsState()
    val items by model.items.collectAsState()
    val members by model.kitMembers.collectAsState()
    val locations by model.locations.collectAsState()
    val reportState by model.reportState.collectAsState()
    val online = rememberNetworkConnected()
    var query by remember { mutableStateOf("") }
    var exportTarget by remember { mutableStateOf<ExportTarget?>(null) }
    val itemNames = items.associate { it.entityId to supportingJson(it).optString("display_name", "Item") }
    val locationNames = locations.associate { it.entityId to supportingJson(it).optString("name") }
    val filtered = kits.filter { query.isBlank() || supportingJson(it).toString().contains(query, true) }
    Box(Modifier.fillMaxSize()) {
    RecordListScreen("EQUIPMENT", "Kits", query, { query = it }, "Find a kit") {
        item {
            StudioButton(
                onClick = { exportTarget = ExportTarget("kits", "Visible kits", filtered.map { it.entityId }) },
                enabled = filtered.isNotEmpty() && !reportState.busy,
                modifier = Modifier.fillMaxWidth(),
                kind = StudioButtonKind.Secondary,
            ) { Text("Export visible kits (${filtered.size})", color = Color.White, fontWeight = FontWeight.Bold) }
        }
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
    exportTarget?.let { target -> ContextExportDialog(target, online, reportState, model) { exportTarget = null } }
    }
}

@Composable
private fun SessionsScreen(model: StudioRackViewModel, openGig: (String) -> Unit) {
    val events by model.events.collectAsState()
    val venues by model.venues.collectAsState()
    val entries by model.entries.collectAsState()
    val attachments by model.attachments.collectAsState()
    val cachedAttachments by model.cachedAttachments.collectAsState()
    val reportState by model.reportState.collectAsState()
    val online = rememberNetworkConnected()
    var query by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("All") }
    var editingEvent by remember { mutableStateOf<EditorTarget?>(null) }
    var exportTarget by remember { mutableStateOf<ExportTarget?>(null) }
    val venueNames = venues.associate { it.entityId to recordJson(it).optString("name") }
    val rows = events.map(::recordJson).onEach { event ->
        val venueName = venueNames[event.optString("venue_id")].orEmpty()
        if (venueName.isNotBlank()) event.put("location", listOf(venueName, event.optString("location")).filter(String::isNotBlank).joinToString(" - "))
    }.filter {
        (type == "All" || it.optString("event_type").humanize() == type) && (query.isBlank() || it.toString().contains(query, true))
    }.sortedBy { it.optString("event_date") + it.optString("start_time") }
    Box(Modifier.fillMaxSize()) {
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeading("SESSIONS", "Schedule") }
        item { StudioButton(onClick = { editingEvent = EditorTarget(null, JSONObject()) }, modifier = Modifier.fillMaxWidth()) { Text("Add Scheduled Event", color = Ink, fontWeight = FontWeight.Black) } }
        item { DictationTextField(query, { query = it }, "Find scheduled work") }
        item { ChoiceStrip(listOf("All", "Performance", "Rehearsal", "Studio Session", "Other"), type) { type = it } }
        item {
            StudioButton(
                onClick = { exportTarget = ExportTarget("events", "Visible scheduled items", rows.map { it.optString("id") }) },
                enabled = rows.isNotEmpty() && !reportState.busy,
                modifier = Modifier.fillMaxWidth(),
                kind = StudioButtonKind.Secondary,
            ) { Text("Export visible scheduled items (${rows.size})", color = Color.White, fontWeight = FontWeight.Bold) }
        }
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
    exportTarget?.let { target -> ContextExportDialog(target, online, reportState, model) { exportTarget = null } }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun LibraryScreen(model: StudioRackViewModel) {
    val context = LocalContext.current
    val songs by model.songs.collectAsState()
    val setLists by model.setLists.collectAsState()
    val sections by model.sections.collectAsState()
    val entries by model.entries.collectAsState()
    val attachments by model.attachments.collectAsState()
    val reportState by model.reportState.collectAsState()
    val online = rememberNetworkConnected()
    var tab by remember { mutableStateOf("Songs") }
    var query by remember { mutableStateOf("") }
    var favoriteScope by remember { mutableStateOf("All") }
    var sort by remember { mutableStateOf("A-Z") }
    var editingSong by remember { mutableStateOf<EditorTarget?>(null) }
    var editingSetList by remember { mutableStateOf<CachedRecord?>(null) }
    var creatingSetList by remember { mutableStateOf(false) }
    var renamingSetList by remember { mutableStateOf<CachedRecord?>(null) }
    var exportTarget by remember { mutableStateOf<ExportTarget?>(null) }
    val filteredSongs = songs
        .filter { query.isBlank() || recordJson(it).toString().contains(query, true) }
        .filter { favoriteScope == "All" || recordJson(it).optInt("is_favorite") == 1 }
        .sortedBy { recordJson(it).optString("title").lowercase() }
        .let { if (sort == "Z-A") it.reversed() else it }
    val filteredSetLists = setLists
        .filter { query.isBlank() || recordJson(it).toString().contains(query, true) }
        .filter { favoriteScope == "All" || recordJson(it).optInt("is_favorite") == 1 }
        .sortedBy { recordJson(it).optString("name").lowercase() }
        .let { if (sort == "Z-A") it.reversed() else it }
    Box(Modifier.fillMaxSize()) {
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeading("LIBRARY", "Songs and Set Lists") }
        item { ChoiceStrip(listOf("Songs", "Set Lists"), tab) { tab = it; query = ""; favoriteScope = "All"; sort = "A-Z" } }
        item {
            StudioButton(
                onClick = { if (tab == "Songs") editingSong = EditorTarget(null, JSONObject()) else creatingSetList = true },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (tab == "Songs") "Add Song" else "Create Set List", color = Ink, fontWeight = FontWeight.Black) }
        }
        item { DictationTextField(query, { query = it }, if (tab == "Songs") "Find a song" else "Find a set list") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                ChoiceStrip(listOf("All", "Favorites"), favoriteScope) { favoriteScope = it }
                ChoiceStrip(listOf("A-Z", "Z-A"), sort) { sort = it }
            }
        }
        if (tab == "Songs") {
            item {
                StudioButton(
                    onClick = { exportTarget = ExportTarget("songs", "Visible songs", filteredSongs.map { it.entityId }) },
                    enabled = filteredSongs.isNotEmpty() && !reportState.busy,
                    modifier = Modifier.fillMaxWidth(),
                    kind = StudioButtonKind.Secondary,
                ) { Text("Export visible songs (${filteredSongs.size})", color = Color.White, fontWeight = FontWeight.Bold) }
            }
            items(filteredSongs, key = { it.entityId }) { record ->
                val song = recordJson(record)
                val songAttachments = attachments.filter { recordJson(it).optString("song_id") == record.entityId }
                ExpandableRecordCard(
                    song.optString("title", "Untitled song"), song.optString("artist"),
                    listOf(song.optString("style"), song.optString("tempo"), song.optString("time_signature"), formatDuration(song.optInt("duration_seconds")), if (song.optInt("is_favorite") == 1) "Favorite" else "").filter(String::isNotBlank),
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
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { editingSong = EditorTarget(record.entityId, song) }) { Text("Edit", color = Amber) }
                    }
                }
            }
        } else {
            val songNames = songs.associate { it.entityId to recordJson(it).optString("title", "Song") }
            item {
                StudioButton(
                    onClick = { exportTarget = ExportTarget("setlists", "Visible set lists", filteredSetLists.map { it.entityId }) },
                    enabled = filteredSetLists.isNotEmpty() && !reportState.busy,
                    modifier = Modifier.fillMaxWidth(),
                    kind = StudioButtonKind.Secondary,
                ) { Text("Export visible set lists (${filteredSetLists.size})", color = Color.White, fontWeight = FontWeight.Bold) }
            }
            items(filteredSetLists, key = { it.entityId }) { record ->
                val row = recordJson(record)
                val setSections = sections.filter { recordJson(it).optString("set_list_id") == record.entityId }
                val setEntries = entries.filter { recordJson(it).optString("set_list_id") == record.entityId }
                val durationBySong = songs.associate { it.entityId to recordJson(it).optInt("duration_seconds") }
                val estimatedSeconds = setEntries.sumOf { durationBySong[recordJson(it).optString("song_id")] ?: 0 }
                ExpandableRecordCard(
                    row.optString("name", "Unnamed set list"), row.optString("description"),
                    listOf("${setSections.size} sets", "${setEntries.size} songs", formatDuration(estimatedSeconds)).filter(String::isNotBlank),
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
                        TextButton(onClick = { exportTarget = ExportTarget("setlists", row.optString("name", "Set List"), listOf(record.entityId)) }) { Text("Export", color = Amber) }
                        TextButton(onClick = { renamingSetList = record }) { Text("Rename", color = Amber) }
                    }
                }
            }
        }
    }
    editingSong?.let { target ->
        SongEditor(target, model, close = { editingSong = null })
    }
    renamingSetList?.let { record ->
        var name by remember(record.entityId) { mutableStateOf(recordJson(record).optString("name")) }
        EditorDialog("Rename Set List", { renamingSetList = null }) {
            StudioField("Name", name) { name = it }
            EditorActions(canSave = name.trim().isNotEmpty() && name.trim().length <= 200,
                save = { model.renameSetList(record, name) { renamingSetList = null } }, delete = null)
        }
    }
    if (creatingSetList || editingSetList != null) {
        Dialog(onDismissRequest = { creatingSetList = false; editingSetList = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            SetListEditor(editingSetList, sections, entries, songs, attachments, model) { creatingSetList = false; editingSetList = null }
        }
    }
    exportTarget?.let { target -> ContextExportDialog(target, online, reportState, model) { exportTarget = null } }
    }
}

@Composable
private fun MoreScreen(model: StudioRackViewModel, uiState: StudioRackUiState) {
    val productName = stringResource(R.string.app_name)
    val agentName = stringResource(R.string.agent_name)
    var tab by remember { mutableStateOf("Reports") }
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeading(productName.uppercase(), "More") }
        item { ChoiceStrip(listOf("Reports", "Sharing", "People", agentName, "Reference", "Sync", "Settings"), tab) { tab = it } }
        when (tab) {
            "Reports" -> reportsContent(model)
            "Sharing" -> sharingContent(model)
            "People" -> directoryContent(model)
            agentName -> buddyContent(model)
            "Reference" -> referenceContent(model)
            "Sync" -> syncContent(model, uiState)
            else -> settingsContent(model, uiState)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.sharingContent(model: StudioRackViewModel) {
    item { SharingPanel(model) }
}

@Composable
private fun SharingPanel(model: StudioRackViewModel) {
    var tab by remember { mutableStateOf("Shared With Me") }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeading("COLLABORATION", "Sharing")
        ChoiceStrip(listOf("Shared With Me", "My Shares"), tab) { tab = it }
        if (tab == "My Shares") MySharesPanel(model) else SharedWithMePanel(model, showHeading = false)
    }
}

@Composable
private fun SharedWithMePanel(model: StudioRackViewModel, showHeading: Boolean = true) {
    val productName = stringResource(R.string.app_name)
    val accessRows by model.sharedAccess.collectAsState()
    val events by model.sharedEvents.collectAsState()
    val venues by model.sharedVenues.collectAsState()
    val setLists by model.sharedSetLists.collectAsState()
    val sections by model.sharedSetListSections.collectAsState()
    val entries by model.sharedSetListEntries.collectAsState()
    val songs by model.sharedSongs.collectAsState()
    val attachments by model.sharedAttachments.collectAsState()
    val cached by model.cachedAttachments.collectAsState()
    val context = LocalContext.current
    var selectedGrant by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<CachedAttachment?>(null) }
    val cacheById = cached.associateBy(CachedAttachment::attachmentId)

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (showHeading) SectionHeading("COLLABORATION", "Shared With Me")
        Text("Live sessions and set lists shared with your account remain available from the last successful sync.", color = TextSoft)
        if (accessRows.isEmpty()) EmptyCard("Nothing has been shared with this account.")
        accessRows.forEach { record ->
            val access = supportingJson(record)
            val grantId = access.optString("grant_id", record.entityId)
            val event = events.map(::supportingJson).firstOrNull { it.optString("grant_id") == grantId }
            val setList = setLists.map(::supportingJson).firstOrNull { it.optString("grant_id") == grantId }
            val title = event?.optString("title")?.takeIf(String::isNotBlank)
                ?: setList?.optString("name")?.takeIf(String::isNotBlank) ?: "Shared item"
            InfoCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text("From ${access.optString("owner_organization").ifBlank { access.optString("owner_name", "Another $productName user") }}", color = Cyan)
                        Text("${access.optString("access_role", "performer").humanize()} access | Expires ${access.optString("expires_utc")}", color = TextSoft, fontSize = 12.sp)
                    }
                    StudioButton(onClick = { selectedGrant = if (selectedGrant == grantId) null else grantId }, kind = StudioButtonKind.Secondary) {
                        Text(if (selectedGrant == grantId) "Close" else "Open", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                if (selectedGrant == grantId) {
                    event?.let {
                        Text(listOf(it.optString("event_date"), it.optString("start_time"), it.optString("event_type").humanize()).filter(String::isNotBlank).joinToString(" | "), color = Amber, fontWeight = FontWeight.Bold)
                    }
                    venues.map(::supportingJson).firstOrNull { it.optString("grant_id") == grantId }?.let { venue ->
                        Text(listOf(venue.optString("name"), venue.optString("address_line1"), venue.optString("city"), venue.optString("region")).filter(String::isNotBlank).joinToString(", "), color = TextSoft)
                        normalizedMediaLink(venue.optString("maps_url"))?.let { mapUrl -> StudioButton(onClick = { openMediaLink(context, mapUrl) }, kind = StudioButtonKind.Secondary) { Text("Directions", color = Color.White) } }
                    }
                    setList?.let { list ->
                        Text(list.optString("name"), color = Amber, fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 8.dp))
                        sections.map(::supportingJson).filter { it.optString("grant_id") == grantId }.sortedBy { it.optInt("position") }.forEach { section ->
                            Text(section.optString("name", "Set"), color = Cyan, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                            entries.map(::supportingJson).filter { it.optString("grant_id") == grantId && it.optString("section_id") == section.optString("id") }.sortedBy { it.optInt("position") }.forEach { entry ->
                                val song = songs.map(::supportingJson).firstOrNull { it.optString("grant_id") == grantId && it.optString("id") == entry.optString("song_id") }
                                Column(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
                                    Text("${entry.optInt("position")}. ${song?.optString("title")?.ifBlank { entry.optString("manual_title", "Untitled") } ?: entry.optString("manual_title", "Untitled")}", color = Color.White, fontWeight = FontWeight.Bold)
                                    song?.let { value ->
                                        if (value.optString("artist").isNotBlank()) Text(value.optString("artist"), color = TextSoft)
                                        Text(listOf(value.optString("starts_by"), value.optString("style"), value.optString("tempo"), value.optString("time_signature")).filter(String::isNotBlank).joinToString(" | "), color = TextSoft, fontSize = 12.sp)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            normalizedMediaLink(value.optString("media_ref"))?.let { media -> StudioButton(onClick = { openMediaLink(context, media) }, kind = StudioButtonKind.Secondary) { Text("Listen", color = Color.White) } }
                                            attachments.filter { supportingJson(it).optString("grant_id") == grantId && supportingJson(it).optString("song_id") == value.optString("id") }.forEach { attachment ->
                                                val attachmentData = supportingJson(attachment)
                                                val cachedAttachment = cacheById[attachment.entityId]
                                                if (cachedAttachment?.status == "ready") StudioButton(onClick = { preview = cachedAttachment }, kind = StudioButtonKind.Secondary) { Text(attachmentLabel(attachmentData), color = Color.White) }
                                                else normalizedMediaLink(attachmentData.optString("file_ref"))?.let { attachmentUrl -> StudioButton(onClick = { openMediaLink(context, attachmentUrl) }, kind = StudioButtonKind.Secondary) { Text(attachmentLabel(attachmentData), color = Color.White) } }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    preview?.let { AttachmentPreviewDialog(it) { preview = null } }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MySharesPanel(model: StudioRackViewModel) {
    val shares by model.ownedShares.collectAsState()
    val orderedShares = shares.sortedByDescending { supportingJson(it).optString("created_utc") }
    val context = LocalContext.current
    val online = rememberNetworkConnected()
    var editingShare by remember { mutableStateOf<SupportingRecord?>(null) }
    var revokingShare by remember { mutableStateOf<SupportingRecord?>(null) }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Active and previous access you have given others remains visible from the last successful sync.", color = TextSoft)
        if (orderedShares.isEmpty()) EmptyCard("You have not shared anything yet.")
        orderedShares.forEach { record ->
            val share = supportingJson(record)
            val status = share.optString("status", "expired")
            val statusColor = when (status) {
                "active" -> Cyan
                "revoked" -> Color(0xFFFF7C7C)
                else -> TextSoft
            }
            val scopes = share.optJSONArray("scopes")?.let { values ->
                (0 until values.length()).mapNotNull { index -> values.optString(index).takeIf(String::isNotBlank)?.humanize() }
            }.orEmpty()
            val recipient = share.optString("recipient_name").ifBlank {
                share.optString("recipient_email").ifBlank { "Temporary recipient" }
            }
            val delivery = if (share.optString("share_mode") == "registered") "Registered Share" else "Guest Link"
            InfoCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(share.optString("object_name", "Removed object"), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text(recipient, color = Cyan, fontWeight = FontWeight.Bold)
                        if (share.optString("recipient_email").isNotBlank() && share.optString("recipient_email") != recipient) {
                            Text(share.optString("recipient_email"), color = TextSoft, fontSize = 13.sp)
                        }
                    }
                    Text(
                        status.uppercase(), color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Black,
                        modifier = Modifier.background(statusColor.copy(alpha = .12f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
                DetailLine("Delivery", delivery)
                DetailLine("Access", share.optString("access_role", "performer").humanize())
                DetailLine("Includes", scopes.joinToString(", ").ifBlank { "No content selected" })
                DetailLine("Expires", share.optString("expires_utc"))
                DetailLine("Last opened", share.optString("last_accessed_utc").ifBlank { "Not opened yet" })
                if (share.optString("share_mode") == "registered") {
                    DetailLine("Keep a copy", if (share.optInt("allow_copy") == 1) "Allowed" else "Not allowed")
                }
                if (status == "active") FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StudioButton(onClick = { editingShare = record }, enabled = online, kind = StudioButtonKind.Secondary) { Text("Edit", color = Color.White, fontWeight = FontWeight.Bold) }
                    if (share.optString("share_mode") == "guest_link") {
                        StudioButton(onClick = {
                            model.copyShareLink(record.entityId) { link ->
                                if (link != null) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Guest Link", link))
                                    Toast.makeText(context, "Guest link copied.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }, enabled = online, kind = StudioButtonKind.Secondary) { Text("Copy Link", color = Color.White, fontWeight = FontWeight.Bold) }
                        if (share.optString("recipient_email").isNotBlank()) {
                            StudioButton(onClick = { model.emailShare(record.entityId) }, enabled = online, kind = StudioButtonKind.Secondary) { Text("Email Link", color = Color.White, fontWeight = FontWeight.Bold) }
                        }
                    }
                    StudioButton(onClick = { revokingShare = record }, enabled = online, kind = StudioButtonKind.Danger) { Text("Revoke", color = Color.White, fontWeight = FontWeight.Bold) }
                }
                if (!online && status == "active") Text("Connect to edit, copy, email, or revoke this share.", color = Amber, fontSize = 11.sp)
            }
        }
    }
    editingShare?.let { record -> ShareEditor(record, model, online) { editingShare = null } }
    revokingShare?.let { record ->
        val share = supportingJson(record)
        EditorDialog("Revoke Shared Access", { revokingShare = null }) {
            Text("Revoke access to ${share.optString("object_name", "this shared item")}? The recipient will no longer be able to open it.", color = Color.White)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StudioButton(onClick = { revokingShare = null }, modifier = Modifier.weight(1f), kind = StudioButtonKind.Secondary) { Text("Cancel", color = Color.White) }
                StudioButton(onClick = { model.revokeShare(record.entityId) { revokingShare = null } }, enabled = online, modifier = Modifier.weight(1f), kind = StudioButtonKind.Danger) { Text("Revoke", color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun ShareEditor(record: SupportingRecord, model: StudioRackViewModel, online: Boolean, close: () -> Unit) {
    val liveModeName = stringResource(R.string.live_mode_name)
    val original = supportingJson(record)
    var recipientName by remember(record.entityId) { mutableStateOf(original.optString("recipient_name")) }
    var recipientEmail by remember(record.entityId) { mutableStateOf(original.optString("recipient_email")) }
    var role by remember(record.entityId) { mutableStateOf(original.optString("access_role", "performer")) }
    var expires by remember(record.entityId) { mutableStateOf(original.optString("expires_utc")) }
    var allowCopy by remember(record.entityId) { mutableStateOf(original.optInt("allow_copy") == 1) }
    val availableScopes = if (original.optString("object_type") == "set_list") {
        listOf("set_list" to "Set list", "gig_mode" to liveModeName, "attachments" to "Charts and attachments")
    } else {
        listOf("event_summary" to "Event summary", "venue_directions" to "Venue location and directions", "set_list" to "Set list", "gig_mode" to liveModeName, "attachments" to "Charts and attachments")
    }
    val originalScopes = original.optJSONArray("scopes")?.let { values -> (0 until values.length()).map { values.optString(it) }.toSet() }.orEmpty()
    var selectedScopes by remember(record.entityId) { mutableStateOf(originalScopes) }

    EditorDialog("Edit Shared Access", close) {
        Text(original.optString("object_name", "Shared item"), color = Amber, fontSize = 20.sp, fontWeight = FontWeight.Black)
        StudioField("Recipient name", recipientName) { recipientName = it }
        StudioField("Recipient email", recipientEmail, dictation = false) { recipientEmail = it }
        Text("Access role", color = TextSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        ChoiceStrip(listOf("Performer", "Crew", "Guest"), role.humanize()) { role = it.lowercase() }
        StudioField("Expires (UTC, for example 2026-09-08T20:00:00Z)", expires, dictation = false) { expires = it }
        Text("Allowed information", color = TextSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        availableScopes.forEach { (value, label) ->
            Row(Modifier.fillMaxWidth().clickable { selectedScopes = if (value in selectedScopes) selectedScopes - value else selectedScopes + value }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = value in selectedScopes, onCheckedChange = { checked -> selectedScopes = if (checked) selectedScopes + value else selectedScopes - value })
                Text(label, color = Color.White)
            }
        }
        if (original.optString("share_mode") == "registered") {
            Row(Modifier.fillMaxWidth().clickable { allowCopy = !allowCopy }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = allowCopy, onCheckedChange = { allowCopy = it })
                Text("Allow recipient to keep an editable copy", color = Color.White)
            }
        }
        StudioButton(
            onClick = {
                model.updateShare(record.entityId, JSONObject()
                    .put("recipient_name", recipientName.trim()).put("recipient_email", recipientEmail.trim())
                    .put("access_role", role).put("expires_utc", expires.trim())
                    .put("allow_copy", allowCopy).put("scopes", JSONArray(selectedScopes.toList())), close)
            },
            enabled = online && recipientEmail.length <= 320 && selectedScopes.isNotEmpty() && expires.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save Share Changes", color = Ink, fontWeight = FontWeight.Black) }
        Text("Share management requires an internet connection and is applied directly to the server.", color = TextSoft, fontSize = 11.sp)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.directoryContent(model: StudioRackViewModel) {
    item { DirectoryPanel(model) }
}

@Composable
private fun DirectoryPanel(model: StudioRackViewModel) {
    val venues by model.venues.collectAsState()
    val contacts by model.contacts.collectAsState()
    val ensembles by model.ensembles.collectAsState()
    val venueContacts by model.venueContacts.collectAsState()
    val ensembleContacts by model.ensembleContacts.collectAsState()
    var tab by remember { mutableStateOf("Venues") }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<EditorTarget?>(null) }
    var managing by remember { mutableStateOf<Pair<String, CachedRecord>?>(null) }
    val entityType = when (tab) { "Contacts" -> "contact"; "Bands / Groups" -> "ensemble"; else -> "venue" }
    val records = when (entityType) { "contact" -> contacts; "ensemble" -> ensembles; else -> venues }
    val filtered = records.filter {
        val data = recordJson(it)
        listOf(data.optString("name"), data.optString("display_name"), data.optString("organization_name"), data.optString("city"))
            .joinToString(" ").contains(query, ignoreCase = true)
    }.sortedBy { recordJson(it).optString(if (entityType == "contact") "display_name" else "name").lowercase() }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeading("PEOPLE & PLACES", "Directory")
        ChoiceStrip(listOf("Venues", "Contacts", "Bands / Groups"), tab) { tab = it; query = "" }
        StudioButton(onClick = { editing = EditorTarget(null, JSONObject()) }, modifier = Modifier.fillMaxWidth()) {
            Text("Add ${if (entityType == "ensemble") "Band / Group" else entityType.humanize()}", color = Ink, fontWeight = FontWeight.Black)
        }
        StudioField("Find ${tab.lowercase()}", query) { query = it }
        filtered.forEach { record ->
            val data = recordJson(record)
            val title = data.optString(if (entityType == "contact") "display_name" else "name")
            InfoCard {
                Text(title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                val detail = when (entityType) {
                    "venue" -> listOf(data.optString("address_line1"), data.optString("city"), data.optString("region")).filter(String::isNotBlank).joinToString(", ")
                    "contact" -> listOf(data.optString("job_title"), data.optString("organization_name"), data.optString("email")).filter(String::isNotBlank).joinToString(" / ")
                    else -> data.optString("ensemble_type", "band").humanize()
                }
                if (detail.isNotBlank()) Text(detail, color = TextSoft)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StudioButton(onClick = { editing = EditorTarget(record.entityId, data) }, kind = StudioButtonKind.Secondary) { Text("Edit", color = Color.White) }
                    if (entityType != "contact") StudioButton(onClick = { managing = entityType to record }, kind = StudioButtonKind.Secondary) { Text("Contacts", color = Color.White) }
                }
            }
        }
        if (filtered.isEmpty()) Text("No ${tab.lowercase()} match this search.", color = TextSoft)
    }
    editing?.let { target -> DirectoryEditor(entityType, target, model) { editing = null } }
    managing?.let { (parentType, record) ->
        DirectoryRelationshipsDialog(
            parentType, record, contacts,
            if (parentType == "venue") venueContacts else ensembleContacts,
            model,
        ) { managing = null }
    }
}

@Composable
private fun DirectoryEditor(entityType: String, target: EditorTarget, model: StudioRackViewModel, close: () -> Unit) {
    val original = target.data
    var name by remember { mutableStateOf(original.optString(if (entityType == "contact") "display_name" else "name")) }
    var type by remember { mutableStateOf(original.optString("ensemble_type", "band")) }
    var organization by remember { mutableStateOf(original.optString("organization_name")) }
    var title by remember { mutableStateOf(original.optString("job_title")) }
    var email by remember { mutableStateOf(original.optString("email")) }
    var phone by remember { mutableStateOf(original.optString("phone")) }
    var address by remember { mutableStateOf(original.optString("address_line1")) }
    var city by remember { mutableStateOf(original.optString("city")) }
    var region by remember { mutableStateOf(original.optString("region")) }
    var postalCode by remember { mutableStateOf(original.optString("postal_code")) }
    var website by remember { mutableStateOf(original.optString("website")) }
    var mapsUrl by remember { mutableStateOf(original.optString("maps_url")) }
    var loadIn by remember { mutableStateOf(original.optString("load_in_notes")) }
    var parking by remember { mutableStateOf(original.optString("parking_notes")) }
    var notes by remember { mutableStateOf(original.optString("notes")) }
    val label = when (entityType) { "venue" -> "Venue"; "contact" -> "Contact"; else -> "Band / Group" }
    EditorDialog(if (target.id == null) "Add $label" else "Edit $label", close) {
        StudioField("Name", name) { name = it }
        when (entityType) {
            "venue" -> {
                StudioField("Address", address) { address = it }; StudioField("City", city) { city = it }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) { StudioField("State / Region", region) { region = it } }
                    Box(Modifier.weight(1f)) { StudioField("Postal Code", postalCode) { postalCode = it } }
                }
                StudioField("Phone", phone) { phone = it }; StudioField("Email", email) { email = it }
                StudioField("Website", website) { website = it }; StudioField("Google Maps Link", mapsUrl) { mapsUrl = it }
                StudioField("Load-in Notes", loadIn, singleLine = false) { loadIn = it }
                StudioField("Parking Notes", parking, singleLine = false) { parking = it }
            }
            "contact" -> {
                StudioField("Organization", organization) { organization = it }; StudioField("Title / Role", title) { title = it }
                StudioField("Email", email) { email = it }; StudioField("Phone", phone) { phone = it }
            }
            else -> {
                Text("Type", color = TextSoft, fontWeight = FontWeight.Bold)
                ChoiceStrip(listOf("band", "worship_group", "studio", "production_company", "other"), type) { type = it }
                StudioField("Website", website) { website = it }
            }
        }
        StudioField("Private Notes", notes, singleLine = false) { notes = it }
        EditorActions(name.isNotBlank(), save = {
            val data = JSONObject().put(if (entityType == "contact") "display_name" else "name", name.trim()).put("notes", notes.trim())
            when (entityType) {
                "venue" -> data.put("address_line1", address.trim()).put("city", city.trim()).put("region", region.trim()).put("postal_code", postalCode.trim()).put("phone", phone.trim()).put("email", email.trim()).put("website", website.trim()).put("maps_url", mapsUrl.trim()).put("load_in_notes", loadIn.trim()).put("parking_notes", parking.trim())
                "contact" -> data.put("organization_name", organization.trim()).put("job_title", title.trim()).put("email", email.trim()).put("phone", phone.trim())
                else -> data.put("ensemble_type", type).put("website", website.trim())
            }
            model.saveDirectoryRecord(entityType, target.id, data, close)
        }, delete = target.id?.let { id -> { model.deleteDirectoryRecord(entityType, id, close) } })
    }
}

@Composable
private fun DirectoryRelationshipsDialog(parentType: String, parent: CachedRecord, contacts: List<CachedRecord>, relationships: List<CachedRecord>, model: StudioRackViewModel, close: () -> Unit) {
    val parentKey = if (parentType == "venue") "venue_id" else "ensemble_id"
    val selectedAtOpen = relationships.filter { recordJson(it).optString(parentKey) == parent.entityId }.mapTo(mutableSetOf()) { recordJson(it).optString("contact_id") }
    var selected by remember { mutableStateOf(selectedAtOpen) }
    EditorDialog("Contacts for ${recordJson(parent).optString("name")}", close) {
        if (contacts.isEmpty()) Text("Add contacts in the Contact tab first.", color = TextSoft)
        contacts.sortedBy { recordJson(it).optString("display_name") }.forEach { contact ->
            val checked = contact.entityId in selected
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked, { enabled -> selected = selected.toMutableSet().apply { if (enabled) add(contact.entityId) else remove(contact.entityId) } })
                Text(recordJson(contact).optString("display_name"), color = Color.White)
            }
        }
        EditorActions(true, save = { model.saveDirectoryRelationships(parentType, parent.entityId, selected, close) }, delete = null)
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
    val maintenanceNotes by model.maintenanceNotes.collectAsState()
    val reportState by model.reportState.collectAsState()
    val online = rememberNetworkConnected()
    val itemRows = items.map(::supportingJson)
    val specRows = specs.map(::supportingJson)
    val maintenanceHistory by model.maintenanceHistory.collectAsState()
    val careRows = maintenanceRows(specRows, itemRows, brands.map(::supportingJson), locations.map(::supportingJson), windowDays = 36500, fieldNotes = maintenanceNotes.map(::recordJson), completions = maintenanceHistory.filter { it.revision == 0 }.map(::recordJson))
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
        ChoiceStrip(listOf("Overview", "Equipment", "Maintenance", "Schedule", "AI", "Import"), tab) { tab = it }
        when (tab) {
            "Overview" -> ReportOverviewTab(itemRows, kits, events.map(::recordJson), units, specRows, categories, statuses, locations, careRows, reportState, online, model)
            "Equipment" -> EquipmentReportTab(itemRows, specRows, categories, types, statuses, locations)
            "Maintenance" -> MaintenanceReport(careRows, model)
            "Schedule" -> ScheduleReportTab(events.map(::recordJson))
            "AI" -> AiReportTab(question, { question = it }, online, reportState, model, runs)
            else -> ImportDataTab(online, reportState, model)
        }
    }
}

@Composable
private fun ImportDataTab(online: Boolean, state: ReportUiState, model: StudioRackViewModel) {
    val productName = stringResource(R.string.app_name)
    val context = LocalContext.current
    var kind by remember { mutableStateOf("Songs") }
    val kindValue = mapOf("Songs" to "songs", "Set Lists" to "setlists", "Items" to "items", "Kits" to "kits")
    val chooseImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val displayName = contentDisplayName(context, uri)
            val safeName = displayName.replace(Regex("[^A-Za-z0-9._-]"), "-").ifBlank { "import-file" }
            val temporary = File(context.cacheDir, "exchange-${System.currentTimeMillis()}-$safeName")
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input -> temporary.outputStream().use(input::copyTo) }
                    ?: error("The selected file could not be opened.")
            }.onSuccess {
                model.importData(kindValue.getValue(kind), temporary, displayName, context.contentResolver.getType(uri).orEmpty()) {
                    temporary.delete()
                }
            }.onFailure {
                temporary.delete()
                Toast.makeText(context, it.message ?: "The selected file could not be opened.", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Import Data", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Bring songs, complete set lists, items, and kits in from spreadsheet or structured data files. Exports live beside each filtered list.", color = TextSoft)
        ChoiceStrip(kindValue.keys.toList(), kind) { kind = it }
        StudioButton(
            onClick = { chooseImport.launch(arrayOf("text/csv", "application/json", "application/xml", "text/xml", "application/vnd.ms-excel", "*/*")) },
            enabled = online && !state.busy,
            modifier = Modifier.fillMaxWidth(),
            kind = StudioButtonKind.Secondary,
        ) { Text("Import $kind", color = Color.White, fontWeight = FontWeight.Bold) }
        if (!online) Text("Connect to $productName to import. Your synchronized working data remains available offline.", color = Amber, fontSize = 12.sp)
        if (state.message.isNotBlank()) Text(state.message, color = if (state.message.startsWith("Import complete")) Cyan else TextSoft, fontSize = 12.sp)
        Text("CSV, XLS, JSON, and XML files can be merged into your synchronized $productName account.", color = TextSoft, fontSize = 11.sp)
    }
}

private data class ExportTarget(val kind: String, val label: String, val ids: List<String>)

@Composable
private fun ContextExportDialog(
    target: ExportTarget,
    online: Boolean,
    state: ReportUiState,
    model: StudioRackViewModel,
    close: () -> Unit,
) {
    val context = LocalContext.current
    var format by remember(target) { mutableStateOf("CSV") }
    var pendingExport by remember(target) { mutableStateOf<DataExport?>(null) }
    val saveExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        val export = pendingExport
        if (uri != null && export != null) {
            runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(export.bytes) } }
                .onSuccess {
                    Toast.makeText(context, "${export.filename} saved.", Toast.LENGTH_SHORT).show()
                    close()
                }
                .onFailure { Toast.makeText(context, "The export could not be saved.", Toast.LENGTH_LONG).show() }
        }
        pendingExport = null
    }
    Dialog(onDismissRequest = close) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = PanelRaised,
            border = BorderStroke(1.dp, Amber.copy(alpha = .45f)),
            shape = RoundedCornerShape(8.dp),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("EXPORT", color = Amber, fontSize = 11.sp, fontWeight = FontWeight.Black)
                Text(target.label, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("${target.ids.size} selected ${if (target.ids.size == 1) "record" else "records"}", color = TextSoft)
                ChoiceStrip(listOf("CSV", "XLS", "JSON", "XML"), format) { format = it }
                StudioButton(
                    onClick = {
                        model.exportData(target.kind, format.lowercase(), target.ids) { export ->
                            if (export != null) {
                                pendingExport = export
                                saveExport.launch(export.filename)
                            }
                        }
                    },
                    enabled = target.ids.isNotEmpty() && !state.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (state.busy) "Preparing" else "Export as $format", color = Ink, fontWeight = FontWeight.Black) }
                Text(
                    if (online) "Exporting the synchronized data stored on this device."
                    else "Offline export ready. Pending device changes are included.",
                    color = if (online) TextSoft else Cyan,
                    fontSize = 12.sp,
                )
                if (target.kind == "setlists") Text("Set and song details are included. Chart and other attachment files are not included.", color = TextSoft, fontSize = 11.sp)
                TextButton(onClick = close, modifier = Modifier.align(Alignment.End)) { Text("Cancel", color = Cyan) }
            }
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
        DictationTextField(query, { query = it }, "Filter equipment report")
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
private fun MaintenanceReport(rows: List<MaintenanceRow>, model: StudioRackViewModel) {
    var status by remember { mutableStateOf("All") }
    val filtered = rows.filter { status == "All" || it.statusLabel == status }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Maintenance Report", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        ChoiceStrip(listOf("All", "Needs attention", "Past due", "Due today", "Upcoming", "Scheduled"), status) { status = it }
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
                MaintenanceHistoryControl(row.itemId, row.name, model)
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
        DictationTextField(query, { query = it }, "Filter schedule report")
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
    val agentName = stringResource(R.string.agent_name)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Ask $agentName", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Describe the equipment report you need in ordinary language.", color = TextSoft)
        DictationTextField(question, changeQuestion, "Report question", enabled = online && !state.busy)
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
            StudioButton(onClick = model::sync, enabled = !uiState.busy, modifier = Modifier.fillMaxWidth()) { Text(if (uiState.busy) "Synchronizing" else "Synchronize", color = Ink, fontWeight = FontWeight.Black) }
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
        item { DictationTextField(query, onQuery, placeholder) }
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
@OptIn(ExperimentalLayoutApi::class)
private fun ChoiceStrip(options: List<String>, selected: String, choose: (String) -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
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
    val agentName = stringResource(R.string.agent_name)
    ExpandableRecordCard(row.optString("subject", "$agentName action"), row.optString("updated_utc"), listOf(row.optString("priority").humanize(), row.optString("status").humanize())) {
        DetailLine("Recipient", row.optString("recipient")); DetailLine("Due", row.optString("source_due_date")); DetailLine("Draft", row.optString("body")); DetailLine("Last reply", row.optString("last_reply_body"))
    }
}

@Composable
private fun CareSummary(rows: List<MaintenanceRow>, model: StudioRackViewModel) {
    val attention = rows.filter { it.status in setOf("attention", "overdue", "due", "soon") }
    val counts = listOf(
        "Attention" to rows.count { it.status == "attention" },
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
                    MaintenanceHistoryControl(row.itemId, row.name, model)
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
    fieldNotes: List<JSONObject> = emptyList(),
    completions: List<JSONObject> = emptyList(),
): List<MaintenanceRow> {
    val specsByItem = specs.groupBy { it.optString("item_id") }.mapValues { (_, rows) ->
        rows.associate { it.optString("key") to it.optString("value") }
    }
    val brandNames = brands.associate { it.optString("id") to it.optString("name") }
    val locationNames = locations.associate { it.optString("id") to it.optString("name") }
    val fieldNotesByItem = fieldNotes
        .filter { it.optString("id") !in resolvedNoteIds(completions) }
        .filter { it.optString("status", "pending") in setOf("pending", "notified") }
        .groupBy { it.optString("item_id") }
    return items.mapNotNull { item ->
        val itemId = item.optString("id")
        val values = projectedMaintenanceSpecs(specsByItem[itemId].orEmpty(), completions.filter { it.optString("item_id") == itemId })
        val explicitDue = parseLocalDate(values["next_service_due"])
        val intervalDue = parseLocalDate(values["last_service_date"])?.let { last ->
            values["service_interval_days"]?.toLongOrNull()?.takeIf { it > 0 }?.let(last::plusDays)
        }
        val itemFieldNotes = fieldNotesByItem[itemId].orEmpty()
        val due = explicitDue ?: intervalDue ?: if (itemFieldNotes.isNotEmpty()) today else return@mapNotNull null
        val days = ChronoUnit.DAYS.between(today, due)
        val (status, label) = when {
            itemFieldNotes.isNotEmpty() -> "attention" to "Needs attention"
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
            careItem = if (itemFieldNotes.isNotEmpty()) "Field note" else values["consumables_tracked"].orEmpty().ifBlank { "Service" },
            location = locationNames[item.optString("default_location_id")].orEmpty().ifBlank { "No location" },
            careStatus = if (itemFieldNotes.isNotEmpty()) "Attention" else values["care_status"].orEmpty().humanize().ifBlank { "Not set" },
            notes = (itemFieldNotes.joinToString("\n") { it.optString("note") }).ifBlank { values["bot_notes"].orEmpty().ifBlank { summary } },
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

private enum class ConnectionKind { ONLINE, OFFLINE, WARNING, ERROR }
private data class ConnectionBanner(val label: String, val kind: ConnectionKind)

private fun connectionBanner(online: Boolean, syncing: Boolean, syncError: Boolean, pending: Int): ConnectionBanner = when {
    syncing && online -> ConnectionBanner("ONLINE - SYNCING", ConnectionKind.ONLINE)
    syncing -> ConnectionBanner("OFFLINE - SYNC PAUSED", ConnectionKind.WARNING)
    online && syncError -> ConnectionBanner("ONLINE - SYNC ERROR", ConnectionKind.ERROR)
    online && pending > 0 -> ConnectionBanner("ONLINE - $pending CHANGE${if (pending == 1) "" else "S"} QUEUED", ConnectionKind.WARNING)
    online -> ConnectionBanner("ONLINE - SYNCED", ConnectionKind.ONLINE)
    pending > 0 -> ConnectionBanner("OFFLINE - $pending CHANGE${if (pending == 1) "" else "S"} QUEUED", ConnectionKind.WARNING)
    else -> ConnectionBanner("OFFLINE - SYNCED", ConnectionKind.OFFLINE)
}

@Composable
private fun rememberNetworkConnected(): Boolean {
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }
    var connected by remember { mutableStateOf(deviceHasInternet(context)) }
    DisposableEffect(manager) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { connected = deviceHasInternet(context) }
            override fun onLost(network: Network) { connected = deviceHasInternet(context) }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                connected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }
        }
        manager.registerDefaultNetworkCallback(callback)
        onDispose { runCatching { manager.unregisterNetworkCallback(callback) } }
    }
    return connected
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
).filter(String::isNotBlank).joinToString(" | ").ifBlank { "Offline workspace" }

private data class EditorTarget(val id: String?, val data: JSONObject)

@Composable
private fun SongEditor(target: EditorTarget, model: StudioRackViewModel, close: () -> Unit) {
    val context = LocalContext.current
    val original = target.data
    val allAttachments by model.attachments.collectAsState()
    val cachedAttachments by model.cachedAttachments.collectAsState()
    val existingAttachments = allAttachments.filter { recordJson(it).optString("song_id") == target.id }
    val cacheById = cachedAttachments.associateBy(CachedAttachment::attachmentId)
    var title by remember { mutableStateOf(original.optString("title")) }
    var artist by remember { mutableStateOf(original.optString("artist")) }
    var style by remember { mutableStateOf(original.optString("style")) }
    var tempo by remember { mutableStateOf(original.optString("tempo")) }
    var duration by remember { mutableStateOf(formatDuration(original.optInt("duration_seconds"))) }
    var signature by remember { mutableStateOf(original.optString("time_signature", "4/4")) }
    var starts by remember { mutableStateOf(original.optString("starts_by")) }
    var patchName by remember { mutableStateOf(original.optString("patch_name")) }
    var patchNumber by remember { mutableStateOf(original.optString("patch_number")) }
    var media by remember { mutableStateOf(original.optString("media_ref")) }
    var notes by remember { mutableStateOf(original.optString("notes")) }
    var favorite by remember { mutableStateOf(original.optInt("is_favorite") == 1) }
    var attachmentType by remember { mutableStateOf("Chart") }
    var newAttachments by remember(target.id) { mutableStateOf(emptyList<SongAttachmentInput>()) }
    var preview by remember { mutableStateOf<CachedAttachment?>(null) }
    val attachmentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val displayName = contentDisplayName(context, uri)
            newAttachments = newAttachments + SongAttachmentInput(
                uri = uri.toString(),
                displayName = displayName,
                attachmentType = attachmentType.lowercase().replace(' ', '_'),
                mimeType = context.contentResolver.getType(uri).orEmpty(),
            )
        }
    }
    EditorDialog(if (target.id == null) "Add Song" else "Edit Song", close) {
        StudioField("Song title", title) { title = it }
        StudioField("Artist", artist) { artist = it }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth < 520.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StudioField("Style", style) { style = it }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.weight(1f)) { StudioField("Tempo", tempo, dictation = false) { tempo = it.filter(Char::isDigit).take(3) } }
                        Box(Modifier.weight(1f)) { StudioField("Time signature", signature, dictation = false) { signature = it.take(12) } }
                    }
                    StudioField("Song length", duration, dictation = false) { duration = it.filter { char -> char.isDigit() || char == ':' }.take(8) }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) { StudioField("Style", style) { style = it } }
                    Box(Modifier.weight(1f)) { StudioField("Tempo", tempo, dictation = false) { tempo = it.filter(Char::isDigit).take(3) } }
                    Box(Modifier.weight(1f)) { StudioField("Time signature", signature, dictation = false) { signature = it.take(12) } }
                    Box(Modifier.weight(1f)) { StudioField("Song length", duration, dictation = false) { duration = it.filter { char -> char.isDigit() || char == ':' }.take(8) } }
                }
            }
        }
        StudioField("Who starts", starts) { starts = it }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) { StudioField("Patch name", patchName) { patchName = it } }
            Box(Modifier.weight(1f)) { StudioField("Patch number", patchNumber) { patchNumber = it } }
        }
        StudioField("Listen / media URL", media, dictation = false) { media = it }
        StudioField("Notes", notes, singleLine = false) { notes = it }
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(favorite, { favorite = it }); Text("Favorite", color = Color.White) }
        Text("Attachments", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Text("Add charts, lyrics, tablature, or sheet music now. Files are copied to this device immediately and uploaded on the next sync.", color = TextSoft, fontSize = 11.sp)
        ChoiceStrip(listOf("Chart", "Lyrics", "Tab", "Sheet Music", "Other"), attachmentType) { attachmentType = it }
        StudioButton(
            onClick = { attachmentPicker.launch(arrayOf("application/pdf", "image/*", "text/plain", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) },
            modifier = Modifier.fillMaxWidth(),
            kind = StudioButtonKind.Secondary,
        ) { Text("Add $attachmentType File", color = Color.White, fontWeight = FontWeight.Bold) }
        existingAttachments.forEach { record ->
            val attachment = recordJson(record)
            val cached = cacheById[record.entityId]
            AttachmentEditorRow(
                label = attachmentLabel(attachment),
                detail = if (cached?.status == "ready") "Available offline" else "Will download when connected",
                view = cached?.takeIf { it.status == "ready" && !it.localPath.isNullOrBlank() }?.let { { preview = it } },
            )
        }
        newAttachments.forEachIndexed { index, attachment ->
            AttachmentEditorRow(
                label = attachment.displayName,
                detail = "${attachment.attachmentType.humanize()} - queued locally",
                remove = { newAttachments = newAttachments.filterIndexed { itemIndex, _ -> itemIndex != index } },
            )
        }
        EditorActions(
            canSave = title.isNotBlank(),
            save = {
                model.saveSong(target.id, JSONObject()
                    .put("title", title.trim()).put("artist", artist.trim()).put("style", style.trim())
                    .put("tempo", tempo.trim()).put("duration_seconds", parseDuration(duration)).put("time_signature", signature.trim()).put("starts_by", starts.trim())
                    .put("patch_name", patchName.trim()).put("patch_number", patchNumber.trim())
                    .put("media_ref", media.trim()).put("notes", notes.trim()).put("is_favorite", if (favorite) 1 else 0), newAttachments, close)
            },
            delete = target.id?.let { id -> { model.deleteSong(id, close) } },
        )
    }
    preview?.let { AttachmentPreviewDialog(it) { preview = null } }
}

@Composable
private fun AttachmentEditorRow(label: String, detail: String, view: (() -> Unit)? = null, remove: (() -> Unit)? = null) {
    Surface(color = Panel, border = BorderStroke(1.dp, Color(0x33FFFFFF)), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, color = Color.White, fontWeight = FontWeight.Bold)
                Text(detail, color = TextSoft, fontSize = 11.sp)
            }
            view?.let { TextButton(onClick = it) { Text("View", color = Cyan, fontWeight = FontWeight.Bold) } }
            remove?.let { TextButton(onClick = it) { Text("Remove", color = Amber, fontWeight = FontWeight.Bold) } }
        }
    }
}

@Composable
private fun AttachmentPreviewDialog(attachment: CachedAttachment, close: () -> Unit) {
    val path = attachment.localPath.orEmpty()
    val isPdf = attachment.mimeType == "application/pdf" || path.endsWith(".pdf", true)
    val pageCount = remember(path) { if (isPdf) pdfPageCount(path) else 1 }
    var page by remember(path) { mutableIntStateOf(0) }
    val bitmap by produceState<Bitmap?>(initialValue = null, path, page) {
        value = withContext(Dispatchers.IO) { if (isPdf) renderPdfPage(path, page) else decodeAttachmentImage(path) }
    }
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Ink) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(attachment.displayName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = close) { Text("Close", color = Cyan) }
                }
                if (pageCount > 1) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { page = (page - 1).coerceAtLeast(0) }, enabled = page > 0) { Text("Previous", color = if (page > 0) Amber else TextSoft) }
                    Text("${page + 1} / $pageCount", color = TextSoft, modifier = Modifier.padding(12.dp))
                    TextButton(onClick = { page = (page + 1).coerceAtMost(pageCount - 1) }, enabled = page < pageCount - 1) { Text("Next", color = if (page < pageCount - 1) Amber else TextSoft) }
                }
                val rendered = bitmap
                if (rendered == null) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Cyan) }
                else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Image(rendered.asImageBitmap(), attachment.displayName, Modifier.fillMaxWidth().aspectRatio(rendered.width.toFloat() / rendered.height.toFloat()), contentScale = ContentScale.FillWidth)
                }
            }
        }
    }
}

private fun contentDisplayName(context: Context, uri: Uri): String {
    val fromProvider = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
    return fromProvider?.takeIf(String::isNotBlank) ?: uri.lastPathSegment?.substringAfterLast('/') ?: "attachment.pdf"
}

@Composable
private fun EventEditor(target: EditorTarget, model: StudioRackViewModel, close: () -> Unit) {
    val agentName = stringResource(R.string.agent_name)
    val original = target.data
    val setLists by model.setLists.collectAsState()
    val venues by model.venues.collectAsState()
    val contacts by model.contacts.collectAsState()
    val ensembles by model.ensembles.collectAsState()
    val eventContacts by model.eventContacts.collectAsState()
    val eventEnsembles by model.eventEnsembles.collectAsState()
    var title by remember { mutableStateOf(original.optString("title")) }
    var type by remember { mutableStateOf(original.optString("event_type", "performance")) }
    var status by remember { mutableStateOf(original.optString("event_status", "scheduled")) }
    var date by remember { mutableStateOf(original.optString("event_date")) }
    var time by remember { mutableStateOf(original.optString("start_time")) }
    var endDate by remember { mutableStateOf(original.optString("end_date")) }
    var endTime by remember { mutableStateOf(original.optString("end_time")) }
    var venueId by remember { mutableStateOf(original.optString("venue_id")) }
    var location by remember { mutableStateOf(original.optString("location")) }
    var setListId by remember { mutableStateOf(original.optString("set_list_id")) }
    var notes by remember { mutableStateOf(original.optString("notes")) }
    var reminder by remember { mutableStateOf(original.optInt("reminder_enabled", 1) == 1) }
    var lead by remember { mutableStateOf(original.optString("reminder_lead_value", "2")) }
    var unit by remember { mutableStateOf(original.optString("reminder_lead_unit", "days")) }
    var selectedEnsembles by remember(target.id, eventEnsembles) { mutableStateOf(eventEnsembles.filter { recordJson(it).optString("event_id") == target.id }.mapTo(mutableSetOf()) { recordJson(it).optString("ensemble_id") }) }
    var selectedContacts by remember(target.id, eventContacts) { mutableStateOf(eventContacts.filter { recordJson(it).optString("event_id") == target.id }.mapTo(mutableSetOf()) { recordJson(it).optString("contact_id") }) }
    EditorDialog(if (target.id == null) "Add Scheduled Event" else "Edit Scheduled Event", close) {
        StudioField("Name", title) { title = it }
        Text("Type", color = TextSoft, fontWeight = FontWeight.Bold); ChoiceStrip(listOf("performance", "rehearsal", "studio_session", "other"), type) { type = it }
        Text("Status", color = TextSoft, fontWeight = FontWeight.Bold); ChoiceStrip(listOf("scheduled", "ended"), status) { status = it }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) { StudioField("Date (YYYY-MM-DD)", date, dictation = false) { date = it.take(10) } }
            Box(Modifier.weight(1f)) { StudioField("Start Time", time, dictation = false) { time = it.take(8) } }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) { StudioField("End Date", endDate, dictation = false) { endDate = it.take(10) } }
            Box(Modifier.weight(1f)) { StudioField("End Time", endTime, dictation = false) { endTime = it.take(8) } }
        }
        Text("Venue", color = TextSoft, fontWeight = FontWeight.Bold)
        ChoiceStrip(listOf("None") + venues.map { recordJson(it).optString("name") }, venues.firstOrNull { it.entityId == venueId }?.let { recordJson(it).optString("name") } ?: "None") { picked ->
            venueId = venues.firstOrNull { recordJson(it).optString("name") == picked }?.entityId.orEmpty()
        }
        StudioField("Room / Stage / Location Details", location) { location = it }
        Text("Set list", color = TextSoft, fontWeight = FontWeight.Bold)
        ChoiceStrip(listOf("None") + setLists.map { recordJson(it).optString("name") }, setLists.firstOrNull { it.entityId == setListId }?.let { recordJson(it).optString("name") } ?: "None") { picked ->
            setListId = setLists.firstOrNull { recordJson(it).optString("name") == picked }?.entityId.orEmpty()
        }
        StudioField("Notes", notes, singleLine = false) { notes = it }
        if (ensembles.isNotEmpty()) {
            Text("Bands / Groups", color = TextSoft, fontWeight = FontWeight.Bold)
            ensembles.forEach { ensemble ->
                val checked = ensemble.entityId in selectedEnsembles
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked, { enabled -> selectedEnsembles = selectedEnsembles.toMutableSet().apply { if (enabled) add(ensemble.entityId) else remove(ensemble.entityId) } })
                    Text(recordJson(ensemble).optString("name"), color = Color.White)
                }
            }
        }
        if (contacts.isNotEmpty()) {
            Text("People / Contacts", color = TextSoft, fontWeight = FontWeight.Bold)
            contacts.forEach { contact ->
                val checked = contact.entityId in selectedContacts
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked, { enabled -> selectedContacts = selectedContacts.toMutableSet().apply { if (enabled) add(contact.entityId) else remove(contact.entityId) } })
                    Text(recordJson(contact).optString("display_name"), color = Color.White)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(reminder, { reminder = it }); Text("$agentName reminder", color = Color.White) }
        if (reminder) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) { StudioField("How close", lead, dictation = false) { lead = it.filter(Char::isDigit).take(3) } }
            Box(Modifier.weight(1f)) { Text("Unit", color = TextSoft); ChoiceStrip(listOf("hours", "days", "weeks"), unit) { unit = it } }
        }
        EditorActions(
            canSave = title.isNotBlank(),
            save = {
                model.saveEvent(target.id, JSONObject()
                    .put("event_type", type).put("event_status", status).put("title", title.trim())
                    .put("event_date", date.trim()).put("start_time", time.trim()).put("end_date", endDate.trim()).put("end_time", endTime.trim())
                    .put("venue_id", venueId.ifBlank { JSONObject.NULL }).put("location", location.trim())
                    .put("set_list_id", setListId.ifBlank { JSONObject.NULL }).put("notes", notes.trim())
                    .put("reminder_enabled", if (reminder) 1 else 0).put("reminder_lead_value", lead.toIntOrNull() ?: 2)
                    .put("reminder_lead_unit", unit), selectedEnsembles, selectedContacts, close)
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
private fun StudioField(
    label: String,
    value: String,
    singleLine: Boolean = true,
    dictation: Boolean = true,
    update: (String) -> Unit,
) {
    if (dictation) {
        DictationTextField(value, update, label, singleLine = singleLine)
    } else {
        OutlinedTextField(value, update, label = { Text(label) }, singleLine = singleLine, minLines = if (singleLine) 1 else 3, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun EditorActions(canSave: Boolean, save: () -> Unit, delete: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StudioButton(onClick = save, enabled = canSave, modifier = Modifier.weight(1f)) { Text("Save", color = Ink, fontWeight = FontWeight.Black) }
        if (delete != null) StudioButton(onClick = delete, kind = StudioButtonKind.Danger) { Text("Delete", color = Color.White, fontWeight = FontWeight.Bold) }
    }
    Text("This change is stored on this device immediately and synchronized when a connection is available.", color = TextSoft, fontSize = 11.sp)
}

@Composable
private fun EventCard(event: JSONObject, readiness: PacketReadiness, open: () -> Unit, edit: (() -> Unit)? = null) {
    val liveModeName = stringResource(R.string.live_mode_name)
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
                if (event.optString("set_list_id").isNotBlank()) Text("Open $liveModeName", color = Amber, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
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
    val venues by model.venues.collectAsState()
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
    val gigStartedAt = remember(eventId) { System.currentTimeMillis() }
    var clockTick by remember(eventId) { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(eventId) {
        while (true) {
            clockTick = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val activeGigSong = performanceSongs.getOrNull(currentSong)
    val setRemainingSeconds = performanceSongs.drop(currentSong)
        .takeWhile { it.sectionName == activeGigSong?.sectionName }
        .sumOf { it.song?.optInt("duration_seconds") ?: 0 }
    val elapsedSeconds = ((clockTick - gigStartedAt) / 1_000).coerceAtLeast(0)

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
            settings = settings,
            elapsedSeconds = elapsedSeconds,
            setRemainingSeconds = setRemainingSeconds,
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
                        val venueName = venues.firstOrNull { it.entityId == event.optString("venue_id") }?.let { recordJson(it).optString("name") }.orEmpty()
                        Text(listOf(venueName, event.optString("location")).filter(String::isNotBlank).joinToString(" - "), color = TextSoft, fontSize = 11.sp, maxLines = 1)
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
        if (settings.showClock || settings.showElapsed || settings.showSetRemaining) {
            item { GigTimeStrip(settings, elapsedSeconds, setRemainingSeconds, activeGigSong?.sectionName.orEmpty()) }
        }
        sectionRows.forEach { section ->
            item {
                val sectionSeconds = performanceSongs.filter { it.sectionName == section.optString("name", "Set") }.sumOf { it.song?.optInt("duration_seconds") ?: 0 }
                Column(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(section.optString("name", "Set"), color = Amber, fontFamily = FontFamily.Serif, fontSize = 34.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    if (sectionSeconds > 0) Text("${formatDuration(sectionSeconds)} estimated music time", color = TextSoft, fontSize = 11.sp)
                }
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
        listOf(song?.optString("tempo"), song?.optString("time_signature"), formatDuration(song?.optInt("duration_seconds") ?: 0)).filterNotNull().filter(String::isNotBlank).forEach { GigValueChip(it) }
    }
}

@Composable
private fun GigTimeStrip(settings: PerformanceSettings, elapsedSeconds: Long, remainingSeconds: Int, sectionName: String) {
    Surface(
        color = Color(0xC9161B26),
        shape = RoundedCornerShape(7.dp),
        border = BorderStroke(1.dp, Amber.copy(alpha = 0.22f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (settings.showClock) GigTimerCell("Clock", LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm:ss a")))
            if (settings.showElapsed) GigTimerCell("Elapsed", formatDuration(elapsedSeconds.toInt(), showZero = true))
            if (settings.showSetRemaining) GigTimerCell("${sectionName.ifBlank { "Set" }} left", formatDuration(remainingSeconds, showZero = true))
        }
    }
}

@Composable
private fun GigTimerCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 5.dp)) {
        Text(label.uppercase(), color = TextSoft, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
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
    settings: PerformanceSettings,
    elapsedSeconds: Long,
    setRemainingSeconds: Int,
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
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Color(0xFF120D08), Ink, Color(0xFF07131B))))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
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
        if (settings.showClock || settings.showElapsed || settings.showSetRemaining) {
            GigTimeStrip(settings, elapsedSeconds, setRemainingSeconds, item.sectionName)
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
            GigDetail("Length", formatDuration(item.song?.optInt("duration_seconds") ?: 0))
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
        val renderedBitmap = rendered.bitmap
        val chartModifier = if (renderedBitmap != null && renderedBitmap.height > 0) {
            Modifier.fillMaxWidth().aspectRatio(renderedBitmap.width.toFloat() / renderedBitmap.height.toFloat())
        } else {
            Modifier.fillMaxWidth().heightIn(min = 320.dp)
        }
        Box(chartModifier.padding(top = 10.dp), contentAlignment = Alignment.TopCenter) {
            when {
                !rendered.complete -> CircularProgressIndicator()
                renderedBitmap == null -> SongDetailFallback(item)
                else -> Image(
                    renderedBitmap.asImageBitmap(),
                    contentDescription = item.attachment?.let(::attachmentLabel),
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth,
                )
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

private fun parseDuration(value: String): Int {
    val parts = value.trim().split(':').mapNotNull(String::toIntOrNull)
    return when (parts.size) {
        1 -> parts[0] * 60
        2 -> parts[0] * 60 + parts[1].coerceIn(0, 59)
        3 -> parts[0] * 3600 + parts[1].coerceIn(0, 59) * 60 + parts[2].coerceIn(0, 59)
        else -> 0
    }.coerceIn(0, 86_400)
}

internal fun formatDuration(seconds: Int, showZero: Boolean = false): String {
    if (seconds <= 0) return if (showZero) "0:00" else ""
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainder = seconds % 60
    return if (hours > 0) "$hours:${minutes.toString().padStart(2, '0')}:${remainder.toString().padStart(2, '0')}"
    else "$minutes:${remainder.toString().padStart(2, '0')}"
}

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
        Toast.makeText(context, "${context.getString(R.string.app_name)} could not open this media link.", Toast.LENGTH_LONG).show()
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

private fun recordJson(record: CachedRecord): JSONObject = runCatching { JSONObject(record.json) }.getOrDefault(JSONObject()).put("id", record.entityId)

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
