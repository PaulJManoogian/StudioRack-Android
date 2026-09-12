package com.manoogianmedia.studiorack.ui

import android.Manifest
import android.app.Activity
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
import android.os.Process
import android.os.Build
import android.content.pm.PackageManager
import android.provider.OpenableColumns
import android.provider.ContactsContract
import android.util.LruCache
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.Shapes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.List as ListIcon
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NavigateBefore
import androidx.compose.material.icons.rounded.NavigateNext
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.darkColorScheme
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
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
import com.manoogianmedia.studiorack.data.LocalLivePeer
import com.manoogianmedia.studiorack.data.LocalLiveRole
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
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import java.text.DateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
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
    val productName = stringResource(R.string.app_name_marked)
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
    val productName = stringResource(R.string.app_name_marked)
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
    val productName = stringResource(R.string.app_name_marked)
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
    val contacts by model.contacts.collectAsState()
    val contactMethods by model.contactMethods.collectAsState()
    val ensembles by model.ensembles.collectAsState()
    val eventEnsembles by model.eventEnsembles.collectAsState()
    val eventContacts by model.eventContacts.collectAsState()
    val ensembleContacts by model.ensembleContacts.collectAsState()
    val venueContacts by model.venueContacts.collectAsState()
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
    val estimatedStudioValue = specRows
        .groupBy { it.optString("item_id") }
        .values
        .sumOf { itemSpecs -> estimatedItemValue(itemSpecs.associate { it.optString("key") to it.optString("value") }) }
    val maintenanceHistory by model.maintenanceHistory.collectAsState()
    val careRows = maintenanceRows(specRows, items.map(::supportingJson), brands.map(::supportingJson), locations.map(::supportingJson), fieldNotes = maintenanceNotes.map(::recordJson), completions = maintenanceHistory.filter { it.revision == 0 }.map(::recordJson))
    val tracked = careRows.size
    val openBuddy = actions.map(::supportingJson).count { it.optString("status") !in setOf("handled", "cleared") }
    var peopleEvent by remember { mutableStateOf<JSONObject?>(null) }
    Box(Modifier.fillMaxSize()) {
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
                        Text("$${"%,.2f".format(estimatedStudioValue)}", color = Cyan, fontSize = 22.sp, fontWeight = FontWeight.Black)
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
                MetricCard(agentName, openBuddy.toString(), Modifier.weight(1f))
                MetricCard("Care", tracked.toString(), Modifier.weight(1f))
            }
        }
        item { SectionHeading("SCHEDULE", "Coming up next.", eyebrowColor = Color.White) }
        if (upcoming.isEmpty()) item { EmptyCard("No upcoming sessions are stored on this device.") }
        items(upcoming.take(5), key = { it.getString("id") }) { event ->
            val readiness = eventPacketReadiness(event, entries, attachments, cachedAttachments)
            val eventPeople = resolveEventPeople(event, contacts, ensembles, eventEnsembles, eventContacts, ensembleContacts, venueContacts)
            val eventGroupNames = resolveEventGroupNames(event, ensembles, eventEnsembles)
            EventCard(
                event,
                readiness,
                open = { if (event.optString("set_list_id").isNotBlank()) openGig(event.getString("id")) },
                peopleCount = eventPeople.size,
                people = if (eventPeople.isNotEmpty() || eventGroupNames.isNotEmpty()) ({ peopleEvent = event }) else null,
            )
        }
        item { SectionHeading("CARE READINESS", "What needs hands on it?") }
        item { CareSummary(careRows, model) }
        item { SubBrandSectionHeading(SubBrand.Crew, "Recent activity.") }
        if (actions.isEmpty()) item { EmptyCard("No $agentName actions are stored on this device.") }
        items(actions.take(5), key = { it.entityId }) { action -> BuddyActionCard(supportingJson(action)) }
        item { Spacer(Modifier.height(30.dp)) }
    }
    peopleEvent?.let { event ->
        EventPeopleDialog(
            event,
            resolveEventPeople(event, contacts, ensembles, eventEnsembles, eventContacts, ensembleContacts, venueContacts),
            contactMethods,
            resolveEventGroupNames(event, ensembles, eventEnsembles),
        ) { peopleEvent = null }
    }
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
    val contacts by model.contacts.collectAsState()
    val contactMethods by model.contactMethods.collectAsState()
    val ensembles by model.ensembles.collectAsState()
    val eventEnsembles by model.eventEnsembles.collectAsState()
    val eventContacts by model.eventContacts.collectAsState()
    val ensembleContacts by model.ensembleContacts.collectAsState()
    val venueContacts by model.venueContacts.collectAsState()
    val entries by model.entries.collectAsState()
    val attachments by model.attachments.collectAsState()
    val cachedAttachments by model.cachedAttachments.collectAsState()
    val reportState by model.reportState.collectAsState()
    val online = rememberNetworkConnected()
    var query by remember { mutableStateOf("") }
    var sessionTab by remember { mutableStateOf("Schedule") }
    var type by remember { mutableStateOf("All") }
    var editingEvent by remember { mutableStateOf<EditorTarget?>(null) }
    var exportTarget by remember { mutableStateOf<ExportTarget?>(null) }
    var localLiveEvent by remember { mutableStateOf<JSONObject?>(null) }
    var showLocalLive by remember { mutableStateOf(false) }
    var peopleEvent by remember { mutableStateOf<JSONObject?>(null) }
    val venueNames = venues.associate { it.entityId to recordJson(it).optString("name") }
    val rows = events.map(::recordJson).onEach { event ->
        val venueName = venueNames[event.optString("venue_id")].orEmpty()
        if (venueName.isNotBlank()) event.put("location", listOf(venueName, event.optString("location")).filter(String::isNotBlank).joinToString(" - "))
    }.filter {
        (type == "All" || it.optString("event_type").humanize() == type) && (query.isBlank() || it.toString().contains(query, true))
    }.sortedBy { it.optString("event_date") + it.optString("start_time") }
    Box(Modifier.fillMaxSize()) {
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            if (sessionTab == "Leviathan Live") SubBrandSectionHeading(SubBrand.Live)
            else SectionHeading("SESSIONS", "Schedule")
        }
        item { SessionChoiceStrip(sessionTab) { sessionTab = it } }
        if (sessionTab == "Leviathan Live") {
            item { LeviathanLiveSettingsPanel(model) }
        } else {
            item { StudioButton(onClick = { editingEvent = EditorTarget(null, JSONObject()) }, modifier = Modifier.fillMaxWidth()) { Text("Add Scheduled Event", color = Ink, fontWeight = FontWeight.Black) } }
            item { StudioButton(onClick = { localLiveEvent = null; showLocalLive = true }, modifier = Modifier.fillMaxWidth(), kind = StudioButtonKind.Secondary) { Text("Local Live Network", color = Color.White, fontWeight = FontWeight.Bold) } }
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
                val eventPeople = resolveEventPeople(event, contacts, ensembles, eventEnsembles, eventContacts, ensembleContacts, venueContacts)
                val eventGroupNames = resolveEventGroupNames(event, ensembles, eventEnsembles)
                EventCard(
                    event,
                    eventPacketReadiness(event, entries, attachments, cachedAttachments),
                    open = { if (event.optString("set_list_id").isNotBlank()) openGig(event.getString("id")) },
                    edit = { editingEvent = EditorTarget(event.optString("id"), event) },
                    copy = {
                        editingEvent = EditorTarget(null, JSONObject(event.toString())
                            .put("_copy_source_id", event.optString("id"))
                            .put("event_status", "scheduled").put("event_date", "").put("start_time", "")
                            .put("end_date", "").put("end_time", ""))
                    },
                    host = if (event.optString("set_list_id").isNotBlank()) ({ localLiveEvent = event; showLocalLive = true }) else null,
                    peopleCount = eventPeople.size,
                    people = if (eventPeople.isNotEmpty() || eventGroupNames.isNotEmpty()) ({ peopleEvent = event }) else null,
                )
            }
        }
    }
    editingEvent?.let { target -> EventEditor(target, model, close = { editingEvent = null }) }
    exportTarget?.let { target -> ContextExportDialog(target, online, reportState, model) { exportTarget = null } }
    if (showLocalLive) LocalLiveDialog(model, localLiveEvent) { showLocalLive = false }
    peopleEvent?.let { event ->
        EventPeopleDialog(
            event,
            resolveEventPeople(event, contacts, ensembles, eventEnsembles, eventContacts, ensembleContacts, venueContacts),
            contactMethods,
            resolveEventGroupNames(event, ensembles, eventEnsembles),
        ) { peopleEvent = null }
    }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun LibraryScreen(model: StudioRackViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
    var copyingSetList by remember { mutableStateOf<CachedRecord?>(null) }
    var creatingSetList by remember { mutableStateOf(false) }
    var renamingSetList by remember { mutableStateOf<CachedRecord?>(null) }
    var exportTarget by remember { mutableStateOf<ExportTarget?>(null) }
    var durationBusy by remember { mutableStateOf(false) }
    var durationResult by remember { mutableStateOf<JSONObject?>(null) }
    var durationMessage by remember { mutableStateOf("") }
    var appliedDurationSongs by remember { mutableStateOf(emptySet<String>()) }
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
                Surface(color = Cyan.copy(alpha = .045f), border = BorderStroke(1.dp, Cyan.copy(alpha = .3f)), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Crew: Fill Missing Song Lengths", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Checks only songs without a length. Existing lengths are never changed.", color = TextSoft, fontSize = 11.sp)
                        StudioButton(onClick = {
                            durationBusy = true; durationMessage = "Crew is checking missing song lengths..."; appliedDurationSongs = emptySet()
                            scope.launch {
                                runCatching { model.fillMissingSongLengths() }
                                    .onSuccess { durationResult = it; durationMessage = "Finished checking missing lengths." }
                                    .onFailure { durationMessage = it.message ?: "Crew could not check song lengths." }
                                durationBusy = false
                            }
                        }, enabled = online && !durationBusy, modifier = Modifier.fillMaxWidth()) {
                            Text(if (durationBusy) "Crew is checking..." else "Fill Missing Song Lengths", color = Ink, fontWeight = FontWeight.Black)
                        }
                        if (!online) Text("Connect to search LRCLIB and MusicBrainz.", color = Amber, fontSize = 11.sp)
                        if (durationMessage.isNotBlank()) Text(durationMessage, color = TextSoft, fontSize = 12.sp)
                        durationResult?.let { payload ->
                            val updated = payload.optJSONArray("updated").jsonObjects()
                            val review = payload.optJSONArray("review").jsonObjects()
                            val unmatched = payload.optJSONArray("unmatched").jsonObjects()
                            Text("Filled ${updated.size}. Review ${review.size}. No match ${unmatched.size}.", color = Color.White, fontWeight = FontWeight.Bold)
                            updated.forEach { item -> Text("${item.optString("title")} - ${item.optString("duration_label")} (${item.optString("source")})", color = TextSoft, fontSize = 11.sp) }
                            review.forEach { song ->
                                Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                                    Text(listOf(song.optString("title"), song.optString("artist")).filter(String::isNotBlank).joinToString(" - "), color = Color.White, fontWeight = FontWeight.Bold)
                                    if (song.optString("song_id") in appliedDurationSongs) Text("Duration saved.", color = Cyan, fontWeight = FontWeight.Bold)
                                    else song.optJSONArray("candidates").jsonObjects().forEach { candidate ->
                                        StudioButton(onClick = {
                                            durationBusy = true
                                            scope.launch {
                                                runCatching { model.applySongDuration(song.optString("song_id"), candidate) }
                                                    .onSuccess { appliedDurationSongs = appliedDurationSongs + song.optString("song_id"); durationMessage = "Saved ${song.optString("title")} at ${candidate.optString("duration_label")}." }
                                                    .onFailure { durationMessage = it.message ?: "The duration could not be saved." }
                                                durationBusy = false
                                            }
                                        }, enabled = !durationBusy, modifier = Modifier.fillMaxWidth(), kind = StudioButtonKind.Secondary) {
                                            Text("Use ${candidate.optString("duration_label")} from ${candidate.optString("source")}", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
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
                    listOf(displaySongKey(song.optString("song_key")), song.optString("style"), song.optString("tempo"), song.optString("time_signature"), formatDuration(song.optInt("duration_seconds")), if (song.optInt("is_favorite") == 1) "Favorite" else "").filter(String::isNotBlank),
                    actionLabel = if (normalizedMediaLink(song.optString("media_ref")) != null) "Listen" else null,
                    action = normalizedMediaLink(song.optString("media_ref"))?.let { link -> { openMediaLink(context, link) } },
                ) {
                    DetailLine("Starts", song.optString("starts_by"))
                    DetailLine("Key", displaySongKey(song.optString("song_key")))
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
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { editingSetList = record }) { Text("Edit Set List", color = Amber) }
                        TextButton(onClick = { exportTarget = ExportTarget("setlists", row.optString("name", "Set List"), listOf(record.entityId)) }) { Text("Export", color = Amber) }
                        TextButton(onClick = { renamingSetList = record }) { Text("Rename", color = Amber) }
                        TextButton(onClick = { copyingSetList = record }) { Text("Copy", color = Amber) }
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
    if (creatingSetList || editingSetList != null || copyingSetList != null) {
        Dialog(onDismissRequest = { creatingSetList = false; editingSetList = null; copyingSetList = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            SetListEditor(editingSetList ?: copyingSetList, sections, entries, songs, attachments, model, copyMode = copyingSetList != null) { creatingSetList = false; editingSetList = null; copyingSetList = null }
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
        item { MoreChoiceStrip(tab, agentName) { tab = it } }
        when (tab) {
            "Reports" -> reportsContent(model)
            "Sharing" -> sharingContent(model)
            "People" -> directoryContent(model)
            agentName -> buddyContent(model)
            "Reference" -> referenceContent(model)
            "Sync" -> syncContent(model, uiState)
            else -> settingsContent(model, uiState)
        }
        item { BrandLegalCard() }
    }
}

@Composable
private fun BrandLegalCard() {
    val productName = stringResource(R.string.app_name_marked)
    val copyrightHolder = stringResource(R.string.copyright_holder)
    val registeredTagline = stringResource(R.string.registered_tagline_marked)
    val trademarkNotice = stringResource(R.string.trademark_notice)
    InfoCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.brand_logo), productName, Modifier.size(42.dp))
            Column(Modifier.padding(start = 10.dp)) {
                Text(productName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text(registeredTagline, color = Amber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text("Copyright ${java.time.Year.now().value} $copyrightHolder. All rights reserved.", color = TextSoft, fontSize = 11.sp)
        Text(trademarkNotice, color = TextSoft, fontSize = 10.sp, lineHeight = 14.sp)
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
    var liveShareRevision by remember(selectedGrant) { mutableStateOf("") }
    var liveShareConnected by remember(selectedGrant) { mutableStateOf(false) }
    val cacheById = cached.associateBy(CachedAttachment::attachmentId)

    LaunchedEffect(selectedGrant) {
        val grantId = selectedGrant ?: return@LaunchedEffect
        while (true) {
            val result = model.refreshLiveShare(grantId, liveShareRevision)
            liveShareRevision = result.revision
            liveShareConnected = result.connected
            delay(2_500)
        }
    }

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
                    Text(if (liveShareConnected) "LIVE UPDATES CONNECTED" else "OFFLINE COPY", color = if (liveShareConnected) Color(0xFF58E99B) else TextSoft, fontSize = 10.sp, fontWeight = FontWeight.Black)
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
                                        Text(listOf(value.optString("starts_by"), displaySongKey(value.optString("song_key")), value.optString("style"), value.optString("tempo"), value.optString("time_signature")).filter(String::isNotBlank).joinToString(" | "), color = TextSoft, fontSize = 12.sp)
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
@OptIn(ExperimentalLayoutApi::class)
private fun DirectoryPanel(model: StudioRackViewModel) {
    val context = LocalContext.current
    val venues by model.venues.collectAsState()
    val contacts by model.contacts.collectAsState()
    val contactMethods by model.contactMethods.collectAsState()
    val ensembles by model.ensembles.collectAsState()
    val venueContacts by model.venueContacts.collectAsState()
    val ensembleContacts by model.ensembleContacts.collectAsState()
    var tab by remember { mutableStateOf("Venues") }
    var mode by remember { mutableStateOf("Browse") }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<EditorTarget?>(null) }
    var managing by remember { mutableStateOf<Pair<String, CachedRecord>?>(null) }
    val entityType = when (tab) { "Contacts" -> "contact"; "Bands / Groups" -> "ensemble"; else -> "venue" }
    val records = when (entityType) { "contact" -> contacts; "ensemble" -> ensembles; else -> venues }
    val relationshipMode = if (entityType == "ensemble") "Members" else "Contacts"
    val filtered = records.filter {
        val data = recordJson(it)
        val relationshipText = when (entityType) {
            "ensemble" -> ensembleContacts.filter { relation -> recordJson(relation).optString("ensemble_id") == it.entityId }
            "venue" -> venueContacts.filter { relation -> recordJson(relation).optString("venue_id") == it.entityId }
            else -> emptyList()
        }.joinToString(" ") { relation ->
            val relationData = recordJson(relation)
            val contact = contacts.firstOrNull { candidate -> candidate.entityId == relationData.optString("contact_id") }
            val contactData = contact?.let(::recordJson) ?: JSONObject()
            val methods = contact?.let { linked -> contactMethods.filter { method -> recordJson(method).optString("contact_id") == linked.entityId } }.orEmpty()
            listOf(
                relationData.optString("relationship_role"), contactData.optString("display_name"),
                contactData.optString("organization_name"), contactData.optString("job_title"),
                contactData.optString("phone"), contactData.optString("email"),
                methods.joinToString(" ") { method -> recordJson(method).optString("value") },
            ).joinToString(" ")
        }
        listOf(data.optString("name"), data.optString("display_name"), data.optString("organization_name"), data.optString("city"), relationshipText)
            .joinToString(" ").contains(query, ignoreCase = true)
    }.sortedBy { recordJson(it).optString(if (entityType == "contact") "display_name" else "name").lowercase() }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeading("PEOPLE & PLACES", "Directory")
        ChoiceStrip(listOf("Venues", "Contacts", "Bands / Groups"), tab) { tab = it; query = ""; mode = "Browse" }
        val modes = if (entityType == "contact") listOf("Browse", "Add") else listOf("Browse", "Add", relationshipMode)
        ChoiceStrip(modes, mode) { choice ->
            mode = choice
            if (choice == "Add") editing = EditorTarget(null, JSONObject())
        }
        if (mode != "Add") StudioField("Find ${tab.lowercase()}", query) { query = it }
        if (mode == relationshipMode && entityType != "contact") {
            if (contacts.isEmpty()) Text("Add contacts first, then return here to connect them.", color = TextSoft)
            filtered.forEach { record ->
                val data = recordJson(record)
                val count = if (entityType == "ensemble") ensembleContacts.count { recordJson(it).optString("ensemble_id") == record.entityId }
                    else venueContacts.count { recordJson(it).optString("venue_id") == record.entityId }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PanelRaised),
                    border = BorderStroke(1.dp, Color(0xFF343B4D)),
                ) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(data.optString("name"), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("$count ${if (entityType == "ensemble") "member" else "contact"}${if (count == 1) "" else "s"}", color = TextSoft)
                        }
                        StudioButton(onClick = { managing = entityType to record }) {
                            Text(if (entityType == "ensemble") "Manage Members" else "Manage Contacts", color = Ink, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        } else if (mode != "Add") filtered.forEach { record ->
            val data = recordJson(record)
            val title = data.optString(if (entityType == "contact") "display_name" else "name")
            val detail = when (entityType) {
                "venue" -> listOf(data.optString("city"), data.optString("region")).filter(String::isNotBlank).joinToString(", ")
                "contact" -> listOf(data.optString("job_title"), data.optString("organization_name")).filter(String::isNotBlank).joinToString(" / ")
                else -> data.optString("ensemble_type", "band").humanize()
            }
            val connectedCount = when (entityType) {
                "venue" -> venueContacts.count { recordJson(it).optString("venue_id") == record.entityId }
                "ensemble" -> ensembleContacts.count { recordJson(it).optString("ensemble_id") == record.entityId }
                else -> 0
            }
            val relationshipRows = when (entityType) {
                "venue" -> venueContacts.filter { recordJson(it).optString("venue_id") == record.entityId }
                "ensemble" -> ensembleContacts.filter { recordJson(it).optString("ensemble_id") == record.entityId }
                else -> emptyList()
            }
            val linkedContacts = relationshipRows.mapNotNull { relation ->
                val contactId = recordJson(relation).optString("contact_id")
                contacts.firstOrNull { it.entityId == contactId }?.let { it to recordJson(relation).optString("relationship_role") }
            }
            val recordMethods = if (entityType == "contact") contactMethods.filter { recordJson(it).optString("contact_id") == record.entityId }
                .sortedWith(compareByDescending<CachedRecord> { recordJson(it).optInt("is_primary") }.thenBy { recordJson(it).optInt("position") }) else emptyList()
            ExpandableRecordCard(
                title = title,
                subtitle = detail,
                chips = listOf(data.optString("phone"), if (entityType != "contact") "$connectedCount contacts" else ""),
                imageUrl = data.optString("image_url"),
                showImage = true,
                circularImage = entityType == "contact",
                imageFallback = title.take(1).uppercase(),
            ) {
                when (entityType) {
                    "venue" -> {
                        DetailLine("Address", listOf(data.optString("address_line1"), data.optString("address_line2"), data.optString("city"), data.optString("region"), data.optString("postal_code")).filter(String::isNotBlank).joinToString(", "))
                        DetailLine("Phone", data.optString("phone")); DetailLine("Email", data.optString("email")); DetailLine("Website", data.optString("website"))
                        DetailLine("Load-in", data.optString("load_in_notes")); DetailLine("Parking", data.optString("parking_notes")); DetailLine("Private notes", data.optString("notes"))
                    }
                    "contact" -> {
                        DetailLine("Organization", data.optString("organization_name")); DetailLine("Role", data.optString("job_title"))
                        if (recordMethods.isEmpty()) {
                            DetailLine("Phone", data.optString("phone")); DetailLine("Email", data.optString("email"))
                        } else recordMethods.forEach { method ->
                            val methodData = recordJson(method)
                            DetailLine(methodData.optString("label", methodData.optString("method_type").humanize()), methodData.optString("value"))
                        }
                        DetailLine("Private notes", data.optString("notes"))
                    }
                    else -> { DetailLine("Type", data.optString("ensemble_type").humanize()); DetailLine("Website", data.optString("website")); DetailLine("Private notes", data.optString("notes")) }
                }
                if (linkedContacts.isNotEmpty()) {
                    if (entityType == "ensemble") {
                        val phones = linkedContacts.map { recordJson(it.first).optString("phone") }.filter(String::isNotBlank).distinct()
                        val emails = linkedContacts.map { recordJson(it.first).optString("email") }.filter(String::isNotBlank).distinct()
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (phones.isNotEmpty()) StudioButton(onClick = { openGroupContactLink(context, "smsto", phones) }) { Text("Text Group", color = Ink, fontWeight = FontWeight.Bold) }
                            if (emails.isNotEmpty()) StudioButton(onClick = { openGroupContactLink(context, "mailto", emails) }, kind = StudioButtonKind.Secondary) { Text("Email Group", color = Color.White, fontWeight = FontWeight.Bold) }
                        }
                    }
                    Text(if (entityType == "ensemble") "MEMBERS" else "VENUE CONTACTS", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    linkedContacts.forEach { (contact, role) ->
                        DirectoryContactRow(
                            recordJson(contact), role, context,
                            contactMethods.filter { recordJson(it).optString("contact_id") == contact.entityId }.map(::recordJson),
                        )
                    }
                }
                if (entityType != "contact") {
                    Spacer(Modifier.height(6.dp))
                    Surface(Modifier.fillMaxWidth().height(1.dp), color = Color(0xFF4A5265)) {}
                    Text(if (entityType == "ensemble") "GROUP CONTROLS" else "VENUE CONTROLS", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (entityType == "contact" && recordMethods.isNotEmpty()) recordMethods.forEach { method ->
                        val methodData = recordJson(method); val value = methodData.optString("value"); val methodLabel = methodData.optString("label", "Other")
                        if (methodData.optString("method_type") == "email") {
                            StudioButton(onClick = { openContactLink(context, "mailto", value) }, kind = StudioButtonKind.Secondary) { Text("Email $methodLabel", color = Color.White) }
                        } else {
                            StudioButton(onClick = { openContactLink(context, "tel", value) }, kind = if (methodData.optInt("is_primary") == 1) StudioButtonKind.Primary else StudioButtonKind.Secondary) { Text("Call $methodLabel", color = if (methodData.optInt("is_primary") == 1) Ink else Color.White, fontWeight = FontWeight.Bold) }
                            StudioButton(onClick = { openContactLink(context, "smsto", value) }, kind = StudioButtonKind.Secondary) { Text("Text $methodLabel", color = Color.White) }
                        }
                    } else {
                        data.optString("phone").takeIf(String::isNotBlank)?.let { phone ->
                            StudioButton(onClick = { openContactLink(context, "tel", phone) }) { Text("Call", color = Ink, fontWeight = FontWeight.Bold) }
                            StudioButton(onClick = { openContactLink(context, "smsto", phone) }, kind = StudioButtonKind.Secondary) { Text("Text", color = Color.White) }
                        }
                        data.optString("email").takeIf(String::isNotBlank)?.let { email -> StudioButton(onClick = { openContactLink(context, "mailto", email) }, kind = StudioButtonKind.Secondary) { Text("Email", color = Color.White) } }
                    }
                    normalizedMediaLink(data.optString("maps_url"))?.let { link -> StudioButton(onClick = { openMediaLink(context, link) }, kind = StudioButtonKind.Secondary) { Text("Directions", color = Color.White) } }
                    normalizedMediaLink(data.optString("website"))?.let { link -> StudioButton(onClick = { openMediaLink(context, link) }, kind = StudioButtonKind.Secondary) { Text("Website", color = Color.White) } }
                    StudioButton(onClick = { editing = EditorTarget(record.entityId, data) }, kind = StudioButtonKind.Secondary) { Text("Edit", color = Color.White) }
                    if (entityType != "contact") StudioButton(onClick = { managing = entityType to record }, kind = StudioButtonKind.Secondary) { Text(if (entityType == "ensemble") "Manage Members" else "Manage Contacts", color = Color.White) }
                }
            }
        }
        if (mode != "Add" && filtered.isEmpty()) Text("No ${tab.lowercase()} match this search.", color = TextSoft)
    }
    editing?.let { target -> DirectoryEditor(entityType, target, model) { editing = null; mode = "Browse" } }
    managing?.let { (parentType, record) ->
        DirectoryRelationshipsDialog(
            parentType, record, contacts,
            if (parentType == "venue") venueContacts else ensembleContacts,
            model,
        ) { managing = null }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun DirectoryContactRow(contact: JSONObject, relationshipRole: String, context: Context, methods: List<JSONObject> = emptyList()) {
    var expanded by remember(contact.optString("id"), contact.optString("display_name")) { mutableStateOf(false) }
    val displayName = contact.optString("display_name", "Contact")
    Surface(
        modifier = Modifier.fillMaxWidth(), color = Color(0xFF171C29),
        shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFF343B4D)),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val photoModifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF2B3242))
                if (contact.optString("image_url").isNotBlank()) {
                    CachedNetworkImage(contact.optString("image_url"), displayName, photoModifier, ContentScale.Crop, fallbackText = displayName.take(1).uppercase())
                } else Box(photoModifier, contentAlignment = Alignment.Center) {
                    Text(displayName.take(1).uppercase(), color = Amber, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(displayName, color = Color.White, fontWeight = FontWeight.Bold)
                    val role = relationshipRole.ifBlank { contact.optString("job_title") }
                    if (role.isNotBlank()) Text(role, color = TextSoft, fontSize = 12.sp)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                contact.optString("phone").takeIf(String::isNotBlank)?.let { phone ->
                    StudioButton(onClick = { openContactLink(context, "tel", phone) }) { Text("Call", color = Ink, fontWeight = FontWeight.Bold) }
                    StudioButton(onClick = { openContactLink(context, "smsto", phone) }, kind = StudioButtonKind.Secondary) { Text("Text", color = Color.White) }
                }
                contact.optString("email").takeIf(String::isNotBlank)?.let { email ->
                    StudioButton(onClick = { openContactLink(context, "mailto", email) }, kind = StudioButtonKind.Secondary) { Text("Email", color = Color.White) }
                }
                StudioButton(onClick = { expanded = !expanded }, kind = StudioButtonKind.Secondary) { Text(if (expanded) "Close Profile" else "Profile", color = Color.White) }
            }
            if (expanded) {
                DetailLine("Organization", contact.optString("organization_name"))
                if (methods.isEmpty()) {
                    DetailLine("Phone", contact.optString("phone"))
                    DetailLine("Email", contact.optString("email"))
                } else methods.sortedWith(compareByDescending<JSONObject> { it.optInt("is_primary") }.thenBy { it.optInt("position") }).forEach { method ->
                    DetailLine(method.optString("label", method.optString("method_type").humanize()), method.optString("value"))
                }
                if (methods.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    methods.forEach { method ->
                        val value = method.optString("value"); val label = method.optString("label", "Other")
                        if (method.optString("method_type") == "email") StudioButton(onClick = { openContactLink(context, "mailto", value) }, kind = StudioButtonKind.Secondary) { Text("Email $label", color = Color.White) }
                        else {
                            StudioButton(onClick = { openContactLink(context, "tel", value) }, kind = StudioButtonKind.Secondary) { Text("Call $label", color = Color.White) }
                            StudioButton(onClick = { openContactLink(context, "smsto", value) }, kind = StudioButtonKind.Secondary) { Text("Text $label", color = Color.White) }
                        }
                    }
                }
                DetailLine("Notes", contact.optString("notes"))
            }
        }
    }
}

private data class ContactMethodDraft(
    val id: String = "",
    val type: String,
    val label: String,
    val value: String,
    val isPrimary: Boolean = false,
)

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun DirectoryEditor(entityType: String, target: EditorTarget, model: StudioRackViewModel, close: () -> Unit) {
    val context = LocalContext.current
    val syncedContactMethods by model.contactMethods.collectAsState()
    val original = target.data
    var name by remember { mutableStateOf(original.optString(if (entityType == "contact") "display_name" else "name")) }
    var type by remember { mutableStateOf(original.optString("ensemble_type", "band")) }
    var organization by remember { mutableStateOf(original.optString("organization_name")) }
    var title by remember { mutableStateOf(original.optString("job_title")) }
    var email by remember { mutableStateOf(original.optString("email")) }
    var phone by remember { mutableStateOf(original.optString("phone")) }
    val initialMethodRecords = remember(target.id, syncedContactMethods) {
        syncedContactMethods.filter { recordJson(it).optString("contact_id") == target.id }
    }
    var contactMethodDrafts by remember(target.id, initialMethodRecords.map { it.entityId }) {
        mutableStateOf(
            initialMethodRecords.sortedBy { recordJson(it).optInt("position") }.map { record ->
                val method = recordJson(record)
                ContactMethodDraft(record.entityId, method.optString("method_type", "phone"), method.optString("label", "Other"), method.optString("value"), method.optInt("is_primary") == 1)
            }.ifEmpty {
                listOfNotNull(
                    original.optString("phone").takeIf(String::isNotBlank)?.let { ContactMethodDraft(type = "phone", label = "Mobile", value = it, isPrimary = true) },
                    original.optString("email").takeIf(String::isNotBlank)?.let { ContactMethodDraft(type = "email", label = "Primary", value = it, isPrimary = true) },
                )
            },
        )
    }
    var address by remember { mutableStateOf(original.optString("address_line1")) }
    var city by remember { mutableStateOf(original.optString("city")) }
    var region by remember { mutableStateOf(original.optString("region")) }
    var postalCode by remember { mutableStateOf(original.optString("postal_code")) }
    var website by remember { mutableStateOf(original.optString("website")) }
    var mapsUrl by remember { mutableStateOf(original.optString("maps_url")) }
    var imageUrl by remember { mutableStateOf(original.optString("image_url")) }
    var selectedImageUri by remember(target.id) { mutableStateOf<Uri?>(null) }
    var selectedImageName by remember(target.id) { mutableStateOf("") }
    var selectedImageMime by remember(target.id) { mutableStateOf("") }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            selectedImageName = contentDisplayName(context, uri)
            selectedImageMime = context.contentResolver.getType(uri).orEmpty()
        }
    }
    val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                readPickedContact(context, uri)?.let { imported ->
                    name = imported.name.ifBlank { name }
                    organization = imported.organization.ifBlank { organization }
                    title = imported.jobTitle.ifBlank { title }
                    if (imported.methods.isNotEmpty()) {
                        contactMethodDrafts = imported.methods
                        phone = imported.methods.firstOrNull { it.type == "phone" && it.isPrimary }?.value
                            ?: imported.methods.firstOrNull { it.type == "phone" }?.value.orEmpty()
                        email = imported.methods.firstOrNull { it.type == "email" && it.isPrimary }?.value
                            ?: imported.methods.firstOrNull { it.type == "email" }?.value.orEmpty()
                    }
                    imported.photoUri?.let { photo ->
                        selectedImageUri = photo
                        selectedImageName = "${imported.name.ifBlank { "contact" }}-photo.jpg"
                        selectedImageMime = context.contentResolver.getType(photo).orEmpty().ifBlank { "image/jpeg" }
                    }
                } ?: Toast.makeText(context, "The selected contact could not be read.", Toast.LENGTH_LONG).show()
            }
        }
    }
    val contactPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) contactPicker.launch(Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI))
        else Toast.makeText(context, "Contact access is needed only to import the person you select.", Toast.LENGTH_LONG).show()
    }
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
                if (imageUrl.isNotBlank()) CachedNetworkImage(imageUrl, "Current venue photo", Modifier.size(150.dp).clip(RoundedCornerShape(8.dp)), ContentScale.Crop, fallbackText = name.take(1).uppercase())
                StudioButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth(), kind = StudioButtonKind.Secondary) {
                    Text(if (selectedImageName.isBlank()) "Choose Venue Photo" else "Photo: $selectedImageName", color = Color.White, fontWeight = FontWeight.Bold)
                }
                StudioField("Venue Photo URL", imageUrl) { imageUrl = it }
                StudioField("Website", website) { website = it }; StudioField("Google Maps Link", mapsUrl) { mapsUrl = it }
                StudioField("Load-in Notes", loadIn, singleLine = false) { loadIn = it }
                StudioField("Parking Notes", parking, singleLine = false) { parking = it }
            }
            "contact" -> {
                StudioButton(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                            contactPicker.launch(Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI))
                        } else contactPermission.launch(Manifest.permission.READ_CONTACTS)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Import From Device Contacts", color = Ink, fontWeight = FontWeight.Black) }
                StudioField("Organization", organization) { organization = it }; StudioField("Title / Role", title) { title = it }
                Text("PHONE NUMBERS & EMAIL", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Black)
                contactMethodDrafts.forEachIndexed { index, method ->
                    Card(colors = CardDefaults.cardColors(containerColor = PanelRaised), border = BorderStroke(1.dp, Color(0xFF343B4D))) {
                        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChoiceStrip(listOf("phone", "email"), method.type) { selected ->
                                contactMethodDrafts = contactMethodDrafts.toMutableList().also { it[index] = method.copy(type = selected) }
                            }
                            StudioField("Label (Mobile, Home, Work, etc.)", method.label) { value ->
                                contactMethodDrafts = contactMethodDrafts.toMutableList().also { it[index] = method.copy(label = value) }
                            }
                            StudioField(if (method.type == "email") "Email" else "Phone Number", method.value) { value ->
                                contactMethodDrafts = contactMethodDrafts.toMutableList().also { it[index] = method.copy(value = value) }
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StudioButton(onClick = {
                                    contactMethodDrafts = contactMethodDrafts.mapIndexed { row, candidate ->
                                        if (candidate.type == method.type) candidate.copy(isPrimary = row == index) else candidate
                                    }
                                }, kind = if (method.isPrimary) StudioButtonKind.Primary else StudioButtonKind.Secondary) {
                                    Text(if (method.isPrimary) "Preferred" else "Make Preferred", color = if (method.isPrimary) Ink else Color.White)
                                }
                                StudioButton(onClick = { contactMethodDrafts = contactMethodDrafts.filterIndexed { row, _ -> row != index } }, kind = StudioButtonKind.Secondary) { Text("Remove", color = Color.White) }
                            }
                        }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StudioButton(onClick = { contactMethodDrafts = contactMethodDrafts + ContactMethodDraft(type = "phone", label = "Mobile", value = "", isPrimary = contactMethodDrafts.none { it.type == "phone" }) }, kind = StudioButtonKind.Secondary) { Text("Add Phone", color = Color.White) }
                    StudioButton(onClick = { contactMethodDrafts = contactMethodDrafts + ContactMethodDraft(type = "email", label = "Primary", value = "", isPrimary = contactMethodDrafts.none { it.type == "email" }) }, kind = StudioButtonKind.Secondary) { Text("Add Email", color = Color.White) }
                }
                if (imageUrl.isNotBlank()) CachedNetworkImage(imageUrl, "Current contact photo", Modifier.size(150.dp).clip(CircleShape), ContentScale.Crop, fallbackText = name.take(1).uppercase())
                StudioButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth(), kind = StudioButtonKind.Secondary) {
                    Text(if (selectedImageName.isBlank()) "Choose Contact Photo" else "Photo: $selectedImageName", color = Color.White, fontWeight = FontWeight.Bold)
                }
                StudioField("Contact Photo URL", imageUrl) { imageUrl = it }
            }
            else -> {
                Text("Type", color = TextSoft, fontWeight = FontWeight.Bold)
                ChoiceStrip(listOf("band", "worship_group", "studio", "production_company", "other"), type) { type = it }
                if (imageUrl.isNotBlank()) CachedNetworkImage(imageUrl, "Current group photo", Modifier.size(150.dp).clip(RoundedCornerShape(8.dp)), ContentScale.Crop, fallbackText = name.take(1).uppercase())
                StudioButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth(), kind = StudioButtonKind.Secondary) {
                    Text(if (selectedImageName.isBlank()) "Choose Group Photo or Logo" else "Photo: $selectedImageName", color = Color.White, fontWeight = FontWeight.Bold)
                }
                StudioField("Group Photo URL", imageUrl) { imageUrl = it }
                StudioField("Website", website) { website = it }
            }
        }
        StudioField("Private Notes", notes, singleLine = false) { notes = it }
        EditorActions(name.isNotBlank(), save = {
            val data = JSONObject().put(if (entityType == "contact") "display_name" else "name", name.trim()).put("notes", notes.trim())
            when (entityType) {
                "venue" -> data.put("address_line1", address.trim()).put("city", city.trim()).put("region", region.trim()).put("postal_code", postalCode.trim()).put("phone", phone.trim()).put("email", email.trim()).put("image_url", imageUrl.trim()).put("website", website.trim()).put("maps_url", mapsUrl.trim()).put("load_in_notes", loadIn.trim()).put("parking_notes", parking.trim())
                "contact" -> {
                    val cleanMethods = contactMethodDrafts.filter { it.value.isNotBlank() }.map { method ->
                        JSONObject().put("id", method.id).put("method_type", method.type).put("label", method.label.trim().ifBlank { "Other" }).put("value", method.value.trim()).put("is_primary", if (method.isPrimary) 1 else 0)
                    }
                    val preferredPhone = cleanMethods.firstOrNull { it.optString("method_type") == "phone" && it.optInt("is_primary") == 1 }?.optString("value")
                        ?: cleanMethods.firstOrNull { it.optString("method_type") == "phone" }?.optString("value").orEmpty()
                    val preferredEmail = cleanMethods.firstOrNull { it.optString("method_type") == "email" && it.optInt("is_primary") == 1 }?.optString("value")
                        ?: cleanMethods.firstOrNull { it.optString("method_type") == "email" }?.optString("value").orEmpty()
                    data.put("organization_name", organization.trim()).put("job_title", title.trim()).put("email", preferredEmail).put("phone", preferredPhone).put("image_url", imageUrl.trim())
                    model.saveDirectoryRecord(entityType, target.id, data, selectedImageUri?.toString(), selectedImageName, selectedImageMime, cleanMethods, done = close)
                    return@EditorActions
                }
                else -> data.put("ensemble_type", type).put("website", website.trim()).put("image_url", imageUrl.trim())
            }
            model.saveDirectoryRecord(entityType, target.id, data, selectedImageUri?.toString(), selectedImageName, selectedImageMime, done = close)
        }, delete = target.id?.let { id -> { model.deleteDirectoryRecord(entityType, id, close) } })
    }
}

private data class ImportedDeviceContact(
    val name: String,
    val organization: String,
    val jobTitle: String,
    val methods: List<ContactMethodDraft>,
    val photoUri: Uri?,
)

private fun readPickedContact(context: Context, contactUri: Uri): ImportedDeviceContact? {
    var name = ""
    var contactId = ""
    var photoUri: Uri? = null
    runCatching { context.contentResolver.query(
        contactUri,
        arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts.PHOTO_URI),
        null,
        null,
        null,
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            contactId = cursor.getString(0).orEmpty()
            name = cursor.getString(1).orEmpty()
            photoUri = cursor.getString(2)?.takeIf(String::isNotBlank)?.let(Uri::parse)
        }
    } }
    if (contactId.isBlank()) return null
    val methods = mutableListOf<ContactMethodDraft>()
    runCatching { context.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.LABEL, ContactsContract.CommonDataKinds.Phone.IS_PRIMARY),
        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID}=?", arrayOf(contactId),
        "${ContactsContract.CommonDataKinds.Phone.IS_PRIMARY} DESC",
    )?.use { cursor ->
        while (cursor.moveToNext()) {
            methods += ContactMethodDraft(
                type = "phone",
                label = ContactsContract.CommonDataKinds.Phone.getTypeLabel(context.resources, cursor.getInt(1), cursor.getString(2)).toString(),
                value = cursor.getString(0).orEmpty(), isPrimary = cursor.getInt(3) == 1,
            )
        }
    } }
    runCatching { context.contentResolver.query(
        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
        arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS, ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.LABEL, ContactsContract.CommonDataKinds.Email.IS_PRIMARY),
        "${ContactsContract.CommonDataKinds.Email.CONTACT_ID}=?", arrayOf(contactId),
        "${ContactsContract.CommonDataKinds.Email.IS_PRIMARY} DESC",
    )?.use { cursor ->
        while (cursor.moveToNext()) {
            methods += ContactMethodDraft(
                type = "email",
                label = ContactsContract.CommonDataKinds.Email.getTypeLabel(context.resources, cursor.getInt(1), cursor.getString(2)).toString(),
                value = cursor.getString(0).orEmpty(), isPrimary = cursor.getInt(3) == 1,
            )
        }
    } }
    var organization = ""
    var jobTitle = ""
    runCatching { context.contentResolver.query(
        ContactsContract.Data.CONTENT_URI,
        arrayOf(ContactsContract.CommonDataKinds.Organization.COMPANY, ContactsContract.CommonDataKinds.Organization.TITLE),
        "${ContactsContract.Data.CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
        arrayOf(contactId, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE), null,
    )?.use { cursor -> if (cursor.moveToFirst()) { organization = cursor.getString(0).orEmpty(); jobTitle = cursor.getString(1).orEmpty() } } }
    val normalized = methods.mapIndexed { index, method ->
        if (methods.none { it.type == method.type && it.isPrimary } && methods.indexOfFirst { it.type == method.type } == index) method.copy(isPrimary = true) else method
    }
    return ImportedDeviceContact(name, organization, jobTitle, normalized, photoUri)
}

@Composable
private fun DirectoryRelationshipsDialog(parentType: String, parent: CachedRecord, contacts: List<CachedRecord>, relationships: List<CachedRecord>, model: StudioRackViewModel, close: () -> Unit) {
    val parentKey = if (parentType == "venue") "venue_id" else "ensemble_id"
    val selectedAtOpen = relationships.filter { recordJson(it).optString(parentKey) == parent.entityId }.mapTo(mutableSetOf()) { recordJson(it).optString("contact_id") }
    var selected by remember { mutableStateOf(selectedAtOpen) }
    var query by remember(parent.entityId) { mutableStateOf("") }
    val filteredContacts = contacts.filter { contact ->
        val data = recordJson(contact)
        listOf(data.optString("display_name"), data.optString("organization_name"), data.optString("job_title"), data.optString("phone"), data.optString("email"))
            .joinToString(" ").contains(query, ignoreCase = true)
    }.sortedBy { recordJson(it).optString("display_name").lowercase() }
    EditorDialog("${if (parentType == "ensemble") "Members" else "Contacts"} for ${recordJson(parent).optString("name")}", close) {
        if (contacts.isEmpty()) Text("Add contacts in the Contact tab first.", color = TextSoft)
        else {
            StudioField("Find a person", query) { query = it }
            Text("${selected.size} selected / ${filteredContacts.size} shown", color = TextSoft, fontSize = 12.sp)
        }
        filteredContacts.forEach { contact ->
            val checked = contact.entityId in selected
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked, { enabled -> selected = selected.toMutableSet().apply { if (enabled) add(contact.entityId) else remove(contact.entityId) } })
                Column {
                    Text(recordJson(contact).optString("display_name"), color = Color.White, fontWeight = FontWeight.Bold)
                    val detail = listOf(recordJson(contact).optString("job_title"), recordJson(contact).optString("organization_name")).filter(String::isNotBlank).joinToString(" / ")
                    if (detail.isNotBlank()) Text(detail, color = TextSoft, fontSize = 12.sp)
                }
            }
        }
        if (contacts.isNotEmpty() && filteredContacts.isEmpty()) Text("No contacts match this search.", color = TextSoft)
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
    item { SubBrandSectionHeading(SubBrand.Crew) }
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
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Account and Device", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            InfoCard { DetailLine("Studio", account.optString("studio_name")); DetailLine("Account", account.optString("email")); DetailLine("Address", studioAddress(account)); DetailLine("Phone", account.optString("phone")); DetailLine("Contact", account.optString("contact_email")); DetailLine("Last sync", state?.lastSyncAt?.let { DateFormat.getDateTimeInstance().format(Date(it)) }.orEmpty()) }
            StudioButton(onClick = model::sync, enabled = !uiState.busy, modifier = Modifier.fillMaxWidth()) { Text(if (uiState.busy) "Synchronizing" else "Synchronize", color = Ink, fontWeight = FontWeight.Black) }
            Text("Leviathan Live settings are managed from Sessions > Leviathan Live.", color = TextSoft, fontSize = 12.sp)
        }
    }
}

@Composable
private fun LeviathanLiveSettingsPanel(model: StudioRackViewModel) {
    val state by model.syncState.collectAsState()
    val loadedSettings = remember(state?.performanceSettingsJson) {
        PerformanceSettings.fromJson(state?.performanceSettingsJson ?: "{}")
    }
    var settings by remember(state?.performanceSettingsJson) { mutableStateOf(loadedSettings) }
    var section by remember { mutableStateOf("Live Settings") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChoiceStrip(listOf("Live Settings", "Performance Material", "Page Turner"), section) { section = it }
        when (section) {
            "Live Settings" -> {
                SubBrandLockup(SubBrand.Live)
                Text("Settings", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                SettingToggle("Show clock", settings.showClock) { settings = settings.copy(showClock = it) }
                SettingToggle("Show elapsed set time", settings.showElapsed) { settings = settings.copy(showElapsed = it) }
                SettingToggle("Show estimated time remaining", settings.showSetRemaining) { settings = settings.copy(showSetRemaining = it) }
                Text("Metronome", color = Amber, fontSize = 18.sp, fontWeight = FontWeight.Black)
                SettingToggle("Auto-start when the song changes", settings.metronomeAutostart) { settings = settings.copy(metronomeAutostart = it) }
                SettingToggle("Start muted", settings.metronomeMuted) { settings = settings.copy(metronomeMuted = it) }
                LabeledChoice("Beat mode", listOf("Tempo", "Downbeat"), settings.metronomeMode.replaceFirstChar(Char::uppercase)) {
                    settings = settings.copy(metronomeMode = it.lowercase())
                }
                LabeledChoice("Sound", listOf("Tone", "Clave", "Woodblock", "Cowbell"), settings.metronomeSound.replaceFirstChar(Char::uppercase)) {
                    settings = settings.copy(metronomeSound = it.lowercase())
                }
            }
            "Performance Material" -> {
                Text("Performance Material", color = Amber, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text("When a song has no explicit live default, Leviathan Live checks these attachment types in order.", color = TextSoft, fontSize = 13.sp)
                settings.attachmentPreferences.take(5).forEachIndexed { index, preference ->
                    AttachmentPreferenceChoice(index + 1, preference) { selected ->
                        settings = settings.copy(
                            attachmentPreferences = settings.attachmentPreferences.toMutableList().apply { this[index] = selected }
                        )
                    }
                }
            }
            else -> {
                Text("Bluetooth Page Turner", color = Amber, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text("Supports keyboard-mode pedals including AirTurn and Donner devices.", color = TextSoft, fontSize = 13.sp)
                SettingToggle("Enable pedal controls", settings.pedalEnabled) { settings = settings.copy(pedalEnabled = it) }
                SettingToggle("Reverse previous and next", settings.pedalReverse) { settings = settings.copy(pedalReverse = it) }
                LabeledChoice("Pedal mode", listOf("Hybrid", "Song", "Scroll"), settings.pedalMode.replaceFirstChar(Char::uppercase)) {
                    settings = settings.copy(pedalMode = it.lowercase())
                }
                LabeledChoice("Page scroll distance", listOf("Small", "Half", "Full"), settings.pedalScrollAmount.replaceFirstChar(Char::uppercase)) {
                    settings = settings.copy(pedalScrollAmount = it.lowercase())
                }
                PedalKeyChoice("Previous Song", settings.previousKey) { settings = settings.copy(previousKey = it) }
                PedalKeyChoice("Next Song", settings.nextKey) { settings = settings.copy(nextKey = it) }
                PedalKeyChoice("Metronome Start / Stop", settings.metronomeKey) { settings = settings.copy(metronomeKey = it) }
                PedalKeyChoice("Metronome Mute / Unmute", settings.muteKey) { settings = settings.copy(muteKey = it) }
            }
        }
        StudioButton(onClick = { model.savePerformanceSettings(settings) }, modifier = Modifier.fillMaxWidth()) {
            Text("Save Leviathan Live Settings", color = Ink, fontWeight = FontWeight.Black)
        }
        Text("Settings take effect on this device immediately and synchronize when connected.", color = TextSoft, fontSize = 12.sp)
    }
}

@Composable
private fun AttachmentPreferenceChoice(priority: Int, selected: String, choose: (String) -> Unit) {
    val options = linkedMapOf(
        "chart" to "Chart", "drum_chart" to "Drum Chart", "sheet_music" to "Sheet Music",
        "lyrics" to "Lyrics", "tab" to "Tab", "guitar_tab" to "Guitar Tab", "bass_tab" to "Bass Tab",
        "keyboard_part" to "Keyboard Part", "reference" to "Reference", "other" to "Other",
    )
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("Priority $priority", color = TextSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Box(Modifier.fillMaxWidth()) {
            StudioButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), kind = StudioButtonKind.Secondary) {
                Text(options[selected] ?: selected.humanize(), color = Color.White, fontWeight = FontWeight.Bold)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (value, label) ->
                    DropdownMenuItem(text = { Text(label) }, onClick = { choose(value); expanded = false })
                }
            }
        }
    }
}

@Composable
private fun SettingToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, color = Color.White, fontSize = 16.sp)
    }
}

@Composable
private fun LabeledChoice(label: String, options: List<String>, selected: String, choose: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = TextSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        ChoiceStrip(options, selected, choose)
    }
}

@Composable
private fun PedalKeyChoice(label: String, selected: String, choose: (String) -> Unit) {
    val keys = linkedMapOf("Left" to "ArrowLeft", "Right" to "ArrowRight", "Up" to "ArrowUp", "Down" to "ArrowDown")
    LabeledChoice(label, keys.keys.toList(), keys.entries.firstOrNull { it.value == selected }?.key ?: "Left") {
        choose(keys.getValue(it))
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

private enum class SubBrand { Live, Crew }

@Composable
private fun SubBrandSectionHeading(kind: SubBrand, title: String = "") {
    Column(Modifier.padding(top = 4.dp, bottom = 2.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        SubBrandLockup(kind)
        if (title.isNotBlank()) Text(title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SubBrandLockup(kind: SubBrand, modifier: Modifier = Modifier) {
    val accent = if (kind == SubBrand.Live) Amber else Cyan
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Image(painterResource(R.drawable.brand_logo), null, Modifier.size(54.dp))
        Box(Modifier.width(3.dp).height(46.dp).background(Amber, RoundedCornerShape(2.dp)))
        Column {
            Text("LEVIATHAN", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(kind.name.uppercase(), color = accent, fontSize = 27.sp, fontWeight = FontWeight.Black)
                SubBrandSignal(kind, Modifier.width(76.dp).height(24.dp))
            }
        }
    }
}

@Composable
private fun SubBrandSignal(kind: SubBrand, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (kind == SubBrand.Live) {
            val points = listOf(
                Offset(0f, size.height * .58f), Offset(size.width * .18f, size.height * .58f),
                Offset(size.width * .29f, size.height * .26f), Offset(size.width * .42f, size.height * .82f),
                Offset(size.width * .57f, size.height * .08f), Offset(size.width * .72f, size.height * .58f),
                Offset(size.width, size.height * .58f),
            )
            points.zipWithNext().forEach { (start, end) -> drawLine(Cyan, start, end, strokeWidth = 4.dp.toPx()) }
            drawCircle(Cyan, 4.dp.toPx(), points.last())
        } else {
            val nodes = listOf(
                Offset(size.width * .12f, size.height * .68f),
                Offset(size.width * .50f, size.height * .20f),
                Offset(size.width * .90f, size.height * .72f),
            )
            drawLine(Amber, nodes[0], nodes[1], strokeWidth = 3.dp.toPx())
            drawLine(Amber, nodes[1], nodes[2], strokeWidth = 3.dp.toPx())
            nodes.forEach { drawCircle(Cyan, 5.dp.toPx(), it) }
        }
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
@OptIn(ExperimentalLayoutApi::class)
private fun SessionChoiceStrip(selected: String, choose: (String) -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        StudioButton(
            onClick = { choose("Schedule") },
            kind = if (selected == "Schedule") StudioButtonKind.Primary else StudioButtonKind.Secondary,
        ) { Text("Schedule", color = if (selected == "Schedule") Ink else Color.White, fontWeight = FontWeight.Bold) }
        SubBrandIconButton(SubBrand.Live, "Leviathan Live", { choose("Leviathan Live") }, selected == "Leviathan Live")
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun MoreChoiceStrip(selected: String, agentName: String, choose: (String) -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        listOf("Reports", "Sharing", "People").forEach { option ->
            StudioButton(onClick = { choose(option) }, kind = if (option == selected) StudioButtonKind.Primary else StudioButtonKind.Secondary) {
                Text(option, color = if (option == selected) Ink else Color.White, fontWeight = FontWeight.Bold)
            }
        }
        SubBrandIconButton(SubBrand.Crew, "Leviathan Crew", { choose(agentName) }, selected == agentName)
        listOf("Reference", "Sync", "Settings").forEach { option ->
            StudioButton(onClick = { choose(option) }, kind = if (option == selected) StudioButtonKind.Primary else StudioButtonKind.Secondary) {
                Text(option, color = if (option == selected) Ink else Color.White, fontWeight = FontWeight.Bold)
            }
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
    showImage: Boolean = false,
    circularImage: Boolean = false,
    imageFallback: String = "SR",
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
                if (showImage || imageUrl.isNotBlank()) {
                    val imageShape = if (circularImage) CircleShape else RoundedCornerShape(7.dp)
                    val imageModifier = Modifier.size(58.dp).clip(imageShape).background(Ink)
                    if (imageUrl.isNotBlank()) {
                        CachedNetworkImage(
                            imageUrl = imageUrl,
                            contentDescription = title,
                            modifier = imageModifier,
                            contentScale = ContentScale.Crop,
                            fallbackText = imageFallback,
                        )
                    } else {
                        Box(imageModifier, contentAlignment = Alignment.Center) {
                            Text(imageFallback, color = Amber, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }
                    }
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
    fallbackText: String = "SR",
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
            Text(fallbackText, color = Amber, fontSize = 11.sp, fontWeight = FontWeight.Black)
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

internal fun estimatedItemValue(specs: Map<String, String>): Double {
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

@OptIn(ExperimentalLayoutApi::class)
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
    var album by remember { mutableStateOf(original.optString("album")) }
    var releaseYear by remember { mutableStateOf(original.optString("release_year")) }
    var genre by remember { mutableStateOf(original.optString("genre")) }
    var metadataSource by remember { mutableStateOf(original.optString("metadata_source")) }
    var metadataSourceId by remember { mutableStateOf(original.optString("metadata_source_id")) }
    var metadataSourceUri by remember { mutableStateOf(original.optString("metadata_source_uri")) }
    var metadataCheckedUtc by remember { mutableStateOf(original.optString("metadata_last_checked_utc")) }
    var danceability by remember { mutableIntStateOf(if (original.has("danceability") && !original.isNull("danceability")) original.optInt("danceability") else -1) }
    var acousticness by remember { mutableIntStateOf(if (original.has("acousticness") && !original.isNull("acousticness")) original.optInt("acousticness") else -1) }
    var artistMbid by remember { mutableStateOf(original.optString("artist_mbid")) }
    var style by remember { mutableStateOf(original.optString("style")) }
    var tempo by remember { mutableStateOf(original.optString("tempo")) }
    var duration by remember { mutableStateOf(formatDuration(original.optInt("duration_seconds"))) }
    var durationSource by remember { mutableStateOf(original.optString("duration_source")) }
    var durationSourceId by remember { mutableStateOf(original.optString("duration_source_id")) }
    var durationSourceUri by remember { mutableStateOf(original.optString("duration_source_uri")) }
    var durationCheckedUtc by remember { mutableStateOf(original.optString("duration_last_checked_utc")) }
    var signature by remember { mutableStateOf(original.optString("time_signature", "4/4")) }
    var songKey by remember { mutableStateOf(original.optString("song_key")) }
    var starts by remember { mutableStateOf(original.optString("starts_by")) }
    var patchName by remember { mutableStateOf(original.optString("patch_name")) }
    var patchNumber by remember { mutableStateOf(original.optString("patch_number")) }
    var media by remember { mutableStateOf(original.optString("media_ref")) }
    var notes by remember { mutableStateOf(original.optString("notes")) }
    var favorite by remember { mutableStateOf(original.optInt("is_favorite") == 1) }
    var attachmentType by remember { mutableStateOf("Chart") }
    var newAttachments by remember(target.id) { mutableStateOf(emptyList<SongAttachmentInput>()) }
    var writingLyrics by remember(target.id) { mutableStateOf(false) }
    var lyricsName by remember(target.id) { mutableStateOf("Lyrics") }
    var lyricsDraft by remember(target.id) { mutableStateOf(TextFieldValue("")) }
    var preview by remember { mutableStateOf<CachedAttachment?>(null) }
    var metadataResults by remember { mutableStateOf(emptyList<JSONObject>()) }
    var metadataMessage by remember { mutableStateOf("") }
    var metadataBusy by remember { mutableStateOf(false) }
    var lyricsResults by remember { mutableStateOf(emptyList<JSONObject>()) }
    var lyricsMessage by remember { mutableStateOf("") }
    var lyricsBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
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
    fun queueLyrics(item: JSONObject, format: String, content: String, message: String = "Lyrics added. Review the material and save the song.") {
        newAttachments = newAttachments + SongAttachmentInput(
            uri = "",
            displayName = "${item.optString("title").ifBlank { title.ifBlank { "Song" } }} Lyrics",
            attachmentType = "lyrics",
            mimeType = "text/plain",
            sourceType = "text",
            contentText = content,
            contentFormat = format,
            sourceProvider = item.optString("source"),
            sourceRecordId = item.optString("source_id"),
            sourceUri = item.optString("source_uri"),
            sourceRetrievedUtc = item.optString("retrieved_utc"),
            sourceAttribution = item.optString("attribution"),
        )
        lyricsResults = emptyList()
        lyricsMessage = message
    }
    EditorDialog(if (target.id == null) "Add Song" else "Edit Song", close) {
        StudioField("Song title", title) { title = it }
        StudioField("Artist", artist) { artist = it }
        Surface(color = Cyan.copy(alpha = .045f), border = BorderStroke(1.dp, Cyan.copy(alpha = .3f)), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Find Recording Metadata", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Search by title and artist, choose the correct recording, then review the imported values before saving.", color = TextSoft, fontSize = 11.sp)
                StudioButton(
                    onClick = {
                        if (title.isBlank()) {
                            metadataMessage = "Enter a song title first."
                        } else {
                            metadataBusy = true
                            metadataMessage = "Looking for matching recordings..."
                            scope.launch {
                                runCatching { model.searchSongMetadata(title.trim(), artist.trim()) }
                                    .onSuccess { payload ->
                                        val rows = payload.optJSONArray("results") ?: JSONArray()
                                        metadataResults = (0 until rows.length()).map { rows.getJSONObject(it) }
                                        metadataMessage = if (metadataResults.isEmpty()) "No matching recordings were found." else "Choose the recording you want to use."
                                    }
                                    .onFailure { metadataMessage = it.message ?: "The lookup failed." }
                                metadataBusy = false
                            }
                        }
                    },
                    enabled = !metadataBusy,
                    modifier = Modifier.fillMaxWidth(),
                    kind = StudioButtonKind.Secondary,
                ) { Text(if (metadataBusy) "Searching..." else "Search GetSongBPM", color = Color.White, fontWeight = FontWeight.Bold) }
                TextButton(onClick = { uriHandler.openUri("https://getsongbpm.com") }) {
                    Text("Song data by GetSongBPM", color = TextSoft, fontSize = 11.sp)
                }
                if (metadataMessage.isNotBlank()) Text(metadataMessage, color = if (metadataResults.isEmpty() && !metadataBusy) Amber else TextSoft, fontSize = 12.sp)
                metadataResults.forEach { item ->
                    Surface(color = PanelRaised, border = BorderStroke(1.dp, Amber.copy(alpha = .25f)), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(listOf(item.optString("title"), item.optString("artist")).filter(String::isNotBlank).joinToString(" - "), color = Color.White, fontWeight = FontWeight.Bold)
                            Text(listOf(item.optString("album"), item.optString("release_year"), item.optString("genre"), item.optString("tempo").takeIf(String::isNotBlank)?.let { "$it BPM" }.orEmpty(), item.optString("song_key"), item.optString("time_signature")).filter(String::isNotBlank).joinToString(" | "), color = TextSoft, fontSize = 11.sp)
                            StudioButton(onClick = {
                                title = item.optString("title").ifBlank { title }; artist = item.optString("artist").ifBlank { artist }
                                album = item.optString("album"); releaseYear = item.optString("release_year"); genre = item.optString("genre")
                                tempo = item.optString("tempo"); signature = item.optString("time_signature").ifBlank { signature }; songKey = item.optString("song_key")
                                metadataSource = item.optString("source"); metadataSourceId = item.optString("source_id"); metadataSourceUri = item.optString("source_uri")
                                metadataCheckedUtc = item.optString("checked_utc"); danceability = if (item.isNull("danceability")) -1 else item.optInt("danceability")
                                acousticness = if (item.isNull("acousticness")) -1 else item.optInt("acousticness"); artistMbid = item.optString("artist_mbid")
                                metadataResults = emptyList(); metadataMessage = "Recording selected. Review or change any field, then save."
                            }, modifier = Modifier.fillMaxWidth()) { Text("Use This Recording", color = Ink, fontWeight = FontWeight.Black) }
                        }
                    }
                }
            }
        }
        StudioField("Album", album) { album = it }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) { StudioField("Release year", releaseYear, dictation = false) { releaseYear = it.filter(Char::isDigit).take(4) } }
            Box(Modifier.weight(2f)) { StudioField("Genre", genre) { genre = it } }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth < 520.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StudioField("Style", style) { style = it }
                    StudioField("Key (C, F#, Bb, Am)", songKey, dictation = false) { songKey = it.take(40) }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.weight(1f)) { StudioField("Tempo", tempo, dictation = false) { tempo = it.filter(Char::isDigit).take(3) } }
                        Box(Modifier.weight(1f)) { StudioField("Time signature", signature, dictation = false) { signature = it.take(12) } }
                    }
                    StudioField("Song length", duration, dictation = false) { duration = it.filter { char -> char.isDigit() || char == ':' }.take(8); durationSource = ""; durationSourceId = ""; durationSourceUri = ""; durationCheckedUtc = "" }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) { StudioField("Style", style) { style = it } }
                    Box(Modifier.weight(1f)) { StudioField("Key", songKey, dictation = false) { songKey = it.take(40) } }
                    Box(Modifier.weight(1f)) { StudioField("Tempo", tempo, dictation = false) { tempo = it.filter(Char::isDigit).take(3) } }
                    Box(Modifier.weight(1f)) { StudioField("Time signature", signature, dictation = false) { signature = it.take(12) } }
                    Box(Modifier.weight(1f)) { StudioField("Song length", duration, dictation = false) { duration = it.filter { char -> char.isDigit() || char == ':' }.take(8); durationSource = ""; durationSourceId = ""; durationSourceUri = ""; durationCheckedUtc = "" } }
                }
            }
        }
        Text("Key notation: use # for sharp, b for flat; no accidental means natural. Musical symbols are also accepted.", color = TextSoft, fontSize = 10.sp)
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
        Surface(color = Cyan.copy(alpha = .045f), border = BorderStroke(1.dp, Cyan.copy(alpha = .3f)), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Find Lyrics", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Search LRCLIB, preview the matches, then import plain, synchronized, or ChordPro lyrics.", color = TextSoft, fontSize = 11.sp)
                StudioButton(
                    onClick = {
                        if (title.isBlank()) lyricsMessage = "Enter a song title first."
                        else {
                            lyricsBusy = true
                            lyricsMessage = "Looking for matching lyrics..."
                            scope.launch {
                                runCatching { model.searchSongLyrics(title.trim(), artist.trim(), album.trim()) }
                                    .onSuccess { payload ->
                                        val rows = payload.optJSONArray("results") ?: JSONArray()
                                        lyricsResults = (0 until rows.length()).map { rows.getJSONObject(it) }
                                        lyricsMessage = if (lyricsResults.isEmpty()) "No matching lyrics were found." else "Choose the correct version."
                                    }
                                    .onFailure { lyricsMessage = it.message ?: "The lyrics search failed." }
                                lyricsBusy = false
                            }
                        }
                    },
                    enabled = !lyricsBusy,
                    modifier = Modifier.fillMaxWidth(),
                    kind = StudioButtonKind.Secondary,
                ) { Text(if (lyricsBusy) "Searching..." else "Search LRCLIB", color = Color.White, fontWeight = FontWeight.Bold) }
                TextButton(onClick = { uriHandler.openUri("https://lrclib.net") }) { Text("Lyrics provided by LRCLIB", color = TextSoft, fontSize = 11.sp) }
                if (lyricsMessage.isNotBlank()) Text(lyricsMessage, color = TextSoft, fontSize = 12.sp)
                lyricsResults.forEach { item ->
                    var expanded by remember(item.optString("source_id")) { mutableStateOf(false) }
                    Surface(color = PanelRaised, border = BorderStroke(1.dp, Amber.copy(alpha = .25f)), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(listOf(item.optString("title"), item.optString("artist")).filter(String::isNotBlank).joinToString(" - "), color = Color.White, fontWeight = FontWeight.Bold)
                            Text(listOf(item.optString("album"), item.optString("duration_label"), if (item.optBoolean("instrumental")) "Instrumental" else "").filter(String::isNotBlank).joinToString(" | "), color = TextSoft, fontSize = 11.sp)
                            if (item.optString("plain_lyrics").isNotBlank()) {
                                Text(item.optString("plain_lyrics"), color = Color.White, fontSize = 12.sp, maxLines = if (expanded) Int.MAX_VALUE else 5)
                                TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Collapse Preview" else "Preview Lyrics", color = Cyan) }
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (item.optInt("duration_seconds") > 0 && duration.isBlank()) {
                                    StudioButton(onClick = {
                                        if (duration.isBlank()) {
                                            duration = item.optString("duration_label")
                                            durationSource = item.optString("source")
                                            durationSourceId = item.optString("source_id")
                                            durationSourceUri = item.optString("source_uri")
                                            durationCheckedUtc = item.optString("retrieved_utc")
                                            lyricsMessage = "Duration ${item.optString("duration_label")} added. Review and save the song."
                                        }
                                    }, kind = StudioButtonKind.Secondary) { Text("Use Duration: ${item.optString("duration_label")}", color = Color.White, fontWeight = FontWeight.Bold) }
                                }
                                if (item.optString("plain_lyrics").isNotBlank()) {
                                    StudioButton(onClick = { queueLyrics(item, "plain", item.optString("plain_lyrics")) }, kind = StudioButtonKind.Secondary) { Text("Plain", color = Color.White, fontWeight = FontWeight.Bold) }
                                    StudioButton(onClick = { queueLyrics(item, "chordpro", item.optString("chordpro")) }, kind = StudioButtonKind.Secondary) { Text("ChordPro", color = Color.White, fontWeight = FontWeight.Bold) }
                                    StudioButton(onClick = {
                                        lyricsBusy = true
                                        lyricsMessage = "Crew is identifying song sections..."
                                        scope.launch {
                                            runCatching { model.structureSongLyrics(item.optString("title"), item.optString("artist"), item.optString("album"), item.optString("plain_lyrics")) }
                                                .onSuccess { payload -> queueLyrics(item, "chordpro", payload.optString("content"), payload.optString("message")) }
                                                .onFailure { lyricsMessage = it.message ?: "Crew could not structure these lyrics." }
                                            lyricsBusy = false
                                        }
                                    }, enabled = !lyricsBusy) { Text("Crew Structure", color = Ink, fontWeight = FontWeight.Black) }
                                }
                                if (item.optString("synced_lyrics").isNotBlank()) StudioButton(onClick = { queueLyrics(item, "lrc", item.optString("synced_lyrics")) }, kind = StudioButtonKind.Secondary) { Text("Synced", color = Color.White, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }
        }
        ChoiceStrip(listOf("Chart", "Lyrics", "Tab", "Sheet Music", "Other"), attachmentType) { attachmentType = it }
        StudioButton(
            onClick = { attachmentPicker.launch(arrayOf("application/pdf", "image/*", "text/plain", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) },
            modifier = Modifier.fillMaxWidth(),
            kind = StudioButtonKind.Secondary,
        ) { Text("Add $attachmentType File", color = Color.White, fontWeight = FontWeight.Bold) }
        StudioButton(
            onClick = { writingLyrics = !writingLyrics },
            modifier = Modifier.fillMaxWidth(),
            kind = StudioButtonKind.Secondary,
        ) { Text(if (writingLyrics) "Close Lyrics Editor" else "Write Lyrics / ChordPro", color = Color.White, fontWeight = FontWeight.Bold) }
        if (writingLyrics) {
            StudioField("Material name", lyricsName, dictation = false) { lyricsName = it }
            ChordProEditor(lyricsDraft) { lyricsDraft = it }
            StudioButton(
                onClick = {
                    if (lyricsDraft.text.isNotBlank()) {
                        newAttachments = newAttachments + SongAttachmentInput(
                            uri = "",
                            displayName = lyricsName.ifBlank { "Lyrics" },
                            attachmentType = "lyrics",
                            mimeType = "text/plain",
                            sourceType = "text",
                            contentText = lyricsDraft.text,
                        )
                        lyricsDraft = TextFieldValue("")
                        lyricsName = "Lyrics"
                        writingLyrics = false
                    }
                },
                enabled = lyricsDraft.text.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Add Written Lyrics", color = Ink, fontWeight = FontWeight.Black) }
        }
        existingAttachments.forEach { record ->
            val attachment = recordJson(record)
            val cached = cacheById[record.entityId]
            if (attachment.optString("source_type") == "text") {
                TextMaterialEditor(record, model)
            } else {
                AttachmentEditorRow(
                    label = attachmentLabel(attachment),
                    detail = if (cached?.status == "ready") "Available offline" else "Will download when connected",
                    view = cached?.takeIf { it.status == "ready" && !it.localPath.isNullOrBlank() }?.let { { preview = it } },
                )
            }
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
                    .put("album", album.trim()).put("release_year", releaseYear.toIntOrNull()).put("genre", genre.trim())
                    .put("metadata_source", metadataSource).put("metadata_source_id", metadataSourceId).put("metadata_source_uri", metadataSourceUri)
                    .put("metadata_last_checked_utc", metadataCheckedUtc).put("danceability", danceability.takeIf { it >= 0 }).put("acousticness", acousticness.takeIf { it >= 0 }).put("artist_mbid", artistMbid)
                    .put("tempo", tempo.trim()).put("duration_seconds", parseDuration(duration))
                    .put("duration_source", durationSource).put("duration_source_id", durationSourceId).put("duration_source_uri", durationSourceUri).put("duration_last_checked_utc", durationCheckedUtc)
                    .put("time_signature", signature.trim()).put("song_key", normalizeSongKey(songKey)).put("starts_by", starts.trim())
                    .put("patch_name", patchName.trim()).put("patch_number", patchNumber.trim())
                    .put("media_ref", media.trim()).put("notes", notes.trim()).put("is_favorite", if (favorite) 1 else 0), newAttachments, close)
            },
            delete = target.id?.let { id -> { model.deleteSong(id, close) } },
        )
    }
    preview?.let { AttachmentPreviewDialog(it) { preview = null } }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ChordProEditor(value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Lyrics / ChordPro", color = TextSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(
                "Verse" to ("{start_of_verse}\n" to "\n{end_of_verse}"),
                "Chorus" to ("{start_of_chorus}\n" to "\n{end_of_chorus}"),
                "Bridge" to ("{start_of_bridge}\n" to "\n{end_of_bridge}"),
                "Chord" to ("[" to "]"), "Bold" to ("**" to "**"), "Italic" to ("*" to "*"),
            ).forEach { (label, markers) ->
                TextButton(onClick = { onValueChange(wrapTextSelection(value, markers.first, markers.second)) }) {
                    Text(label, color = Cyan, fontWeight = FontWeight.Bold)
                }
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = 280.dp),
            placeholder = { Text("{start_of_verse}\nLyrics with optional [C]chords...\n{end_of_verse}") },
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace, color = Color.White),
        )
    }
}

private fun wrapTextSelection(value: TextFieldValue, prefix: String, suffix: String): TextFieldValue {
    val start = value.selection.min.coerceIn(0, value.text.length)
    val end = value.selection.max.coerceIn(start, value.text.length)
    val replacement = prefix + value.text.substring(start, end) + suffix
    val updated = value.text.replaceRange(start, end, replacement)
    return TextFieldValue(updated, androidx.compose.ui.text.TextRange(start + prefix.length, start + prefix.length + end - start))
}

@Composable
private fun TextMaterialEditor(record: CachedRecord, model: StudioRackViewModel) {
    val original = recordJson(record)
    val format = original.optString("content_format", "chordpro").ifBlank { "chordpro" }
    var expanded by remember(record.entityId) { mutableStateOf(false) }
    var name by remember(record.json) { mutableStateOf(original.optString("display_name", "Lyrics")) }
    var content by remember(record.json) { mutableStateOf(TextFieldValue(original.optString("content_text"))) }
    Surface(color = Panel, border = BorderStroke(1.dp, Amber.copy(alpha = .28f)), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(name.ifBlank { "Lyrics" }, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("${format.uppercase()} material - available offline", color = TextSoft, fontSize = 11.sp)
                    original.optString("source_attribution").takeIf(String::isNotBlank)?.let { Text(it, color = TextSoft, fontSize = 10.sp) }
                }
                TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Close" else "Edit", color = Cyan, fontWeight = FontWeight.Bold) }
            }
            if (expanded) {
                StudioField("Material name", name, dictation = false) { name = it }
                ChordProEditor(content) { content = it }
                StudioButton(
                    onClick = {
                        model.saveSongAttachment(record.entityId, JSONObject(original.toString())
                            .put("display_name", name.trim()).put("source_type", "text")
                            .put("content_format", format).put("content_text", content.text)
                            .put("file_ref", "text://chordpro"))
                        expanded = false
                    },
                    enabled = content.text.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Save Lyrics", color = Ink, fontWeight = FontWeight.Black) }
                TextButton(onClick = { model.deleteSongAttachment(record.entityId) }) {
                    Text("Delete Lyrics Material", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
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
    val kits by model.kits.collectAsState()
    val eventKits by model.eventKits.collectAsState()
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
    val relationshipSourceId = original.optString("_copy_source_id").ifBlank { target.id.orEmpty() }
    var selectedKits by remember(relationshipSourceId, eventKits) { mutableStateOf(eventKits.filter { recordJson(it).optString("event_id") == relationshipSourceId }.mapTo(mutableSetOf()) { recordJson(it).optString("kit_id") }) }
    var selectedEnsembles by remember(relationshipSourceId, eventEnsembles) { mutableStateOf(eventEnsembles.filter { recordJson(it).optString("event_id") == relationshipSourceId }.mapTo(mutableSetOf()) { recordJson(it).optString("ensemble_id") }) }
    var selectedContacts by remember(relationshipSourceId, eventContacts) { mutableStateOf(eventContacts.filter { recordJson(it).optString("event_id") == relationshipSourceId }.mapTo(mutableSetOf()) { recordJson(it).optString("contact_id") }) }
    var kitQuery by remember(relationshipSourceId) { mutableStateOf("") }
    var venueQuery by remember(target.id) { mutableStateOf("") }
    var setListQuery by remember(target.id) { mutableStateOf("") }
    var ensembleQuery by remember(target.id) { mutableStateOf("") }
    var contactQuery by remember(target.id) { mutableStateOf("") }
    EditorDialog(if (original.optString("_copy_source_id").isNotBlank()) "Copy Scheduled Event" else if (target.id == null) "Add Scheduled Event" else "Edit Scheduled Event", close) {
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
        StudioField("Find a venue", venueQuery) { venueQuery = it }
        val visibleVenues = venues.filter { recordJson(it).optString("name").contains(venueQuery, ignoreCase = true) }
        ChoiceStrip(listOf("None") + visibleVenues.map { recordJson(it).optString("name") }, venues.firstOrNull { it.entityId == venueId }?.let { recordJson(it).optString("name") } ?: "None") { picked ->
            venueId = venues.firstOrNull { recordJson(it).optString("name") == picked }?.entityId.orEmpty()
        }
        StudioField("Room / Stage / Location Details", location) { location = it }
        Text("Set list", color = TextSoft, fontWeight = FontWeight.Bold)
        StudioField("Find a set list", setListQuery) { setListQuery = it }
        val visibleSetLists = setLists.filter { recordJson(it).optString("name").contains(setListQuery, ignoreCase = true) }
        ChoiceStrip(listOf("None") + visibleSetLists.map { recordJson(it).optString("name") }, setLists.firstOrNull { it.entityId == setListId }?.let { recordJson(it).optString("name") } ?: "None") { picked ->
            setListId = setLists.firstOrNull { recordJson(it).optString("name") == picked }?.entityId.orEmpty()
        }
        if (kits.isNotEmpty()) {
            Text("Kits / Gear To Take", color = TextSoft, fontWeight = FontWeight.Bold)
            StudioField("Find a kit", kitQuery) { kitQuery = it }
            kits.filter { supportingJson(it).optString("name").contains(kitQuery, ignoreCase = true) }.forEach { kit ->
                val checked = kit.entityId in selectedKits
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked, { enabled -> selectedKits = selectedKits.toMutableSet().apply { if (enabled) add(kit.entityId) else remove(kit.entityId) } })
                    Text(supportingJson(kit).optString("name"), color = Color.White)
                }
            }
        }
        StudioField("Notes", notes, singleLine = false) { notes = it }
        if (ensembles.isNotEmpty()) {
            Text("Bands / Groups", color = TextSoft, fontWeight = FontWeight.Bold)
            StudioField("Find a band or group", ensembleQuery) { ensembleQuery = it }
            ensembles.filter { recordJson(it).optString("name").contains(ensembleQuery, ignoreCase = true) }.forEach { ensemble ->
                val checked = ensemble.entityId in selectedEnsembles
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked, { enabled -> selectedEnsembles = selectedEnsembles.toMutableSet().apply { if (enabled) add(ensemble.entityId) else remove(ensemble.entityId) } })
                    Text(recordJson(ensemble).optString("name"), color = Color.White)
                }
            }
        }
        if (contacts.isNotEmpty()) {
            Text("People / Contacts", color = TextSoft, fontWeight = FontWeight.Bold)
            StudioField("Find a person", contactQuery) { contactQuery = it }
            contacts.filter { contact ->
                val data = recordJson(contact)
                listOf(data.optString("display_name"), data.optString("organization_name"), data.optString("job_title"), data.optString("phone"), data.optString("email"))
                    .joinToString(" ").contains(contactQuery, ignoreCase = true)
            }.forEach { contact ->
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
                    .put("reminder_lead_unit", unit), selectedKits, selectedEnsembles, selectedContacts, close)
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
@OptIn(ExperimentalLayoutApi::class)
private fun EventCard(
    event: JSONObject,
    readiness: PacketReadiness,
    open: () -> Unit,
    edit: (() -> Unit)? = null,
    copy: (() -> Unit)? = null,
    host: (() -> Unit)? = null,
    peopleCount: Int = 0,
    people: (() -> Unit)? = null,
) {
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
                if (event.optString("set_list_id").isNotBlank() || edit != null || copy != null || host != null || people != null) FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (event.optString("set_list_id").isNotBlank()) SubBrandIconButton(SubBrand.Live, "Open in $liveModeName", open)
                    if (people != null) EventToolIconButton(Icons.Rounded.Groups, "People", people, peopleCount)
                    if (host != null) TextButton(onClick = host) { Text("Host", color = Cyan, fontWeight = FontWeight.Bold) }
                    if (edit != null) EventToolIconButton(Icons.Rounded.Edit, "Edit event", edit)
                    if (copy != null) EventToolIconButton(Icons.Rounded.ContentCopy, "Copy event", copy)
                }
            }
        }
    }
}

@Composable
private fun EventToolIconButton(icon: ImageVector, description: String, onClick: () -> Unit, badgeCount: Int = 0) {
    Box(Modifier.size(width = 54.dp, height = 50.dp)) {
        Surface(
            modifier = Modifier.size(44.dp).align(Alignment.BottomStart).clickable(onClick = onClick),
            color = Color(0xFF303646),
            contentColor = Color.White,
            shape = CircleShape,
            border = BorderStroke(1.dp, Color(0xFF596174)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = description, modifier = Modifier.size(21.dp))
            }
        }
        if (badgeCount > 0) Surface(
            modifier = Modifier.widthIn(min = 36.dp).height(22.dp).align(Alignment.TopEnd),
            color = Amber,
            contentColor = Ink,
            shape = RoundedCornerShape(50),
            border = BorderStroke(2.dp, Ink),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(badgeCount.toString(), fontSize = 10.sp, fontWeight = FontWeight.Black, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(horizontal = 4.dp))
            }
        }
    }
}

@Composable
private fun SubBrandIconButton(kind: SubBrand, description: String, onClick: () -> Unit, selected: Boolean = false) {
    val badgeColor = if (kind == SubBrand.Live) Amber else Cyan
    Box(Modifier.size(width = 54.dp, height = 50.dp)) {
        Surface(
            modifier = Modifier.size(44.dp).align(Alignment.BottomStart).clickable(onClick = onClick),
            color = if (selected) Color(0xFF3A3323) else Color(0xFF303646),
            contentColor = Color.White,
            shape = CircleShape,
            border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) badgeColor else Color(0xFF596174)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.brand_logo), description, Modifier.size(38.dp))
                Surface(
                    modifier = Modifier.size(19.dp).align(Alignment.BottomEnd),
                    color = badgeColor,
                    shape = CircleShape,
                    border = BorderStroke(2.dp, Ink),
                ) {
                    Canvas(Modifier.fillMaxSize().padding(3.dp)) {
                        if (kind == SubBrand.Live) {
                            val points = listOf(
                                Offset(0f, size.height * .58f), Offset(size.width * .22f, size.height * .58f),
                                Offset(size.width * .36f, size.height * .18f), Offset(size.width * .52f, size.height * .86f),
                                Offset(size.width * .68f, size.height * .34f), Offset(size.width, size.height * .58f),
                            )
                            points.zipWithNext().forEach { (start, end) -> drawLine(Ink, start, end, strokeWidth = 1.7.dp.toPx()) }
                        } else {
                            val nodes = listOf(Offset(0f, size.height), Offset(size.width * .48f, 0f), Offset(size.width, size.height))
                            drawLine(Ink, nodes[0], nodes[1], strokeWidth = 1.5.dp.toPx())
                            drawLine(Ink, nodes[1], nodes[2], strokeWidth = 1.5.dp.toPx())
                            nodes.forEach { drawCircle(Ink, 1.7.dp.toPx(), it) }
                        }
                    }
                }
            }
        }
    }
}

private data class EventPerson(
    val id: String,
    val contact: JSONObject,
    val roles: List<String>,
    val contexts: List<String>,
)

private data class EventPersonAccumulator(
    val id: String,
    val contact: JSONObject,
    val roles: LinkedHashSet<String> = linkedSetOf(),
    val contexts: LinkedHashSet<String> = linkedSetOf(),
)

private fun resolveEventPeople(
    event: JSONObject,
    contacts: List<CachedRecord>,
    ensembles: List<CachedRecord>,
    eventEnsembles: List<CachedRecord>,
    eventContacts: List<CachedRecord>,
    ensembleContacts: List<CachedRecord>,
    venueContacts: List<CachedRecord>,
): List<EventPerson> {
    val eventId = event.optString("id")
    val contactsById = contacts.associateBy(CachedRecord::entityId)
    val ensembleNames = ensembles.associate { it.entityId to recordJson(it).optString("name", "Band / Group") }
    val selectedEnsembles = eventEnsembles.map(::recordJson)
        .filter { it.optString("event_id") == eventId }
        .mapTo(linkedSetOf()) { it.optString("ensemble_id") }
    val people = linkedMapOf<String, EventPersonAccumulator>()
    fun add(contactId: String, role: String, context: String) {
        val record = contactsById[contactId] ?: return
        val person = people.getOrPut(contactId) { EventPersonAccumulator(contactId, recordJson(record)) }
        role.takeIf(String::isNotBlank)?.let(person.roles::add)
        context.takeIf(String::isNotBlank)?.let(person.contexts::add)
    }
    eventContacts.map(::recordJson).filter { it.optString("event_id") == eventId }.forEach {
        add(it.optString("contact_id"), it.optString("relationship_role", "Participant").ifBlank { "Participant" }, "Event")
    }
    ensembleContacts.map(::recordJson).filter { it.optString("ensemble_id") in selectedEnsembles }.forEach {
        val ensembleId = it.optString("ensemble_id")
        add(it.optString("contact_id"), it.optString("relationship_role", "Member").ifBlank { "Member" }, ensembleNames[ensembleId].orEmpty())
    }
    val venueId = event.optString("venue_id")
    venueContacts.map(::recordJson).filter { venueId.isNotBlank() && it.optString("venue_id") == venueId }.forEach {
        add(it.optString("contact_id"), it.optString("relationship_role", "Venue Contact").ifBlank { "Venue Contact" }, "Venue")
    }
    return people.values.map { EventPerson(it.id, it.contact, it.roles.toList(), it.contexts.toList()) }
        .sortedBy { it.contact.optString("display_name").lowercase() }
}

private fun resolveEventGroupNames(
    event: JSONObject,
    ensembles: List<CachedRecord>,
    eventEnsembles: List<CachedRecord>,
): List<String> {
    val eventId = event.optString("id")
    val names = ensembles.associate { it.entityId to recordJson(it).optString("name") }
    return eventEnsembles.map(::recordJson)
        .filter { it.optString("event_id") == eventId }
        .mapNotNull { names[it.optString("ensemble_id")]?.takeIf(String::isNotBlank) }
        .distinct()
        .sortedBy(String::lowercase)
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun EventPeopleDialog(
    event: JSONObject,
    people: List<EventPerson>,
    contactMethods: List<CachedRecord>,
    groupNames: List<String>,
    close: () -> Unit,
) {
    val context = LocalContext.current
    val phones = people.map { it.contact.optString("phone") }.filter(String::isNotBlank).distinct()
    val emails = people.map { it.contact.optString("email") }.filter(String::isNotBlank).distinct()
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(14.dp).statusBarsPadding().navigationBarsPadding(),
            color = Panel,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFF343B4D)),
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("EVENT ROSTER", color = Amber, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Text(event.optString("title", "Scheduled Event"), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("${people.size} ${if (people.size == 1) "person" else "people"} associated with this event", color = TextSoft, fontSize = 12.sp)
                        if (groupNames.isNotEmpty()) Text("Bands / Groups: ${groupNames.joinToString(", ")}", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    StudioButton(onClick = close, kind = StudioButtonKind.Secondary) { Text("Close", color = Color.White) }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (phones.isNotEmpty()) StudioButton(onClick = { openGroupContactLink(context, "smsto", phones) }) { Text("Text Everyone", color = Ink, fontWeight = FontWeight.Bold) }
                    if (emails.isNotEmpty()) StudioButton(onClick = { openGroupContactLink(context, "mailto", emails) }, kind = StudioButtonKind.Secondary) { Text("Email Everyone", color = Color.White, fontWeight = FontWeight.Bold) }
                }
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (people.isEmpty()) item { EmptyCard("A band or group is selected, but it has no connected members yet.") }
                    items(people, key = EventPerson::id) { person ->
                        val label = (person.roles + person.contexts).filter(String::isNotBlank).joinToString(" / ")
                        DirectoryContactRow(
                            person.contact,
                            label,
                            context,
                            contactMethods.filter { recordJson(it).optString("contact_id") == person.id }.map(::recordJson),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalLiveDialog(model: StudioRackViewModel, suggestedEvent: JSONObject?, close: () -> Unit) {
    val state by model.localLive.collectAsState()
    val peers by model.nearbyLiveSessions.collectAsState()
    var code by remember(state.role) { mutableStateOf("") }
    var manualAddress by remember(state.role) { mutableStateOf("") }
    Dialog(onDismissRequest = close) {
        Surface(
            color = Panel,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0x6642D9FF)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.WifiTethering, contentDescription = null, tint = Cyan, modifier = Modifier.size(34.dp))
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text("LOCAL LIVE NETWORK", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Text("Play together without internet", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = close) { Text("Close", color = TextSoft) }
                }
                when (state.role) {
                    LocalLiveRole.HOST -> {
                        Text("HOSTING", color = Color(0xFF58E99B), fontWeight = FontWeight.Black)
                        Text(state.sessionName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Session code", color = TextSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(state.code, color = Amber, fontSize = 36.sp, fontWeight = FontWeight.Black)
                        DetailLine("Local address", state.address)
                        DetailLine("Connected devices", state.peerCount.toString())
                        Text("Keep this screen awake and connected to the same Wi-Fi or hotspot. Internet access is not required.", color = TextSoft, fontSize = 12.sp)
                        StudioButton(onClick = model::leaveLocalLive, modifier = Modifier.fillMaxWidth(), kind = StudioButtonKind.Danger) {
                            Text("Stop Hosting", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    LocalLiveRole.GUEST -> {
                        Text(if (state.connected) "LOCAL LIVE CONNECTED" else "CONNECTING TO HOST", color = if (state.connected) Color(0xFF58E99B) else Amber, fontWeight = FontWeight.Black)
                        Text(state.sessionName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        DetailLine("Host", state.address)
                        state.error?.let { Text(it, color = Color(0xFFFF7A82), fontSize = 12.sp) }
                        Text("Set-list changes from the host are being written into this device's offline database.", color = TextSoft, fontSize = 12.sp)
                        StudioButton(onClick = model::leaveLocalLive, modifier = Modifier.fillMaxWidth(), kind = StudioButtonKind.Danger) {
                            Text("Leave Local Session", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    LocalLiveRole.NONE -> {
                        suggestedEvent?.takeIf { it.optString("set_list_id").isNotBlank() }?.let { event ->
                            Text("Host this session", color = Color.White, fontWeight = FontWeight.Bold)
                            Text(event.optString("title", "Scheduled session"), color = TextSoft)
                            StudioButton(
                                onClick = { model.hostLocalLive(event.optString("id"), event.optString("title", "Local Live Session")) },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("I'm the Host", color = Ink, fontWeight = FontWeight.Black) }
                        }
                        Text("Join a nearby host", color = Color.White, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it.filter(Char::isDigit).take(6) },
                            label = { Text("Six-digit session code") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        peers.forEach { peer ->
                            StudioButton(onClick = { model.joinLocalLive(peer, code) }, enabled = code.length == 6, modifier = Modifier.fillMaxWidth(), kind = StudioButtonKind.Secondary) {
                                Text("Join ${peer.name.removePrefix("Leviathan - ")}", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (peers.isEmpty()) Text("Looking for Leviathan Live hosts on this Wi-Fi...", color = TextSoft, fontSize = 12.sp)
                        Text("Manual connection", color = TextSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = manualAddress,
                            onValueChange = { manualAddress = it.trim() },
                            label = { Text("Host address, including port") },
                            placeholder = { Text("192.168.1.25:54321") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        StudioButton(
                            onClick = {
                                val separator = manualAddress.lastIndexOf(':')
                                if (separator > 0) {
                                    val port = manualAddress.substring(separator + 1).toIntOrNull()
                                    if (port != null) model.joinLocalLive(LocalLivePeer("Manual local session", manualAddress.substring(0, separator), port), code)
                                }
                            },
                            enabled = code.length == 6 && manualAddress.contains(':'),
                            modifier = Modifier.fillMaxWidth(),
                            kind = StudioButtonKind.Secondary,
                        ) { Text("Connect by Address", color = Color.White, fontWeight = FontWeight.Bold) }
                    }
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
    val localLive by model.localLive.collectAsState()
    val settings = remember(syncState?.performanceSettingsJson) {
        PerformanceSettings.fromJson(syncState?.performanceSettingsJson ?: "{}")
    }
    val metronome = remember { NativeMetronome() }
    val listState = rememberLazyListState()
    val event = remember(events, eventId) { events.firstOrNull { it.entityId == eventId }?.let(::recordJson) ?: JSONObject() }
    val setListId = event.optString("set_list_id")
    val setListRecord = remember(setLists, setListId) { setLists.firstOrNull { it.entityId == setListId } }
    val setList = remember(setListRecord) { setListRecord?.let(::recordJson) }
    val songMap = remember(songs) { songs.associate { it.entityId to recordJson(it) } }
    val sectionRows = remember(sections, setListId) { sections.map(::recordJson).filter { it.optString("set_list_id") == setListId }.sortedBy { it.optInt("position") } }
    val entryRows = remember(entries, setListId) { entries.map(::recordJson).filter { it.optString("set_list_id") == setListId }.groupBy { it.optString("section_id") } }
    val attachmentsBySong = remember(attachments) { attachments.map(::recordJson).groupBy { it.optString("song_id") } }
    val cacheById = remember(cachedAttachments) { cachedAttachments.associateBy(CachedAttachment::attachmentId) }
    val rawPerformanceSongs = remember(sectionRows, entryRows, songMap, attachmentsBySong, cacheById, settings.attachmentPreferences) {
        sectionRows.flatMap { section ->
            entryRows[section.optString("id")].orEmpty().sortedBy { it.optInt("position") }.map { entry ->
                val song = songMap[entry.optString("song_id")]
                val attachment = selectPerformanceAttachment(entry, attachmentsBySong[entry.optString("song_id")].orEmpty(), settings.attachmentPreferences)
                GigSong(section.optString("name", "Set"), entry, song, attachment, attachment?.optString("id")?.let(cacheById::get))
            }
        }
    }
    val performanceSongs = remember(rawPerformanceSongs) {
        val groups = rawPerformanceSongs.mapNotNull { item -> item.entry.performanceGroupOrNull()?.let { it.id to item } }.groupBy({ it.first }, { it.second })
        rawPerformanceSongs.map { item ->
            val performanceGroup = item.entry.performanceGroupOrNull()
            val members = performanceGroup?.let { groups[it.id] }.orEmpty()
            item.copy(
                performanceGroup = performanceGroup,
                performanceGroupPosition = members.indexOfFirst { it.entry.optString("id") == item.entry.optString("id") }.takeIf { it >= 0 }?.plus(1) ?: 0,
                performanceGroupCount = members.size,
            )
        }
    }
    var currentSong by remember(eventId) { mutableIntStateOf(0) }
    var currentEntryId by remember(eventId) { mutableStateOf("") }
    var detailOpen by remember(eventId) { mutableStateOf(false) }
    var editingLiveSet by remember(eventId) { mutableStateOf(false) }
    var liveRevision by remember(eventId) { mutableStateOf("") }
    var liveConnected by remember(eventId) { mutableStateOf(false) }
    var liveUpdating by remember(eventId) { mutableStateOf(false) }
    var showLocalLive by remember(eventId) { mutableStateOf(false) }
    val gigStartedAt = remember(eventId) { System.currentTimeMillis() }
    LaunchedEffect(performanceSongs.map { it.entry.optString("id") }) {
        if (performanceSongs.isEmpty()) {
            currentSong = 0
            currentEntryId = ""
        } else {
            val anchoredIndex = performanceSongs.indexOfFirst { it.entry.optString("id") == currentEntryId }
            currentSong = if (anchoredIndex >= 0) anchoredIndex else currentSong.coerceIn(0, performanceSongs.lastIndex)
            currentEntryId = performanceSongs[currentSong].entry.optString("id")
        }
    }
    LaunchedEffect(eventId) {
        while (true) {
            liveUpdating = true
            val result = model.refreshLiveEvent(eventId, liveRevision)
            liveRevision = result.revision
            liveConnected = result.connected
            liveUpdating = false
            delay(2_500)
        }
    }
    val activeGigSong = performanceSongs.getOrNull(currentSong)
    val setRemainingSeconds = remember(performanceSongs, currentSong) {
        performanceSongs.drop(currentSong)
            .takeWhile { it.sectionName == activeGigSong?.sectionName }
            .sumOf { it.song?.optInt("duration_seconds") ?: 0 }
    }

    val performanceAttachmentKeys = remember(performanceSongs) {
        performanceSongs.map { item ->
            item.cache?.let { "${it.attachmentId}:${it.sha256}:${it.revision}" }.orEmpty()
        }
    }
    LaunchedEffect(currentSong, detailOpen, performanceAttachmentKeys) {
        // Give the selected chart first access to rendering resources. Neighbor
        // preloads are useful, but they must never delay a performer's tap.
        if (detailOpen) delay(PERFORMANCE_NEIGHBOR_PRELOAD_DELAY_MS)
        withContext(Dispatchers.IO) {
            (if (detailOpen) listOf(currentSong + 1, currentSong - 1) else listOf(currentSong, currentSong + 1))
                .distinct()
                .mapNotNull(performanceSongs::getOrNull)
                .forEach(::preloadPerformanceAttachment)
        }
    }

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
                        currentEntryId = performanceSongs[currentSong].entry.optString("id")
                        if (!detailOpen) listState.animateScrollToItem(gigListItemIndex(currentSong, performanceSongs))
                    }
                }
                null -> Unit
            }
        }
    }

    if (editingLiveSet && setListRecord != null) {
        Dialog(
            onDismissRequest = { editingLiveSet = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            SetListEditor(setListRecord, sections, entries, songs, attachments, model, liveAutosave = true) { editingLiveSet = false }
        }
    }

    if (showLocalLive) LocalLiveDialog(model, event) { showLocalLive = false }

    if (detailOpen && performanceSongs.isNotEmpty()) {
        PerformanceSongScreen(
            item = performanceSongs[currentSong],
            position = currentSong,
            total = performanceSongs.size,
            metronome = metronome,
            previousItem = performanceSongs.getOrNull(currentSong - 1),
            nextItem = performanceSongs.getOrNull(currentSong + 1),
            settings = settings,
            gigStartedAt = gigStartedAt,
            setRemainingSeconds = setRemainingSeconds,
            close = { detailOpen = false },
            previous = {
                currentSong = (currentSong - 1).coerceAtLeast(0)
                currentEntryId = performanceSongs[currentSong].entry.optString("id")
            },
            next = {
                currentSong = (currentSong + 1).coerceAtMost(performanceSongs.lastIndex)
                currentEntryId = performanceSongs[currentSong].entry.optString("id")
            },
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
                    GigIconButton(Icons.Rounded.ArrowBack, "Back to upcoming schedule", back)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text("SET LIST", color = TextSoft, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        Text(setList?.optString("name")?.ifBlank { null } ?: event.optString("title", "Set List"), color = Color.White, fontFamily = FontFamily.Serif, fontSize = 22.sp, maxLines = 1)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        val venueName = venues.firstOrNull { it.entityId == event.optString("venue_id") }?.let { recordJson(it).optString("name") }.orEmpty()
                        Text(listOf(venueName, event.optString("location")).filter(String::isNotBlank).joinToString(" - "), color = TextSoft, fontSize = 11.sp, maxLines = 1)
                        Text(listOf(event.optString("event_date"), event.optString("start_time")).filter(String::isNotBlank).joinToString("  "), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        val localForEvent = localLive.role != LocalLiveRole.NONE && localLive.eventId == eventId
                        LiveConnectionStatus(
                            connected = liveConnected || (localForEvent && localLive.connected),
                            updating = liveUpdating,
                            label = when {
                                localForEvent && localLive.role == LocalLiveRole.HOST -> "LOCAL HOST"
                                localForEvent && localLive.connected -> "LOCAL LIVE"
                                else -> null
                            },
                        )
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                GigIconButton(Icons.Rounded.WifiTethering, "Local live network", onClick = { showLocalLive = true }, active = localLive.role != LocalLiveRole.NONE && localLive.eventId == eventId)
                Spacer(Modifier.width(7.dp))
                GigIconButton(Icons.Rounded.Edit, "Edit live set list", onClick = { editingLiveSet = true }, enabled = setListRecord != null)
                Spacer(Modifier.width(7.dp))
                GigIconButton(Icons.Rounded.ListIcon, "List view", onClick = {}, active = true)
                Spacer(Modifier.width(7.dp))
                GigIconButton(Icons.Rounded.Description, "Chart view", onClick = { if (performanceSongs.isNotEmpty()) detailOpen = true })
            }
        }
        if (settings.showClock || settings.showElapsed || settings.showSetRemaining) {
            item { GigTimeStrip(settings, gigStartedAt, setRemainingSeconds, activeGigSong?.sectionName.orEmpty()) }
        }
        sectionRows.forEach { section ->
            item {
                val sectionSeconds = performanceSongs.filter { it.sectionName == section.optString("name", "Set") }.sumOf { it.song?.optInt("duration_seconds") ?: 0 }
                Column(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(section.optString("name", "Set"), color = Amber, fontFamily = FontFamily.Serif, fontSize = 34.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    if (sectionSeconds > 0) Text("${formatDuration(sectionSeconds)} estimated music time", color = TextSoft, fontSize = 11.sp)
                }
            }
            itemsIndexed(entryRows[section.optString("id")].orEmpty().sortedBy { it.optInt("position") }) { entryIndex, entry ->
                val song = songMap[entry.optString("song_id")]
                val attachment = selectPerformanceAttachment(entry, attachmentsBySong[entry.optString("song_id")].orEmpty(), settings.attachmentPreferences)
                val cached = attachment?.optString("id")?.let(cacheById::get)
                val gigSong = performanceSongs.firstOrNull { it.entry.optString("id") == entry.optString("id") }
                Column {
                    if (gigSong?.performanceGroupPosition == 1 && gigSong.performanceGroup != null) {
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            val tablet = maxWidth >= 600.dp
                            Row(Modifier.fillMaxWidth().padding(start = 18.dp, top = 14.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${gigSong.performanceGroup.type.replaceFirstChar(Char::uppercase)}:", color = Amber, fontSize = if (tablet) 18.sp else 14.sp, fontWeight = FontWeight.Black)
                                Text(gigSong.performanceGroup.name, color = Color.White, fontSize = if (tablet) 24.sp else 19.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    val grouped = gigSong?.performanceGroup != null
                    SongRow(entry, song, attachment, cached, displayPosition = entryIndex + 1, grouped = grouped, modifier = if (grouped) Modifier.padding(start = 32.dp) else Modifier) {
                        currentSong = performanceSongs.indexOfFirst { it.entry.optString("id") == entry.optString("id") }.coerceAtLeast(0)
                        currentEntryId = performanceSongs[currentSong].entry.optString("id")
                        detailOpen = true
                    }
                }
            }
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun LiveConnectionStatus(connected: Boolean, updating: Boolean, label: String? = null) {
    val liveGreen = Color(0xFF58E99B)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(
            Modifier
                .size(14.dp)
                .semantics { stateDescription = if (updating) "Synchronizing" else "Idle" }
                .background(Color(0xFF07090F), CircleShape)
                .border(1.dp, if (connected) liveGreen else TextSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (updating) LiveUpdatingLight()
        }
        Text(
            label ?: if (connected) "LIVE" else "OFFLINE READY",
            color = if (connected) liveGreen else TextSoft,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun LiveUpdatingLight() {
    val pulse = rememberInfiniteTransition(label = "live refresh pulse")
    val lampOn by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 640
                0f at 0
                0f at 285
                1f at 286
                1f at 615
                0f at 616
            },
        ),
        label = "live refresh light",
    )
    Box(
        Modifier
            .size(10.dp)
            .graphicsLayer {
                alpha = lampOn
            }
            .background(Color(0xFFFFC24B), CircleShape)
            .border(1.dp, Color.White, CircleShape),
    )
}

@Composable
private fun SongRow(entry: JSONObject, song: JSONObject?, attachment: JSONObject?, cached: CachedAttachment?, displayPosition: Int, grouped: Boolean, modifier: Modifier = Modifier, openAttachment: () -> Unit) {
    val context = LocalContext.current
    val availableOffline = cached?.status == "ready" && cached.localPath != null
    val mediaLink = normalizedMediaLink(song?.optString("media_ref").orEmpty())
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xE8202635)),
        shape = RoundedCornerShape(6.dp),
        border = if (grouped) BorderStroke(1.dp, Amber.copy(alpha = .42f)) else null,
        modifier = modifier.fillMaxWidth().clickable(onClick = openAttachment),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val compact = maxWidth < 650.dp
                Column {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(displayPosition.toString(), color = Color.White, fontSize = 18.sp, modifier = Modifier.padding(end = 12.dp))
                        Text(song?.optString("title")?.takeIf(String::isNotBlank) ?: entry.optString("manual_title", "Untitled"), color = Amber, fontFamily = FontFamily.Serif, fontSize = 27.sp, modifier = Modifier.weight(1f))
                        if (!compact) GigSongCues(song, compact = false)
                    }
                    if (compact) GigSongCues(song, compact = true)
                }
            }
            Text(song?.optString("artist").orEmpty(), color = TextSoft, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 30.dp))
            val patch = listOf(song?.optString("patch_name"), song?.optString("patch_number")).filterNotNull().filter(String::isNotBlank).joinToString(" / ")
            if (patch.isNotBlank()) Text("Patch: $patch", color = TextSoft, fontSize = 12.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, modifier = Modifier.align(Alignment.End).padding(top = 4.dp))
            if (attachment != null || mediaLink != null) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                    if (attachment != null) {
                        GigIconButton(
                            Icons.Rounded.Description,
                            if (availableOffline) "Open ${attachmentLabel(attachment)}" else "${attachmentLabel(attachment)} unavailable offline",
                            onClick = openAttachment,
                            enabled = availableOffline,
                        )
                    }
                    if (attachment != null && mediaLink != null) Spacer(Modifier.width(7.dp))
                    mediaLink?.let { link -> GigIconButton(Icons.Rounded.Headphones, "Listen", onClick = { openMediaLink(context, link) }) }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GigSongCues(song: JSONObject?, compact: Boolean) {
    val cues = listOf(song?.optString("starts_by"), song?.optString("style")).filterNotNull().filter(String::isNotBlank)
    val facts = listOf(displaySongKey(song?.optString("song_key").orEmpty()), song?.optString("tempo"), song?.optString("time_signature"), formatDuration(song?.optInt("duration_seconds") ?: 0)).filterNotNull().filter(String::isNotBlank)
    if (compact) {
        Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.End) {
            if (cues.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    cues.forEach { Text(it, color = Color.White, fontSize = 12.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic) }
                }
            }
            FlowRow(
                Modifier.fillMaxWidth().padding(top = if (cues.isEmpty()) 0.dp else 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                facts.forEach { GigValueChip(it) }
            }
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            cues.forEach { Text(it, color = Color.White, fontSize = 12.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic) }
            facts.forEach { GigValueChip(it) }
        }
    }
}

@Composable
private fun GigTimeStrip(settings: PerformanceSettings, startedAt: Long, remainingSeconds: Int, sectionName: String) {
    var now by remember(startedAt) { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startedAt) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val elapsedSeconds = ((now - startedAt) / 1_000).coerceAtLeast(0)
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
    previousItem: GigSong?,
    nextItem: GigSong?,
    settings: PerformanceSettings,
    gigStartedAt: Long,
    setRemainingSeconds: Int,
    close: () -> Unit,
    previous: () -> Unit,
    next: () -> Unit,
) {
    val context = LocalContext.current
    val mediaLink = normalizedMediaLink(item.song?.optString("media_ref").orEmpty())
    val path = item.cache?.localPath.orEmpty()
    val attachmentVersion = item.cache?.sha256.orEmpty().ifBlank { item.cache?.revision?.toString().orEmpty() }
    val isPdf = item.cache?.mimeType == "application/pdf" || path.endsWith(".pdf", true)
    var page by remember(path) { mutableIntStateOf(0) }
    var rendered by remember(path, attachmentVersion, page) { mutableStateOf(cachedPerformanceAttachment(path, attachmentVersion, page) ?: AttachmentRender()) }
    LaunchedEffect(path, attachmentVersion, isPdf, page) {
        if (!rendered.complete) rendered = withContext(Dispatchers.IO) { loadPerformanceAttachment(path, attachmentVersion, isPdf, page) }
    }
    val pageCount = rendered.pageCount
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Color(0xFF120D08), Ink, Color(0xFF07131B))))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GigIconButton(Icons.Rounded.Close, "Return to set list", close)
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(item.sectionName, color = Color.White, fontWeight = FontWeight.Bold)
                item.performanceGroup?.let { group -> Text("${group.type.replaceFirstChar(Char::uppercase)}: ${group.name}  •  ${item.performanceGroupPosition} of ${item.performanceGroupCount}", color = Amber, fontSize = 10.sp, fontWeight = FontWeight.Black) }
                Text("SONG ${position + 1} OF $total", color = TextSoft, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PerformanceMetronomeControls(mediaLink, context, metronome)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            GigIconButton(Icons.Rounded.NavigateBefore, "Previous song", previous, position > 0)
            Column(Modifier.weight(1f)) {
                if (nextItem != null) {
                    Text("> ${gigSongTitle(nextItem)}", color = Amber, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 2)
                    val nextCue = gigSongCue(nextItem)
                    if (nextCue.isNotBlank()) Text(nextCue, color = TextSoft, fontSize = 10.sp, maxLines = 2)
                } else {
                    Text("> END OF SET LIST", color = Amber, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
                previousItem?.let { Text("< ${gigSongTitle(it)}", color = TextSoft, fontSize = 10.sp, maxLines = 2) }
            }
            GigIconButton(Icons.Rounded.NavigateNext, "Next song", next, position < total - 1)
        }
        if (settings.showClock || settings.showElapsed || settings.showSetRemaining) {
            GigTimeStrip(settings, gigStartedAt, setRemainingSeconds, item.sectionName)
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val tablet = maxWidth >= 600.dp
            Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    item.song?.optString("title") ?: item.entry.optString("manual_title", "Untitled"),
                    color = Color.White,
                    fontFamily = FontFamily.Serif,
                    fontSize = if (tablet) 56.sp else 38.sp,
                    lineHeight = if (tablet) 62.sp else 44.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Text(item.song?.optString("artist").orEmpty(), color = TextSoft, fontFamily = FontFamily.Serif, fontSize = if (tablet) 28.sp else 23.sp)
            }
        }
        Box(
            Modifier.fillMaxWidth().height(5.dp).padding(top = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            PerformancePulseLine(metronome)
        }
        val patch = listOf(item.song?.optString("patch_name"), item.song?.optString("patch_number")).filterNotNull().filter(String::isNotBlank).joinToString(" / ")
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val tablet = maxWidth >= 600.dp
            val primarySize = if (tablet) 34.sp else 25.sp
            val secondarySize = if (tablet) 27.sp else 20.sp
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    GigDetail("Starts", item.song?.optString("starts_by").orEmpty(), Modifier.weight(1f), primarySize)
                    GigDetail("Key", displaySongKey(item.song?.optString("song_key").orEmpty()), Modifier.weight(1f), primarySize)
                    GigDetail("Tempo", item.song?.optString("tempo").orEmpty(), Modifier.weight(1f), primarySize)
                    GigDetail("Time", item.song?.optString("time_signature").orEmpty(), Modifier.weight(1f), primarySize)
                }
                Row(Modifier.fillMaxWidth()) {
                    GigDetail("Length", formatDuration(item.song?.optInt("duration_seconds") ?: 0), Modifier.weight(1f), secondarySize)
                    GigDetail("Style", item.song?.optString("style").orEmpty(), Modifier.weight(1f), secondarySize)
                    GigDetail("Patch", patch, Modifier.weight(1f), secondarySize)
                }
            }
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
                GigIconButton(Icons.Rounded.NavigateBefore, "Previous chart page", onClick = { page = (page - 1).coerceAtLeast(0) }, enabled = page > 0)
                Spacer(Modifier.size(6.dp))
                GigIconButton(Icons.Rounded.NavigateNext, "Next chart page", onClick = { page = (page + 1).coerceAtMost(pageCount - 1) }, enabled = page < pageCount - 1)
            }
        }
        val renderedBitmap = rendered.bitmap
        val chartModifier = if (renderedBitmap != null && renderedBitmap.height > 0) {
            Modifier.fillMaxWidth().aspectRatio(renderedBitmap.width.toFloat() / renderedBitmap.height.toFloat())
        } else {
            Modifier.fillMaxWidth().heightIn(min = 320.dp)
        }
        Box(chartModifier.padding(top = 10.dp), contentAlignment = Alignment.TopCenter) {
            val textMaterial = item.attachment?.takeIf { it.optString("source_type") == "text" }?.optString("content_text").orEmpty()
            when {
                textMaterial.isNotBlank() -> ChordProDocument(textMaterial)
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
private fun ChordProDocument(source: String) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val tablet = maxWidth >= 600.dp
        val document = remember(source, tablet) { buildChordProDocument(source, tablet) }
        Text(
            document,
            color = Color.White,
            fontFamily = FontFamily.Serif,
            fontSize = if (tablet) 27.sp else 20.sp,
            lineHeight = if (tablet) 36.sp else 28.sp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = if (tablet) 34.dp else 12.dp, vertical = 18.dp),
        )
    }
}

private fun buildChordProDocument(source: String, tablet: Boolean) = buildAnnotatedString {
    source.replace("\r\n", "\n").lines().forEach { rawLine ->
        val line = stripLeadingLyricTimestamps(rawLine).trimEnd()
        val directive = parseChordProDirective(line)
        val name = directive?.first.orEmpty()
        val argument = directive?.second.orEmpty()
        when {
            name.startsWith("end_of_") || name in CHORDPRO_END_DIRECTIVES -> Unit
            name in CHORDPRO_SECTION_DIRECTIVES -> {
                val label = argument.ifBlank {
                    name.substringAfter("start_of_").ifBlank {
                        when (name) { "sov" -> "verse"; "soc" -> "chorus"; "sob" -> "bridge"; else -> "tab" }
                    }.humanize()
                }
                if (length > 0) append('\n')
                pushStyle(SpanStyle(color = Amber, fontSize = if (tablet) 18.sp else 14.sp, fontWeight = FontWeight.Black))
                append(label.uppercase())
                pop()
                append('\n')
            }
            name in CHORDPRO_COMMENT_DIRECTIVES -> {
                pushStyle(SpanStyle(color = Cyan, fontSize = if (tablet) 19.sp else 15.sp, fontStyle = FontStyle.Italic))
                append(argument)
                pop()
                append('\n')
            }
            directive != null -> Unit
            line.isBlank() -> append('\n')
            else -> {
                append(chordProLine(line))
                append('\n')
            }
        }
    }
}

internal fun stripLeadingLyricTimestamps(value: String): String {
    var cursor = 0
    while (cursor < value.length && value[cursor] == '[') {
        val end = value.indexOf(']', cursor + 1)
        if (end < 0) break
        val marker = value.substring(cursor + 1, end)
        if (marker.isBlank() || marker.any { !it.isDigit() && it != ':' && it != '.' }) break
        cursor = end + 1
    }
    while (cursor < value.length && value[cursor].isWhitespace()) cursor++
    return value.substring(cursor)
}

internal fun parseChordProDirective(line: String): Pair<String, String>? {
    val trimmed = line.trim()
    if (trimmed.length < 3 || trimmed.first() != '{' || trimmed.last() != '}') return null
    val body = trimmed.substring(1, trimmed.lastIndex)
    val separator = body.indexOf(':')
    val name = (if (separator >= 0) body.substring(0, separator) else body).trim().lowercase()
    if (name.isBlank() || '}' in name) return null
    val argument = if (separator >= 0) body.substring(separator + 1).trim() else ""
    return name to argument
}

private fun chordProLine(line: String) = buildAnnotatedString {
    var cursor = 0
    Regex("\\[([^]]+)]|\\*\\*([^*]+)\\*\\*|\\*([^*]+)\\*").findAll(line).forEach { match ->
        append(line.substring(cursor, match.range.first))
        when {
            match.groupValues[1].isNotEmpty() -> {
                pushStyle(SpanStyle(color = Cyan, fontWeight = FontWeight.Black, fontFamily = StudioFont))
                append(match.groupValues[1])
                append(" ")
            }
            match.groupValues[2].isNotEmpty() -> {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                append(match.groupValues[2])
            }
            else -> {
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                append(match.groupValues[3])
            }
        }
        pop()
        cursor = match.range.last + 1
    }
    append(line.substring(cursor))
}

private val CHORDPRO_END_DIRECTIVES = setOf("eov", "eoc", "eob", "eot")
private val CHORDPRO_SECTION_DIRECTIVES = setOf("start_of_verse", "sov", "start_of_chorus", "soc", "start_of_bridge", "sob", "start_of_prechorus", "start_of_intro", "start_of_outro", "start_of_tab", "sot")
private val CHORDPRO_COMMENT_DIRECTIVES = setOf("comment", "c")

@Composable
private fun PerformanceMetronomeControls(mediaLink: String?, context: Context, metronome: NativeMetronome) {
    val state by metronome.state.collectAsState()
    mediaLink?.let { link -> GigIconButton(Icons.Rounded.Headphones, "Listen", onClick = { openMediaLink(context, link) }) }
    GigIconButton(
        if (state.running) Icons.Rounded.Pause else Icons.Rounded.MusicNote,
        if (state.running) "Stop metronome" else "Start metronome",
        metronome::toggle,
        active = state.running,
    )
    GigIconButton(
        if (state.muted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
        if (state.muted) "Unmute metronome" else "Mute metronome",
        metronome::toggleMuted,
        active = state.muted,
    )
}

@Composable
private fun PerformancePulseLine(metronome: NativeMetronome) {
    val state by metronome.state.collectAsState()
    Surface(
        Modifier.fillMaxWidth().height(if (state.pulse) 5.dp else 2.dp),
        color = if (state.downbeat) Cyan else Amber,
    ) {}
}

@Composable
private fun GigCircleButton(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    Surface(
        color = Amber.copy(alpha = if (enabled) 1f else 0.28f), contentColor = Ink, shape = RoundedCornerShape(50),
        modifier = Modifier.size(46.dp).clickable(enabled = enabled, onClick = onClick),
    ) { Box(contentAlignment = Alignment.Center) { Text(label, fontSize = if (label == "♪") 24.sp else 20.sp, fontWeight = FontWeight.Black) } }
}

@Composable
private fun GigIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    active: Boolean = false,
) {
    Surface(
        color = when {
            !enabled -> Amber.copy(alpha = 0.18f)
            active -> Cyan
            else -> Amber
        },
        contentColor = Ink,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, if (active) Cyan else Amber.copy(alpha = if (enabled) 0.9f else 0.22f)),
        modifier = Modifier.size(46.dp).clickable(enabled = enabled, onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(23.dp), tint = Ink.copy(alpha = if (enabled) 1f else 0.45f))
        }
    }
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
private fun GigDetail(label: String, value: String, modifier: Modifier = Modifier, valueSize: androidx.compose.ui.unit.TextUnit = 20.sp) {
    Column(modifier.padding(horizontal = 3.dp, vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label.uppercase(), color = TextSoft, fontSize = 9.sp, fontWeight = FontWeight.Black)
        Text(value.ifBlank { "Not set" }, color = Color.White, fontFamily = FontFamily.Serif, fontSize = valueSize, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

private fun gigSongTitle(item: GigSong) = item.song?.optString("title")?.takeIf(String::isNotBlank) ?: item.entry.optString("manual_title", "Untitled")
private fun gigSongCue(item: GigSong) = listOf(item.song?.optString("starts_by"), displaySongKey(item.song?.optString("song_key").orEmpty()).takeIf(String::isNotBlank)?.let { "Key $it" }, item.song?.optString("tempo"), item.song?.optString("time_signature")).filterNotNull().filter(String::isNotBlank).joinToString(" / ")

private fun normalizeSongKey(value: String): String = value
    .replace('♯', '#').replace('♭', 'b').replace("♮", "")
    .trim().replace(Regex("\\s+"), " ").take(40)

private fun displaySongKey(value: String): String {
    val clean = normalizeSongKey(value)
    val match = Regex("^([A-Ga-g])([#b]?)(.*)$").matchEntire(clean) ?: return clean
    val accidental = when (match.groupValues[2]) { "#" -> "♯"; "b" -> "♭"; else -> "" }
    return match.groupValues[1].uppercase() + accidental + match.groupValues[3]
}

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

private fun openContactLink(context: Context, scheme: String, value: String) {
    val action = if (scheme == "tel") Intent.ACTION_DIAL else Intent.ACTION_SENDTO
    val intent = Intent(action, Uri.fromParts(scheme, value.trim(), null))
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No application is available for this action.", Toast.LENGTH_LONG).show()
    } catch (_: SecurityException) {
        Toast.makeText(context, "This action is not available on this device.", Toast.LENGTH_LONG).show()
    }
}

private fun openGroupContactLink(context: Context, scheme: String, values: List<String>) {
    val recipients = values.map(String::trim).filter(String::isNotBlank).distinct()
    if (recipients.isEmpty()) return
    val uri = if (scheme == "mailto") {
        Uri.parse("mailto:?bcc=${Uri.encode(recipients.joinToString(","))}")
    } else {
        Uri.parse("smsto:${recipients.joinToString(";") { Uri.encode(it) }}")
    }
    try {
        context.startActivity(Intent(Intent.ACTION_SENDTO, uri))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No application is available for this group message.", Toast.LENGTH_LONG).show()
    } catch (_: SecurityException) {
        Toast.makeText(context, "Group messaging is not available on this device.", Toast.LENGTH_LONG).show()
    }
}

internal fun mediaIntent(link: String): Intent? = normalizedMediaLink(link)?.let { safeLink ->
    Intent(Intent.ACTION_VIEW, Uri.parse(safeLink))
}

@Composable
private fun SongDetailFallback(item: GigSong) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val tablet = maxWidth >= 600.dp
        Column(
            Modifier.fillMaxWidth().heightIn(min = if (tablet) 360.dp else 250.dp).padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                item.song?.optString("title")?.takeIf(String::isNotBlank) ?: item.entry.optString("manual_title", "Untitled"),
                color = Color.White,
                fontFamily = FontFamily.Serif,
                fontSize = if (tablet) 56.sp else 38.sp,
                lineHeight = if (tablet) 62.sp else 44.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Text(
                if (item.attachment != null && item.cache?.status != "ready") "${attachmentLabel(item.attachment)} is not available offline." else "No performance attachment is available for this song.",
                color = TextSoft,
                fontSize = if (tablet) 18.sp else 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
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
    val performanceGroup: PerformanceGroup? = null,
    val performanceGroupPosition: Int = 0,
    val performanceGroupCount: Int = 0,
)
private data class PacketReadiness(val ready: Int, val total: Int)
private data class AttachmentRender(val bitmap: Bitmap? = null, val complete: Boolean = false, val pageCount: Int = 1)

private val performanceAttachmentCache = object : LruCache<String, AttachmentRender>(performanceAttachmentCacheBytes(Runtime.getRuntime().maxMemory())) {
    override fun sizeOf(key: String, value: AttachmentRender): Int = value.bitmap?.allocationByteCount ?: 1
}
private val performanceAttachmentRenderLocks = ConcurrentHashMap<String, Any>()

private fun performanceAttachmentCacheKey(path: String, version: String, page: Int) = "$path#$version#$page"

private fun cachedPerformanceAttachment(path: String, version: String, page: Int): AttachmentRender? =
    path.takeIf(String::isNotBlank)?.let { performanceAttachmentCache.get(performanceAttachmentCacheKey(it, version, page)) }

private fun preloadPerformanceAttachment(item: GigSong) {
    val path = item.cache?.localPath.orEmpty()
    if (path.isBlank()) return
    val version = item.cache?.sha256.orEmpty().ifBlank { item.cache?.revision?.toString().orEmpty() }
    val isPdf = item.cache?.mimeType == "application/pdf" || path.endsWith(".pdf", true)
    val previousPriority = Process.getThreadPriority(Process.myTid())
    try {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
        loadPerformanceAttachment(path, version, isPdf, 0)
    } finally {
        Process.setThreadPriority(previousPriority)
    }
}

private fun loadPerformanceAttachment(path: String, version: String, isPdf: Boolean, page: Int): AttachmentRender {
    if (path.isBlank()) return AttachmentRender(complete = true)
    val key = performanceAttachmentCacheKey(path, version, page)
    performanceAttachmentCache.get(key)?.let { return it }
    val renderLock = performanceAttachmentRenderLocks.getOrPut(key) { Any() }
    return synchronized(renderLock) {
        performanceAttachmentCache.get(key) ?: run {
            val rendered = if (isPdf) renderPdfAttachment(path, page) else AttachmentRender(decodeAttachmentImage(path), complete = true)
            if (rendered.bitmap != null) performanceAttachmentCache.put(key, rendered)
            rendered
        }
    }
}

internal fun clearPerformanceAttachmentMemoryCache() {
    performanceAttachmentCache.evictAll()
}

private fun renderPdfAttachment(path: String, pageIndex: Int): AttachmentRender = runCatching {
    ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
        PdfRenderer(descriptor).use { renderer ->
            val count = renderer.pageCount.coerceAtLeast(1)
            renderer.openPage(pageIndex.coerceIn(0, count - 1)).use { page ->
                val width = performanceAttachmentRenderWidth(Runtime.getRuntime().maxMemory())
                val height = (width.toFloat() / page.width * page.height).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                AttachmentRender(bitmap, complete = true, pageCount = count)
            }
        }
    }
}.getOrElse { AttachmentRender(complete = true) }

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

private fun selectPerformanceAttachment(
    entry: JSONObject,
    attachments: List<JSONObject>,
    preferences: List<String> = listOf("drum_chart", "chart", "sheet_music", "lyrics", "tab"),
): JSONObject? {
    if (attachments.isEmpty()) return null
    val overrideId = entry.optString("performance_attachment_id")
    if (overrideId.isNotBlank()) attachments.firstOrNull { it.optString("id") == overrideId }?.let { return it }
    return attachments.firstOrNull { it.optInt("is_gig_default") == 1 }
        ?: preferences.firstNotNullOfOrNull { preferred ->
            attachments.filter { it.optString("attachment_type") == preferred }.minByOrNull { it.optInt("position", Int.MAX_VALUE) }
        }
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
                val width = performanceAttachmentRenderWidth(Runtime.getRuntime().maxMemory())
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
    val width = performanceAttachmentRenderWidth(Runtime.getRuntime().maxMemory())
    val sample = performanceAttachmentSampleSize(bounds.outWidth, bounds.outHeight, width, width * 2)
    return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
}

internal fun performanceAttachmentRenderWidth(maxMemoryBytes: Long): Int = when {
    maxMemoryBytes <= 256L * 1024 * 1024 -> 1080
    maxMemoryBytes <= 384L * 1024 * 1024 -> 1280
    else -> 1440
}

internal fun performanceAttachmentCacheBytes(maxMemoryBytes: Long): Int =
    (maxMemoryBytes / 10L).coerceIn(12L * 1024 * 1024, 32L * 1024 * 1024).toInt()

internal fun performanceAttachmentSampleSize(sourceWidth: Int, sourceHeight: Int, targetWidth: Int, targetHeight: Int): Int {
    if (sourceWidth <= 0 || sourceHeight <= 0) return 1
    var sample = 1
    while (sourceWidth / (sample * 2) >= targetWidth && sourceHeight / (sample * 2) >= targetHeight) sample *= 2
    while (sourceWidth / sample > targetWidth * 2 || sourceHeight / sample > targetHeight * 2) sample *= 2
    return sample.coerceAtLeast(1)
}

private const val PERFORMANCE_NEIGHBOR_PRELOAD_DELAY_MS = 350L
