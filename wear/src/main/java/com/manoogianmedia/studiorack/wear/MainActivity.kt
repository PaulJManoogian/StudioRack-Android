package com.manoogianmedia.studiorack.wear

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import com.manoogianmedia.studiorack.liveprotocol.LiveCommandType
import com.manoogianmedia.studiorack.liveprotocol.LiveSnapshot
import com.manoogianmedia.studiorack.liveprotocol.LiveSong
import kotlinx.coroutines.delay

private val Ink = Color(0xFF07090F)
private val Panel = Color(0xFF202635)
private val Amber = Color(0xFFFF9D1E)
private val Cyan = Color(0xFF42D9FF)
private val TextSoft = Color(0xFFAEB8CB)
private val LiveGreen = Color(0xFF58E99B)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WearStateStore.initialize(this)
        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0 && intent.getBooleanExtra("wear_preview", false)) {
            WearStateStore.preview(
                LiveSnapshot(
                    active = true,
                    eventId = "preview",
                    eventTitle = "Saturday Night",
                    setListName = "Main Set",
                    sectionName = "Main Set",
                    currentIndex = 6,
                    totalSongs = 12,
                    current = LiveSong("China Grove", "The Doobie Brothers", "A", "146", "4/4", "Guitar", "medley", "Opening Run", 2, 3),
                    previous = LiveSong("Rain", "The Beatles", "G", "120", "4/4", "Drums"),
                    next = LiveSong("Walkin' On The Sun", "Smash Mouth", "F#", "123", "4/4", "All Start"),
                    metronomeTempo = 146,
                    beatsPerMeasure = 4,
                    revision = System.currentTimeMillis(),
                )
            )
        }
        setContent {
            MaterialTheme {
                val controller = remember { WearCommandController(this) }
                LeviathanLiveWatch(controller)
            }
        }
    }
}

@Composable
private fun LeviathanLiveWatch(controller: WearCommandController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val snapshot by WearStateStore.snapshot.collectAsState()
    val phoneConnected by WearStateStore.phoneConnected.collectAsState()
    val commandStatus by controller.status.collectAsState()
    val listState = rememberScalingLazyListState()

    LaunchedEffect(snapshot.lastCommandId) { controller.acknowledge(snapshot.lastCommandId) }
    WatchMetronome(context, snapshot)

    Scaffold(
        modifier = Modifier.fillMaxSize().background(Ink),
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        if (!snapshot.active) {
            Column(
                Modifier.fillMaxSize().padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("LEVIATHAN", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Text("LIVE", color = Amber, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text("Open a set list in Leviathan Live on your phone or tablet.", color = TextSoft, textAlign = TextAlign.Center, fontSize = 12.sp)
            }
            return@Scaffold
        }

        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 18.dp),
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        val connectionColor = if (phoneConnected) LiveGreen else Amber
                        Box(Modifier.size(7.dp).background(connectionColor, CircleShape))
                        Text(if (phoneConnected) "LIVE" else "CACHED", color = connectionColor, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                    Text(snapshot.sectionName.ifBlank { snapshot.setListName }, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("SONG ${snapshot.currentIndex + 1} OF ${snapshot.totalSongs}", color = TextSoft, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            snapshot.current.groupLabel()?.let { label ->
                item { Text(label, color = Amber, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
            }
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(snapshot.current.title, color = Color.White, fontSize = 22.sp, lineHeight = 24.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 3)
                    if (snapshot.current.artist.isNotBlank()) Text(snapshot.current.artist, color = TextSoft, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 2)
                    val facts = snapshot.current.facts()
                    if (facts.isNotBlank()) Text(facts, color = Amber, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { controller.send(LiveCommandType.PREVIOUS) },
                        enabled = snapshot.currentIndex > 0 && commandStatus.pendingId.isBlank(),
                        colors = ButtonDefaults.secondaryButtonColors(backgroundColor = Panel),
                    ) { Text("<", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black) }
                    Button(
                        onClick = { controller.send(LiveCommandType.NEXT) },
                        enabled = snapshot.currentIndex < snapshot.totalSongs - 1 && commandStatus.pendingId.isBlank(),
                        colors = ButtonDefaults.primaryButtonColors(backgroundColor = Amber),
                    ) { Text(">", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Black) }
                }
            }
            if (commandStatus.message.isNotBlank()) {
                item { Text(commandStatus.message, color = if (commandStatus.pendingId.isBlank()) LiveGreen else Cyan, fontSize = 9.sp) }
            }
            if (snapshot.next.title.isNotBlank()) {
                item {
                    Column(Modifier.fillMaxWidth().background(Panel).padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NEXT", color = Amber, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        Text(snapshot.next.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2)
                        Text(snapshot.next.facts(), color = TextSoft, fontSize = 10.sp, textAlign = TextAlign.Center)
                    }
                }
            }
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { controller.send(LiveCommandType.TOGGLE_METRONOME) },
                    enabled = commandStatus.pendingId.isBlank(),
                    label = { Text(if (snapshot.metronomeRunning) "Stop metronome" else "Start metronome", fontWeight = FontWeight.Bold) },
                    secondaryLabel = { Text("${snapshot.metronomeTempo} BPM • ${snapshot.beatsPerMeasure}/4") },
                    colors = ChipDefaults.primaryChipColors(backgroundColor = if (snapshot.metronomeRunning) Amber else Panel, contentColor = if (snapshot.metronomeRunning) Ink else Color.White),
                )
            }
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { controller.send(LiveCommandType.TOGGLE_MUTE) },
                    enabled = commandStatus.pendingId.isBlank(),
                    label = { Text(if (snapshot.metronomeMuted) "Metronome muted" else "Metronome audible") },
                    colors = ChipDefaults.secondaryChipColors(backgroundColor = Panel),
                )
            }
            if (snapshot.previous.title.isNotBlank()) {
                item { Text("PREVIOUS  ${snapshot.previous.title}", color = TextSoft, fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 2) }
            }
        }
    }
}

@Composable
private fun WatchMetronome(context: Context, snapshot: LiveSnapshot) {
    var pulse by remember { mutableIntStateOf(0) }
    LaunchedEffect(snapshot.metronomeRunning, snapshot.metronomeStartedAtEpochMs, snapshot.metronomeTempo, snapshot.beatsPerMeasure) {
        if (!snapshot.metronomeRunning || snapshot.metronomeStartedAtEpochMs <= 0) return@LaunchedEffect
        val interval = 60_000L / snapshot.metronomeTempo.coerceIn(30, 260)
        while (true) {
            val elapsed = (System.currentTimeMillis() - snapshot.metronomeStartedAtEpochMs).coerceAtLeast(0)
            val nextBeat = elapsed / interval + 1
            delay((snapshot.metronomeStartedAtEpochMs + nextBeat * interval - System.currentTimeMillis()).coerceAtLeast(1))
            pulse = (nextBeat % snapshot.beatsPerMeasure.coerceAtLeast(1)).toInt()
            vibrateBeat(context, downbeat = pulse == 0)
        }
    }
}

@Suppress("DEPRECATION")
private fun vibrateBeat(context: Context, downbeat: Boolean) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java).defaultVibrator
    } else {
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    if (vibrator.hasVibrator()) vibrator.vibrate(
        VibrationEffect.createOneShot(if (downbeat) 55 else 28, if (downbeat) 210 else 120)
    )
}

private fun LiveSong.facts(): String = listOf(
    startsBy.takeIf(String::isNotBlank),
    key.takeIf(String::isNotBlank)?.let { "Key $it" },
    tempo.takeIf(String::isNotBlank),
    timeSignature.takeIf(String::isNotBlank),
).filterNotNull().joinToString(" • ")

private fun LiveSong.groupLabel(): String? = groupName.takeIf(String::isNotBlank)?.let { name ->
    val kind = groupType.ifBlank { "Group" }.replaceFirstChar(Char::uppercase)
    "$kind: $name • $groupPosition of $groupCount"
}
