package com.noirmusic.app

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutionException

private val Bg = Color(0xFF090811)
private val SurfaceDark = Color(0xFF171322)
private val Purple = Color(0xFFB66CFF)
private val Muted = Color(0xFFAAA2B8)

class MainActivity : ComponentActivity() {
    private var controllerFuture: ListenableFuture<MediaController>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, token).buildAsync()

        setContent {
            NoirTheme {
                NoirHome(
                    onPlay = { track ->
                        val future = controllerFuture
                        if (future != null) {
                            try {
                                val controller = future.get()
                                controller.setMediaItem(MediaItem.fromUri(track.uri))
                                controller.prepare()
                                controller.play()
                            } catch (_: Exception) { }
                        }
                    },
                    onPermissionGranted = { }
                )
            }
        }
    }

    override fun onDestroy() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        super.onDestroy()
    }
}

@Composable
private fun NoirTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Purple,
            secondary = Color(0xFFDAA8FF),
            background = Bg,
            surface = SurfaceDark,
            onBackground = Color.White,
            onSurface = Color.White
        ),
        content = content
    )
}

@Composable
private fun NoirHome(onPlay: (MusicTrack) -> Unit, onPermissionGranted: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var tracks by remember { mutableStateOf<List<MusicTrack>>(emptyList()) }
    var selectedTab by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var permissionGranted by remember { mutableStateOf(false) }
    var selectedTrack by remember { mutableStateOf<MusicTrack?>(null) }

    val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
                     else Manifest.permission.READ_EXTERNAL_STORAGE
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
        if (granted) scope.launch(Dispatchers.IO) {
            val loaded = MusicRepository(context).loadAudio()
            withContext(Dispatchers.Main) { tracks = loaded }
        }
    }

    LaunchedEffect(Unit) {
        permissionGranted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        if (permissionGranted) {
            tracks = withContext(Dispatchers.IO) { MusicRepository(context).loadAudio() }
        } else {
            permissionLauncher.launch(permission)
        }
    }

    val filtered = remember(tracks, query) {
        tracks.filter { it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true) }
    }

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF100D19), contentColor = Purple) {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.LibraryMusic, null) }, label = { Text("Library") })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Folder, null) }, label = { Text("Folders") })
                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Equalizer, null) }, label = { Text("Equalizer") })
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding)
                .background(Brush.verticalGradient(listOf(Color(0xFF21112F), Bg, Bg)))
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("NOIR", color = Purple, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                    Text("Music", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                }
                Box(Modifier.size(46.dp).clip(CircleShape).background(SurfaceDark), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Headphones, null, tint = Purple)
                }
            }
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search your music...", color = Muted) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Muted) },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Purple, unfocusedBorderColor = Color(0xFF352A45),
                    focusedContainerColor = SurfaceDark, unfocusedContainerColor = SurfaceDark
                )
            )
            Spacer(Modifier.height(22.dp))
            when (selectedTab) {
                0 -> {
                    Text("YOUR LIBRARY", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(12.dp))
                    if (!permissionGranted) {
                        EmptyState("Music access needed", "Allow access to scan audio files on your phone.") {
                            permissionLauncher.launch(permission)
                        }
                    } else if (filtered.isEmpty()) {
                        EmptyState("No music found", "Add audio files to your phone, then reopen Noir Music.") { 
                            scope.launch(Dispatchers.IO) {
                                val loaded = MusicRepository(context).loadAudio()
                                withContext(Dispatchers.Main) { tracks = loaded }
                            }
                        }
                    } else {
                        Text("${filtered.size} tracks", color = Muted, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filtered, key = { it.id }) { track ->
                                TrackRow(track, onClick = {
                                    selectedTrack = track
                                    onPlay(track)
                                })
                            }
                        }
                    }
                }
                1 -> {
                    Text("FOLDERS", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Your device audio library is scanned through MediaStore. Folder browsing and SAF import are planned for the next implementation stage.", color = Muted, lineHeight = 22.sp)
                }
                else -> {
                    Text("AUDIO", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Equalizer controls depend on the active Android audio session. The full-band EQ and bass boost panel will be added after device testing.", color = Muted, lineHeight = 22.sp)
                }
            }
            selectedTrack?.let { track ->
                Spacer(Modifier.height(12.dp))
                NowPlayingMini(track)
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String, action: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.LibraryMusic, null, tint = Purple, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(14.dp))
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        Button(onClick = action, colors = ButtonDefaults.buttonColors(containerColor = Purple)) { Text("Try again") }
    }
}

@Composable
private fun TrackRow(track: MusicTrack, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SurfaceDark)
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF30203F)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.MusicNote, null, tint = Purple)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Default.PlayCircle, null, tint = Purple, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun NowPlayingMini(track: MusicTrack) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFF281A35)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.GraphicEq, null, tint = Purple, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = Muted, fontSize = 12.sp)
        }
        Text("PLAYING", color = Purple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
