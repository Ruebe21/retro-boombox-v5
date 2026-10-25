package com.example.boomboxv5

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

data class Station(
    val name: String,
    val city: String,
    val url: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RetroBoomboxV5App()
        }
    }
}

@Composable
fun RetroBoomboxV5App() {
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }

    var mode by remember { mutableStateOf("radio") }
    var isPlaying by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var volume by remember { mutableFloatStateOf(0.8f) }
    var bass by remember { mutableFloatStateOf(0.6f) }
    var treble by remember { mutableFloatStateOf(0.5f) }
    var balance by remember { mutableFloatStateOf(0.5f) }
    var stationIndex by remember { mutableIntStateOf(0) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedName by remember { mutableStateOf("No file selected") }

    val stations = remember {
        listOf(
            Station("Deutschlandfunk", "Berlin", "https://st01.sslstream.dlf.de/dlf/01/128/mp3/stream.mp3"),
            Station("NDR 2", "Hamburg", "https://icecast.ndr.de/ndr/ndr2/hamburg/stream.mp3"),
            Station("France Inter", "Paris", "https://stream.radiofrance.fr/franceinter/franceinter.m3u8"),
            Station("RNE Radio 5", "Madrid", "https://rtvelivestream.akamaized.net/rtvesec/rne/rne_r5_main.m3u8"),
            Station("Radio Swiss Classic", "Zürich", "http://stream.srg-ssr.ch/m/rsc_de/mp3_128"),
            Station("BBC Radio 2", "London", "https://stream.live.vc.bbcmedia.co.uk/bbc_radio_two"),
            Station("Euronews", "Brussels", "https://static.euronews.com/streams/mp3/radio_en.mp3"),
            Station("RFI", "Paris", "https://rfienchinha.olympion.com/rfi.mp3"),
            Station("RTS Espace 2", "Genève", "https://stream.srg-ssr.ch/m/espace-2/mp3_128"),
            Station("Radio 24", "Milano", "https://stream.zeno.fm/6g9s3d5s6e3tv")
        )
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedUri = it
            selectedName = getFileName(context, it)
            mode = "cassette"
            player.setMediaItem(MediaItem.fromUri(it))
            player.prepare()
            player.play()
            isPlaying = true
        }
    }

    LaunchedEffect(player) {
        while (true) {
            position = player.currentPosition
            delay(1000L)
        }
    }

    LaunchedEffect(mode, stationIndex, isPlaying) {
        if (mode == "radio" && isPlaying) {
            val station = stations[stationIndex]
            player.setMediaItem(MediaItem.fromUri(station.url))
            player.prepare()
            player.play()
        }
    }

    LaunchedEffect(volume) {
        player.volume = volume
    }

    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

    val cassetteRotation by rememberInfiniteTransition(label = "cassetteRot").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cassetteRot"
    )

    BoomboxV5Layout(
        mode = mode,
        isPlaying = isPlaying,
        cassetteRotation = if (isPlaying && mode == "cassette") cassetteRotation else 0f,
        selectedName = selectedName,
        currentPosition = position,
        stationIndex = stationIndex,
        stationList = stations,
        volume = volume,
        bass = bass,
        treble = treble,
        balance = balance,
        onMode = { newMode ->
            mode = newMode
            if (newMode == "radio") {
                val station = stations[stationIndex]
                player.setMediaItem(MediaItem.fromUri(station.url))
                player.prepare()
                player.play()
                isPlaying = true
            } else if (selectedUri != null) {
                player.setMediaItem(MediaItem.fromUri(selectedUri!!))
                player.prepare()
                player.play()
                isPlaying = true
            }
        },
        onEject = { filePicker.launch("audio/mpeg") },
        onPlayPause = {
            if (mode == "cassette") {
                if (selectedUri == null) return@BoomboxV5Layout
                if (isPlaying) player.pause() else player.play()
                isPlaying = !isPlaying
            } else {
                if (isPlaying) {
                    player.pause()
                } else {
                    val station = stations[stationIndex]
                    player.setMediaItem(MediaItem.fromUri(station.url))
                    player.prepare()
                    player.play()
                }
                isPlaying = !isPlaying
            }
        },
        onTune = { delta ->
            var next = stationIndex + delta
            if (next < 0) next = stations.size - 1
            if (next >= stations.size) next = 0
            stationIndex = next
            if (mode == "radio") {
                val s = stations[next]
                player.setMediaItem(MediaItem.fromUri(s.url))
                player.prepare()
                player.play()
                isPlaying = true
            }
        },
        onVolumeChanged = { volume = it },
        onBassChanged = { bass = it },
        onTrebleChanged = { treble = it },
        onBalanceChanged = { balance = it }
    )
}

@Composable
fun BoomboxV5Layout(
    mode: String,
    isPlaying: Boolean,
    cassetteRotation: Float,
    selectedName: String,
    currentPosition: Long,
    stationIndex: Int,
    stationList: List<Station>,
    volume: Float,
    bass: Float,
    treble: Float,
    balance: Float,
    onMode: (String) -> Unit,
    onEject: () -> Unit,
    onPlayPause: () -> Unit,
    onTune: (Int) -> Unit,
    onVolumeChanged: (Float) -> Unit,
    onBassChanged: (Float) -> Unit,
    onTrebleChanged: (Float) -> Unit,
    onBalanceChanged: (Float) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFD9D9D9)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(980.dp)
                .height(720.dp)
                .shadow(24.dp, RoundedCornerShape(28.dp))
                .background(
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0xFFF6D95B),
                            Color(0xFFF2D14D),
                            Color(0xFFEFD34B),
                            Color(0xFFF7E479)
                        )
                    )
                )
                .border(4.dp, Color(0xFF7D8D92), RoundedCornerShape(28.dp))
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-22).dp)
                    .width(820.dp)
                    .height(86.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        brush = Brush.linearGradient(
                            listOf(Color(0xFF7ED8C8), Color(0xFF6AC3B6), Color(0xFF8CE2D3))
                        )
                    )
                    .border(4.dp, Color(0xFF7D8C91), RoundedCornerShape(18.dp))
                    .shadow(10.dp, RoundedCornerShape(18.dp))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InternetBadge()
                TunerDisplay(
                    stationList = stationList,
                    stationIndex = stationIndex,
                    onTune = onTune
                )
                DeckVolume(
                    volume = volume,
                    onValueChange = onVolumeChanged
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 96.dp)
                    .width(500.dp)
                    .height(188.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFF0D24D))
                    .border(3.dp, Color(0xFF8C9097), RoundedCornerShape(18.dp))
                    .shadow(10.dp, RoundedCornerShape(18.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.width(110.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SmallToggle("MODE", active = mode == "radio") { onMode("radio") }
                        SmallToggle("BAND", active = mode == "cassette") { onMode("cassette") }
                        SmallToggle("MENU", active = false) { onEject() }
                        SmallToggle("FAV", active = false) { onEject() }
                    }

                    Box(
                        modifier = Modifier
                            .width(220.dp)
                            .height(128.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFCF9E39))
                            .border(2.dp, Color(0xFF686D70), RoundedCornerShape(12.dp))
                            .shadow(8.dp, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CassetteWheel(rotation = cassetteRotation)
                            CassetteWheel(rotation = cassetteRotation)
                        }
                    }

                    Text(
                        text = formatTime(currentPosition),
                        color = Color(0xFF1A7EA8),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            SpeakerPanel(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 32.dp, y = (-38).dp)
                    .size(300.dp, 250.dp)
            )

            SpeakerPanel(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-32).dp, y = (-38).dp)
                    .size(300.dp, 250.dp)
            )

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-30).dp)
                    .fillMaxWidth()
                    .padding(horizontal = 170.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TransportButton(Icons.Default.FastRewind) {}
                TransportButton(Icons.Default.PlayArrow) { onPlayPause() }
                TransportButton(Icons.Default.Stop) {}
                TransportButton(Icons.Default.FastForward) {}
                RecordButton {}
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-120).dp)
                    .fillMaxWidth()
                    .padding(horizontal = 110.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                KnobControl("BASS", bass, onBassChanged)
                KnobControl("TREBLE", treble, onTrebleChanged)
                KnobControl("BALANCE", balance, onBalanceChanged)
                SideSwitch("TAPE", active = mode == "cassette")
                SideSwitch("RADIO", active = mode == "radio")
                SideSwitch("AUX", active = false)
            }

            if (selectedName != "No file selected") {
                Text(
                    text = selectedName,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = (-170).dp),
                    color = Color(0xFF143C5C),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun InternetBadge() {
    Box(
        modifier = Modifier
            .width(170.dp)
            .height(120.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFF3D75A))
            .border(4.dp, Color(0xFF7EA8AE), RoundedCornerShape(18.dp))
            .shadow(8.dp, RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Canvas(modifier = Modifier.size(32.dp)) {
                drawArc(
                    color = Color(0xFF173F5C),
                    startAngle = 20f,
                    sweepAngle = 330f,
                    useCenter = false,
                    style = Stroke(width = 3f)
                )
                drawCircle(Color(0xFF173F5C), radius = 4f)
            }
            Text(
                text = "INTERNET\nRADIO",
                color = Color(0xFF173F5C),
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TunerDisplay(
    stationList: List<Station>,
    stationIndex: Int,
    onTune: (Int) -> Unit
) {
    val current = stationList[stationIndex]

    Box(
        modifier = Modifier
            .width(420.dp)
            .height(120.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF113E5E))
            .border(4.dp, Color(0xFF7ED7C9), RoundedCornerShape(18.dp))
            .shadow(10.dp, RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("FM", color = Color(0xFFBDEAF8), fontWeight = FontWeight.Bold)
                Text("88", color = Color(0xFFBDEAF8))
                Text("92", color = Color(0xFFBDEAF8))
                Text("96", color = Color(0xFFBDEAF8))
                Text("100", color = Color(0xFFBDEAF8))
                Text("104", color = Color(0xFFBDEAF8))
                Text("108", color = Color(0xFFBDEAF8))
                Text("MHz", color = Color(0xFFBDEAF8))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("AM", color = Color(0xFFBDEAF8), fontWeight = FontWeight.Bold)
                Text("530", color = Color(0xFFBDEAF8))
                Text("700", color = Color(0xFFBDEAF8))
                Text("900", color = Color(0xFFBDEAF8))
                Text("1100", color = Color(0xFFBDEAF8))
                Text("1300", color = Color(0xFFBDEAF8))
                Text("1600", color = Color(0xFFBDEAF8))
                Text("kHz", color = Color(0xFFBDEAF8))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFB2E4D7))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { _, dragAmount ->
                            if (dragAmount.x > 30f) onTune(1)
                            else if (dragAmount.x < -30f) onTune(-1)
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(current.city, color = Color(0xFF153D5D), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(current.name, color = Color(0xFF153D5D), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun DeckVolume(volume: Float, onValueChange: (Float) -> Unit) {
    Box(
        modifier = Modifier
            .width(250.dp)
            .height(120.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFBEE7D7))
            .border(4.dp, Color(0xFF7AAFB8), RoundedCornerShape(18.dp))
            .shadow(8.dp, RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Good Vibes", color = Color(0xFF123E5F), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("The Sunlights", color = Color(0xFF123E5F), fontSize = 12.sp)
            }
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF7BD6C8)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFF123E5F),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 14.dp, y = 16.dp)
                .size(108.dp)
                .clip(CircleShape)
                .background(Color(0xFF6AC9C5))
                .border(3.dp, Color(0xFF7C8D91), CircleShape)
                .shadow(8.dp, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEFF4F5))
                    .border(3.dp, Color(0xFF9AA6A9), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .offset(y = (-18).dp)
                        .clip(CircleShape)
                        .background(Color(0xFF5FAE9D))
                )
            }
        }

        Slider(
            value = volume,
            onValueChange = onValueChange,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-12).dp)
                .width(170.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF183E5D),
                activeTrackColor = Color(0xFF1B75B0),
                inactiveTrackColor = Color(0xFF9CC4D7)
            )
        )

        Text(
            text = "VOLUME",
            modifier = Modifier.align(Alignment.BottomEnd).offset(y = 10.dp),
            color = Color(0xFF153E59),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SpeakerPanel(modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(30.dp))
            .background(
                brush = Brush.linearGradient(
                    listOf(Color(0xFF73D8C6), Color(0xFF5BC4B9), Color(0xFF7FE6D2))
                )
            )
            .border(3.dp, Color(0xFF7E8C92), RoundedCornerShape(30.dp))
            .shadow(10.dp, RoundedCornerShape(30.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(Color(0xFF232323))
                .border(10.dp, Color(0xFF67D0BE), CircleShape)
                .shadow(12.dp, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(126.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF191919))
                    .border(2.dp, Color(0xFF8A9197), CircleShape)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val r = minOf(size.width, size.height) * 0.34f
                    for (i in 0 until 14) {
                        val angle = Math.toRadians((i * 25).toDouble())
                        val x = cx + r * cos(angle).toFloat()
                        val y = cy + r * sin(angle).toFloat()
                        drawCircle(color = Color(0xFF6F6F6F), radius = 4f, center = Offset(x, y))
                    }
                }
            }
        }
    }
}

@Composable
fun SmallToggle(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(24.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) Color(0xFF8EDBD2) else Color(0xFF8DD4CA))
            .border(2.dp, Color(0xFF718D90), RoundedCornerShape(8.dp))
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color(0xFF153F5D),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
    }
}

@Composable
fun TransportButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(58.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(Color(0xFF7FDBC9))
            .border(2.dp, Color(0xFF788C90), RoundedCornerShape(15.dp))
            .shadow(6.dp, RoundedCornerShape(15.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF153C5D),
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
fun RecordButton(onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .size(58.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(Color(0xFFF35F6D))
            .border(2.dp, Color(0xFF7A8D91), RoundedCornerShape(15.dp))
            .shadow(6.dp, RoundedCornerShape(15.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.FiberManualRecord,
            contentDescription = null,
            tint = Color(0xFF123B5C),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun KnobControl(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(Color(0xFF7CCFC3))
                .border(2.dp, Color(0xFF728A8F), CircleShape)
                .shadow(6.dp, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3A4041))
            )
        }

        Text(
            text = label,
            color = Color(0xFF153F5F),
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )

        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.width(82.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF1D3E5E),
                activeTrackColor = Color(0xFF78A9D8),
                inactiveTrackColor = Color(0xFFBCD2E6)
            )
        )
    }
}

@Composable
fun SideSwitch(label: String, active: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (active) Color(0xFFE7D86D) else Color(0xFFDDE7F5))
                .border(2.dp, Color(0xFF7A8A8F), RoundedCornerShape(10.dp))
                .shadow(4.dp, RoundedCornerShape(10.dp))
        )
        Text(
            text = label,
            color = Color(0xFF153E5C),
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}

@Composable
fun CassetteWheel(rotation: Float) {
    Box(
        modifier = Modifier
            .size(62.dp)
            .clip(CircleShape)
            .background(Color(0xFF2D2D2D))
            .border(2.dp, Color(0xFFB2B7B9), CircleShape)
            .shadow(6.dp, CircleShape)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color(0xFFB7B1AA))
                .align(Alignment.Center)
        )
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000L
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

fun getFileName(context: Context, uri: Uri): String {
    val resolver: ContentResolver = context.contentResolver
    val cursor = resolver.query(uri, null, null, null, null)
    cursor?.use {
        val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && it.moveToFirst()) {
            return it.getString(index)
        }
    }
    return "Unknown File"
}
