package com.lyra.sona.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.lyra.sona.android.library.Track

private val MediaItem.title get() = mediaMetadata.title?.toString().orEmpty()
private val MediaItem.artist get() = mediaMetadata.artist?.toString().orEmpty()
private val MediaItem.thumb get() = mediaMetadata.artworkUri?.toString().orEmpty()

@Composable
private fun RoundBack(label: String = "Back", onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = Color.Black.copy(alpha = 0.35f), modifier = Modifier.size(40.dp)) {
        Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, label, Modifier.size(22.dp)) }
    }
}

// ---------------------------------------------------------------- Playlist / album / artist

@Composable
fun CollectionScreen(collection: MusicCollection?, playback: PlaybackUi, actions: LyraActions) {
    if (collection == null) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            RoundBack(onClick = actions.back)
            Text("This collection is no longer in your library.", color = Muted)
        }
        return
    }
    val tracks = collection.tracks
    val tint = rememberArtColor(collection.artwork, if (collection.pinned) Color(0xFF6B4A05) else Color(0xFF3A2C0A))
    val playingHere = playback.current?.mediaId?.let { id -> tracks.any { it.id == id } } == true
    var menu by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(tint, tint.copy(alpha = 0.55f), Background)))
                .statusBarsPadding().padding(horizontal = 16.dp)) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundBack(onClick = actions.back)
                    Spacer(Modifier.weight(1f))
                    Box {
                        Control(Icons.Outlined.MoreVert, "Actions for ${collection.title}") { menu = true }
                        DropdownMenu(menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Add to queue") }, enabled = playback.ready && tracks.isNotEmpty(),
                                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.QueueMusic, null) }, onClick = { actions.model.enqueue(tracks); menu = false })
                            DropdownMenuItem(text = { Text("Play next") }, enabled = playback.ready && tracks.isNotEmpty(),
                                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.PlaylistPlay, null) }, onClick = { actions.model.enqueue(tracks, next = true); menu = false })
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                CollectionArt(collection, Modifier.align(Alignment.CenterHorizontally).fillMaxWidth(0.66f).widthIn(max = 320.dp).aspectRatio(1f), large = true)
                Spacer(Modifier.height(20.dp))
                Text(collection.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LogoAvatar(22); Spacer(Modifier.width(8.dp))
                    Text(when (collection.kind) { CollectionKind.Album -> collection.subtitle; else -> "Lyra" }, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                Text("${collection.kind.label} · ${tracks.size} ${songs(tracks.size)} · ${longTime(collection.durationMs)}", color = Muted, style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Control(Icons.AutoMirrored.Outlined.QueueMusic, "Add ${collection.title} to queue", playback.ready && tracks.isNotEmpty(), tint = Muted) { actions.model.enqueue(tracks) }
                    Spacer(Modifier.weight(1f))
                    Control(Icons.Outlined.Shuffle, if (playback.shuffle) "Shuffle on" else "Shuffle off", playback.ready, tint = if (playback.shuffle) Amber else Muted, size = 28) { actions.model.shuffle() }
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(onClick = {
                        if (playingHere) actions.model.toggle()
                        else actions.play(tracks, if (playback.shuffle) tracks.indices.random() else 0, collection.title)
                    }, enabled = playback.ready && tracks.isNotEmpty(), modifier = Modifier.size(56.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Amber, contentColor = Color.Black)) {
                        val pause = playingHere && playback.playing
                        Icon(if (pause) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (pause) "Pause ${collection.title}" else "Play ${collection.title}", Modifier.size(30.dp))
                    }
                }
            }
        }
        if (tracks.isEmpty()) item { Text("No songs here yet.", Modifier.padding(16.dp), color = Muted) }
        items(tracks.size, key = { tracks[it].id }) { index ->
            val track = tracks[index]
            Box(Modifier.padding(horizontal = 8.dp)) {
                TrackRow(track, actions.model, playback.current?.mediaId == track.id, playback.playing,
                    showArt = collection.kind != CollectionKind.Album) { actions.play(tracks, index, collection.title) }
            }
        }
    }
}

@Composable
fun TrackRow(track: Track, model: LyraViewModel, current: Boolean = false, playing: Boolean = false, showArt: Boolean = true, onPlay: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(6.dp)).clickable(onClick = onPlay).padding(start = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (showArt) Artwork(track.thumbnail, "${track.title} artwork", Modifier.size(48.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (current) { Icon(if (playing) Icons.Outlined.GraphicEq else Icons.AutoMirrored.Outlined.VolumeUp, "Now playing", tint = Amber, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)) }
                Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (current) Amber else OnSurface, style = MaterialTheme.typography.bodyLarge)
            }
            Text(track.artist, color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (track.metadataError.isNotEmpty()) Text("Metadata unavailable · playback can still be tried", color = Subtle, style = MaterialTheme.typography.labelSmall)
        }
        Text(time(track.durationMs), color = Muted, style = MaterialTheme.typography.labelSmall)
        Box {
            Control(Icons.Outlined.MoreVert, "Actions for ${track.title}", tint = Muted) { menu = true }
            DropdownMenu(menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Play next") }, onClick = { model.enqueue(listOf(track), next = true); menu = false })
                DropdownMenuItem(text = { Text("Add to queue") }, onClick = { model.enqueue(listOf(track)); menu = false })
            }
        }
    }
}

// ---------------------------------------------------------------- Mini player

@Composable
fun MiniPlayer(playback: PlaybackUi, model: LyraViewModel, expand: () -> Unit) {
    val item = playback.current ?: return
    val tint = rememberArtColor(item.thumb, SurfaceHigh)
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp).clip(RoundedCornerShape(8.dp)).background(tint).clickable(onClick = expand)) {
        Row(Modifier.padding(start = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Artwork(item.thumb, "Current track artwork", Modifier.size(42.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(item.artist, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
            }
            Control(if (playback.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (playback.playing) "Pause" else "Play", enabled = playback.ready, size = 28) { model.toggle() }
            Control(Icons.Outlined.SkipNext, "Next track", enabled = playback.ready) { model.next() }
        }
        val fraction = if (playback.duration > 0) (playback.position.toFloat() / playback.duration).coerceIn(0f, 1f) else 0f
        Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(2.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f))) {
            Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(Color.White))
        }
    }
}

// ---------------------------------------------------------------- Full player

@Composable
fun PlayerScreen(playback: PlaybackUi, actions: LyraActions, queue: () -> Unit) {
    val model = actions.model
    val item = playback.current
    val art = playback.currentArtwork.ifEmpty { item?.thumb.orEmpty() }
    val tint = rememberArtColor(art)
    LazyColumn(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(tint, tint.copy(alpha = 0.6f), Background))),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
        item {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Control(Icons.Outlined.KeyboardArrowDown, "Close player", size = 30, onClick = actions.back)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("NOW PLAYING", style = MaterialTheme.typography.labelSmall, color = Muted)
                    val from = actions.playingFrom.ifEmpty { item?.mediaMetadata?.albumTitle?.toString().orEmpty() }
                    if (from.isNotEmpty()) Text(from, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(48.dp))
            }
            Spacer(Modifier.height(28.dp))
        }
        item {
            Artwork(art, "Now playing artwork", Modifier.fillMaxWidth().widthIn(max = 440.dp).aspectRatio(1f), RoundedCornerShape(8.dp), large = true)
            Spacer(Modifier.height(28.dp))
            Text(item?.title?.ifEmpty { null } ?: "Nothing playing", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(item?.artist.orEmpty(), color = Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(12.dp))
        }
        item {
            var scrub by remember(playback.current?.mediaId, playback.currentIndex) { mutableStateOf<Float?>(null) }
            val max = playback.duration.coerceAtLeast(1).toFloat()
            Slider(value = (scrub ?: playback.position.toFloat()).coerceIn(0f, max), onValueChange = { scrub = it },
                onValueChangeFinished = { scrub?.let { model.seek(it.toLong()) }; scrub = null }, valueRange = 0f..max,
                enabled = playback.ready && playback.duration > 0, modifier = Modifier.semantics { contentDescription = "Playback position" },
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.25f)))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(time(scrub?.toLong() ?: playback.position), color = Muted, style = MaterialTheme.typography.labelSmall)
                Text(time(playback.duration), color = Muted, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Control(Icons.Outlined.Shuffle, if (playback.shuffle) "Shuffle on" else "Shuffle off", playback.ready, tint = if (playback.shuffle) Amber else OnSurface, size = 26) { model.shuffle() }
                Control(Icons.Outlined.SkipPrevious, "Previous track", playback.ready, size = 38) { model.previous() }
                FilledIconButton(onClick = { model.toggle() }, enabled = playback.ready && item != null, modifier = Modifier.size(72.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Color.Black)) {
                    Icon(if (playback.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (playback.playing) "Pause" else "Play", Modifier.size(38.dp))
                }
                Control(Icons.Outlined.SkipNext, "Next track", playback.ready, size = 38) { model.next() }
                Control(if (playback.repeat == Player.REPEAT_MODE_ONE) Icons.Outlined.RepeatOne else Icons.Outlined.Repeat,
                    when (playback.repeat) { Player.REPEAT_MODE_ONE -> "Repeat one"; Player.REPEAT_MODE_ALL -> "Repeat all"; else -> "Repeat off" }, playback.ready,
                    tint = if (playback.repeat == Player.REPEAT_MODE_OFF) OnSurface else Amber, size = 26) { model.repeat() }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (playback.explicitQueue.isNotEmpty()) TextButton(onClick = queue, colors = ButtonDefaults.textButtonColors(contentColor = OnSurface)) {
                    Icon(Icons.AutoMirrored.Outlined.QueueMusic, null); Spacer(Modifier.width(8.dp)); Text("Queue · ${playback.explicitQueue.size}")
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Queue

@Composable
fun QueueScreen(playback: PlaybackUi, actions: LyraActions) {
    val model = actions.model
    val order = playback.explicitQueue
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 4.dp, end = 16.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Control(Icons.Outlined.KeyboardArrowDown, "Back", size = 30, onClick = actions.back)
            Text(if (order.isEmpty()) "Your queue" else "Your queue · ${order.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        playback.current?.let { current ->
            Text("Now playing", Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Artwork(current.thumb, "Current track artwork", Modifier.size(48.dp))
                Column(Modifier.weight(1f)) {
                    Text(current.title, color = Amber, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(current.artist, color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (order.isNotEmpty()) Text("Next in queue", Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 12.dp, end = 4.dp, bottom = 24.dp)) {
            items(order.size, key = { "${playback.queue[order[it]].mediaId}:${order[it]}" }) { displayIndex ->
                val index = order[displayIndex]
                val item = playback.queue[index]
                var menu by remember { mutableStateOf(false) }
                Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(6.dp)).clickable { model.jump(index) }.padding(start = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Artwork(item.thumb, "${item.title} artwork", Modifier.size(48.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (index == playback.currentIndex) Amber else OnSurface)
                        Text(item.artist, color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Box {
                        Control(Icons.Outlined.MoreVert, "Queue actions for ${item.mediaMetadata.title}", tint = Muted) { menu = true }
                        DropdownMenu(menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Move up") }, enabled = displayIndex > 0, onClick = { model.move(index, order[displayIndex - 1]); menu = false })
                            DropdownMenuItem(text = { Text("Move down") }, enabled = displayIndex < order.lastIndex, onClick = { model.move(index, order[displayIndex + 1]); menu = false })
                            DropdownMenuItem(text = { Text("Remove from queue") }, onClick = { model.remove(index); menu = false })
                        }
                    }
                }
            }
        }
    }
}
