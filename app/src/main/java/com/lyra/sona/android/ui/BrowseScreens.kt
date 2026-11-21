package com.lyra.sona.android.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lyra.sona.android.LyraApplication
import com.lyra.sona.android.library.FOLDER_HINT
import com.lyra.sona.android.library.LibrarySnapshot
import java.util.Calendar

private val BottomSpace = 24.dp

@Composable
private fun RecordLibraryVisible(library: LibrarySnapshot) {
    val graph = LocalContext.current.applicationContext as LyraApplication
    LaunchedEffect(library.tracks.isNotEmpty()) { if (library.tracks.isNotEmpty()) graph.performance.cachedLibraryVisible(library.tracks.size) }
}

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning"; in 12..17 -> "Good afternoon"; else -> "Good evening"
}

private fun MusicCollection.caption(): String = when (kind) {
    CollectionKind.Playlist -> "Playlist • $subtitle"
    CollectionKind.Album -> "Album • $subtitle"
    CollectionKind.Artist -> "Artist • $subtitle"
}

// ---------------------------------------------------------------- Home

@Composable
fun HomeScreen(library: LibrarySnapshot, collections: LibraryCollections, selected: Boolean, selecting: Boolean, scanning: Boolean,
    playback: PlaybackUi, actions: LyraActions) {
    RecordLibraryVisible(library)
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    val playingId = playback.current?.mediaId
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = BottomSpace)) {
        item {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                LogoAvatar(36) { actions.navigate("settings") }
                Spacer(Modifier.width(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 16.dp)) {
                    item { Pill("All", filter == null) { filter = null } }
                    items(CollectionKind.entries.toList()) { kind -> Pill("${kind.label}s", filter == kind.name) { filter = if (filter == kind.name) null else kind.name } }
                }
            }
        }
        if (!selected) item { Onboarding(selecting, actions.choose) }
        if (selected && !scanning && library.tracks.isEmpty()) item {
            Column(Modifier.padding(16.dp)) {
                Text("No music found yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Add audio to this folder, refresh, or choose another folder.", color = Muted)
                TextButton(onClick = actions.choose, contentPadding = PaddingValues(0.dp)) { Text("Choose a different folder", color = Amber) }
            }
        }
        if (library.tracks.isNotEmpty()) {
            val shortcuts = when (filter) {
                CollectionKind.Playlist.name -> collections.playlists
                CollectionKind.Album.name -> collections.albums
                CollectionKind.Artist.name -> collections.artists
                else -> (collections.playlists + collections.albums.sortedByDescending { it.latest }).take(8)
            }
            if (filter == null) item {
                Text(greeting(), Modifier.padding(start = 16.dp, top = 16.dp, bottom = 12.dp), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            } else item { Spacer(Modifier.height(12.dp)) }
            items(shortcuts.chunked(2), key = { row -> "shortcut-" + row.joinToString { it.route } }) { row ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { ShortcutTile(it, playingId != null && it.tracks.any { t -> t.id == playingId }, Modifier.weight(1f)) { actions.navigate(it.route) } }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            if (filter == null) {
                val recent = (collections.albums.ifEmpty { collections.playlists.filter { !it.pinned } }).sortedByDescending { it.latest }.take(12)
                if (recent.isNotEmpty()) {
                    item { SectionHeader("Recently added", "Show all") { actions.navigate("library") } }
                    item { Carousel(recent, 150, actions) }
                }
                val jump = collections.playlists.filter { !it.pinned }.sortedByDescending { it.latest }.take(10)
                if (jump.isNotEmpty()) {
                    item { SectionHeader("Jump back in") }
                    item { Carousel(jump, 200, actions) }
                }
                if (collections.artists.size > 1) {
                    item { SectionHeader("Your artists") }
                    item { Carousel(collections.artists.sortedByDescending { it.tracks.size }.take(12), 130, actions) }
                }
            }
        }
    }
}

@Composable
private fun Onboarding(selecting: Boolean, choose: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(12.dp))
        .background(Brush.verticalGradient(listOf(Color(0xFF4A3608), Surface))).padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(Icons.Outlined.FolderOpen, null, tint = Amber, modifier = Modifier.size(44.dp))
        Text("Bring your library", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Each folder with audio becomes a playlist. Tracks directly in your chosen folder appear in Liked Songs.", color = Muted)
        Text(FOLDER_HINT, style = MaterialTheme.typography.bodyMedium, color = Muted)
        Button(onClick = choose, enabled = !selecting, colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Color.Black)) {
            Text(if (selecting) "Checking access…" else "Choose music folder", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ShortcutTile(collection: MusicCollection, playing: Boolean, modifier: Modifier, open: () -> Unit) {
    Row(modifier.height(56.dp).clip(RoundedCornerShape(6.dp)).background(SurfaceHigh).clickable(onClick = open), verticalAlignment = Alignment.CenterVertically) {
        CollectionArt(collection, Modifier.size(56.dp).let { if (collection.kind == CollectionKind.Artist) it.padding(4.dp) else it })
        Text(collection.title, Modifier.weight(1f).padding(horizontal = 10.dp), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
            maxLines = 2, overflow = TextOverflow.Ellipsis, color = if (playing) Amber else OnSurface)
    }
}

@Composable
private fun Carousel(entries: List<MusicCollection>, size: Int, actions: LyraActions) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(entries, key = { it.route }) { collection ->
            val artist = collection.kind == CollectionKind.Artist
            Column(Modifier.width(size.dp).clickable { actions.navigate(collection.route) },
                horizontalAlignment = if (artist) Alignment.CenterHorizontally else Alignment.Start) {
                CollectionArt(collection, Modifier.size(size.dp), large = size > 160)
                Spacer(Modifier.height(8.dp))
                Text(collection.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (artist) "Artist" else collection.caption(), color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ---------------------------------------------------------------- Your Library

@Composable
fun LibraryScreen(collections: LibraryCollections, selected: Boolean, scanning: Boolean, actions: LyraActions) {
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var alphabetical by rememberSaveable { mutableStateOf(false) }
    var grid by rememberSaveable { mutableStateOf(false) }
    val shown = remember(collections, filter, alphabetical) {
        val pool = when (filter) {
            CollectionKind.Playlist.name -> collections.playlists
            CollectionKind.Album.name -> collections.albums
            CollectionKind.Artist.name -> collections.artists
            else -> collections.playlists + collections.albums
        }
        val order = if (alphabetical) compareByDescending<MusicCollection> { it.pinned }.thenBy { it.title.lowercase() }
            else compareByDescending<MusicCollection> { it.pinned }.thenByDescending { it.latest }
        pool.sortedWith(order)
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            LogoAvatar(36) { actions.navigate("settings") }
            Spacer(Modifier.width(12.dp))
            Text("Your Library", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Control(Icons.Outlined.Search, "Search library", size = 26) { actions.navigate("search") }
            if (selected && !scanning) Control(Icons.Outlined.Refresh, "Refresh library", size = 26) { actions.model.refresh() }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (filter != null) item { Surface(onClick = { filter = null }, shape = CircleShape, color = SurfaceHigh) { Icon(Icons.Outlined.Close, "Clear filter", Modifier.padding(8.dp).size(18.dp)) } }
            items(CollectionKind.entries.toList()) { kind -> Pill("${kind.label}s", filter == kind.name) { filter = if (filter == kind.name) null else kind.name } }
        }
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { alphabetical = !alphabetical }, colors = ButtonDefaults.textButtonColors(contentColor = OnSurface)) {
                Icon(Icons.Outlined.SwapVert, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                Text(if (alphabetical) "Alphabetical" else "Recents", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.weight(1f))
            Control(if (grid) Icons.AutoMirrored.Outlined.List else Icons.Outlined.GridView, if (grid) "Show as list" else "Show as grid") { grid = !grid }
        }
        if (shown.isEmpty()) Text(if (selected) "Nothing here yet." else "Choose a music folder from Home to build your library.",
            Modifier.padding(16.dp), color = Muted)
        LazyVerticalGrid(GridCells.Fixed(if (grid) 3 else 1), Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = BottomSpace),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(if (grid) 16.dp else 12.dp)) {
            items(shown, key = { it.route }) { collection ->
                if (grid) Column(Modifier.clickable { actions.navigate(collection.route) },
                    horizontalAlignment = if (collection.kind == CollectionKind.Artist) Alignment.CenterHorizontally else Alignment.Start) {
                    CollectionArt(collection, Modifier.fillMaxWidth().aspectRatio(1f))
                    Spacer(Modifier.height(6.dp))
                    Text(collection.title, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(collection.kind.label, style = MaterialTheme.typography.labelSmall, color = Muted, maxLines = 1)
                } else Row(Modifier.fillMaxWidth().clickable { actions.navigate(collection.route) }, verticalAlignment = Alignment.CenterVertically) {
                    CollectionArt(collection, Modifier.size(64.dp))
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(collection.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (collection.pinned) { Icon(Icons.Filled.PushPin, "Pinned", tint = Amber, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)) }
                            Text(collection.caption(), color = Muted, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Search

private val BrowseColors = listOf(0xFFE13300, 0xFF8D67AB, 0xFF1E3264, 0xFF148A08, 0xFFBA5D07, 0xFF477D95, 0xFFAF2896, 0xFF503750, 0xFF0D73EC, 0xFFB49BC8)

@Composable
fun SearchScreen(library: LibrarySnapshot, collections: LibraryCollections, playback: PlaybackUi, actions: LyraActions) {
    var query by rememberSaveable { mutableStateOf("") }
    val normalized = query.trim()
    val tracks = remember(library, normalized) { if (normalized.isBlank()) emptyList() else library.tracks.filter { it.matchesSearch(normalized) } }
    val found = remember(collections, normalized) { if (normalized.isBlank()) emptyList() else collections.all.filter { it.title.contains(normalized, ignoreCase = true) } }
    val genres = remember(library) { library.tracks.map { it.genre.trim() }.filter { it.isNotEmpty() }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key }.take(16) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.statusBarsPadding().padding(start = 16.dp, top = 14.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            LogoAvatar(36) { actions.navigate("settings") }
            Spacer(Modifier.width(12.dp))
            Text("Search", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        TextField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp), placeholder = { Text("Search your music", color = Color(0xFF555555)) },
            leadingIcon = { Icon(Icons.Outlined.Search, null, tint = Color.Black) },
            trailingIcon = { if (query.isNotEmpty()) Control(Icons.Outlined.Close, "Clear search", tint = Color.Black) { query = "" } },
            singleLine = true, shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black, cursorColor = Color.Black, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
        if (normalized.isBlank()) {
            if (genres.isEmpty()) Text("Find tracks, artists, albums, genres, or playlists.", Modifier.padding(16.dp), color = Muted)
            else {
                Text("Browse your genres", Modifier.padding(start = 16.dp, top = 22.dp, bottom = 12.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                LazyVerticalGrid(GridCells.Fixed(2), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = BottomSpace),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(genres.size) { index ->
                        Box(Modifier.height(96.dp).clip(RoundedCornerShape(8.dp)).background(Color(BrowseColors[index % BrowseColors.size]))
                            .clickable { query = genres[index] }.padding(12.dp)) {
                            Text(genres[index], style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        } else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = BottomSpace)) {
            if (tracks.isEmpty() && found.isEmpty()) item { Text("No matches for “$normalized”.", Modifier.padding(8.dp), color = Muted) }
            if (found.isNotEmpty()) item { SectionTitle("Playlists, albums & artists") }
            items(found, key = { "c-" + it.route }) { collection ->
                Row(Modifier.fillMaxWidth().clickable { actions.navigate(collection.route) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    CollectionArt(collection, Modifier.size(52.dp))
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(collection.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(collection.caption(), color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            if (tracks.isNotEmpty()) item { SectionTitle("Songs · ${tracks.size}") }
            items(tracks, key = { it.id }) { track ->
                TrackRow(track, actions.model, playback.current?.mediaId == track.id, playback.playing) { actions.play(tracks, tracks.indexOf(track), "Search") }
            }
        }
    }
}

// ---------------------------------------------------------------- Settings

@Composable
fun SettingsScreen(tree: String, model: LyraViewModel, choose: () -> Unit) {
    val context = LocalContext.current
    var notifications by remember { mutableStateOf(Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifications = it }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notifications = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
    val metrics by model.library.metrics.collectAsStateWithLifecycle()
    val amberButton = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Color.Black)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = BottomSpace), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(Modifier.statusBarsPadding().padding(top = 14.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                LogoAvatar(56)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Lyra · offline music player", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            SettingsCard("Music folder", Icons.Outlined.Folder) {
                Text(if (tree.isBlank()) "No folder selected" else "Your chosen folder is connected through Android’s file picker.", color = Muted)
                Text(FOLDER_HINT, color = Muted, style = MaterialTheme.typography.bodySmall)
                Button(onClick = choose, colors = amberButton) { Text(if (tree.isBlank()) "Choose music folder" else "Choose another folder", fontWeight = FontWeight.Bold) }
                if (tree.isNotEmpty()) OutlinedButton(onClick = { model.refresh() }) { Text("Refresh library", color = OnSurface) }
            }
        }
        item {
            SettingsCard("Notifications", Icons.Outlined.Notifications) {
                Text(if (notifications) "Notification permission enabled." else "General notifications are disabled. Playback and media-session controls remain available.", color = Muted)
                if (Build.VERSION.SDK_INT >= 33 && !notifications) OutlinedButton(onClick = { permission.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Enable notifications", color = OnSurface) }
                TextButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                }, contentPadding = PaddingValues(0.dp)) { Text("Open notification settings", color = Amber) }
                Text("Declining notification permission does not prevent listening.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            SettingsCard("About Lyra", Icons.Outlined.Info) {
                Text("An offline home for your music. No accounts, network access, or changes to your music files.", color = Muted)
                Text("Liked Songs contains the tracks directly in your music folder; it is not a favorites list.", color = Muted, style = MaterialTheme.typography.bodySmall)
                Text("Version ${com.lyra.sona.android.BuildConfig.VERSION_NAME} · Android 8+", color = Amber)
            }
        }
        metrics?.let { scan -> item {
            SettingsCard("Last scan · this device", Icons.Outlined.Speed) {
                Text("${scan.discovered} tracks · ${scan.elapsedMs} ms\n${scan.metadataReads} metadata reads · ${scan.reused} reused", color = Muted)
                Text("Actual timings depend on this device and storage provider.", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        } }
    }
}

@Composable
private fun SettingsCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Amber, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        content()
    }
}
