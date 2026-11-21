package com.lyra.sona.android.ui

import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.lyra.sona.android.R
import java.io.File

// Lyra palette: near-black surfaces with the amber of the Lyra icon as the single accent.
val Amber = Color(0xFFF5B301)
val AmberDeep = Color(0xFFB86E00)
val Background = Color(0xFF121212)
val Surface = Color(0xFF1F1F1F)
val SurfaceHigh = Color(0xFF2A2A2A)
val OnSurface = Color(0xFFFFFFFF)
val Muted = Color(0xFFB3B3B3)
val Subtle = Color(0xFF7A7A7A)

val LyraColors = darkColorScheme(primary = Amber, onPrimary = Color.Black, secondary = Amber, onSecondary = Color.Black,
    background = Background, onBackground = OnSurface, surface = Surface, onSurface = OnSurface,
    surfaceVariant = SurfaceHigh, onSurfaceVariant = Muted, surfaceContainer = Surface, surfaceContainerHigh = SurfaceHigh,
    surfaceContainerHighest = SurfaceHigh, outline = Color(0xFF3E3E3E), outlineVariant = Color(0xFF2E2E2E))

fun artworkData(path: String): Any = if (path.startsWith("file:") || path.startsWith("content:")) Uri.parse(path) else File(path)

@Composable
fun Artwork(path: String, description: String, modifier: Modifier, shape: Shape = RoundedCornerShape(4.dp),
    icon: ImageVector = Icons.Outlined.MusicNote, large: Boolean = false) {
    Box(modifier.clip(shape).background(SurfaceHigh), contentAlignment = Alignment.Center) {
        if (path.isEmpty()) Icon(icon, description, tint = Muted, modifier = Modifier.fillMaxSize(0.42f))
        else AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(artworkData(path)).size(if (large) 1024 else 256).crossfade(true).build(),
            contentDescription = description, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
            error = rememberVectorPainterFallback())
    }
}

@Composable private fun rememberVectorPainterFallback() = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Outlined.MusicNote)

/** Liked Songs tile: the amber of the launcher icon behind a heart. */
@Composable
fun LikedArt(modifier: Modifier, shape: Shape = RoundedCornerShape(4.dp)) {
    Box(modifier.clip(shape).background(Brush.linearGradient(listOf(Amber, AmberDeep, Color(0xFF5A2A00)))), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.Favorite, "Liked Songs", tint = Color.White, modifier = Modifier.fillMaxSize(0.4f))
    }
}

@Composable
fun CollectionArt(collection: MusicCollection, modifier: Modifier, large: Boolean = false) {
    val shape = if (collection.kind == CollectionKind.Artist) CircleShape else RoundedCornerShape(if (large) 6.dp else 4.dp)
    when {
        collection.pinned -> LikedArt(modifier, shape)
        collection.kind == CollectionKind.Playlist && collection.mosaic.size == 4 -> Column(modifier.clip(shape)) {
            collection.mosaic.chunked(2).forEach { row ->
                Row(Modifier.weight(1f)) { row.forEach { Artwork(it, collection.title, Modifier.weight(1f).fillMaxHeight(), RoundedCornerShape(0.dp), large = large) } }
            }
        }
        else -> Artwork(collection.artwork, "${collection.title} artwork", modifier, shape,
            icon = if (collection.kind == CollectionKind.Artist) Icons.Outlined.Person else Icons.Outlined.MusicNote, large = large)
    }
}

/** The Lyra icon used as the profile avatar in screen headers. */
@Composable
fun LogoAvatar(size: Int = 36, onClick: (() -> Unit)? = null) {
    Image(painterResource(R.drawable.lyra_logo), "Lyra", contentScale = ContentScale.Crop,
        modifier = Modifier.size(size.dp).clip(CircleShape).let { if (onClick != null) it.clickable(onClick = onClick) else it })
}

@Composable
fun Control(icon: ImageVector, label: String, enabled: Boolean = true, tint: Color = OnSurface, size: Int = 24, onClick: () -> Unit) {
    IconButton(onClick, Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp), enabled,
        colors = IconButtonDefaults.iconButtonColors(contentColor = tint, disabledContentColor = tint.copy(alpha = 0.38f))) {
        Icon(icon, label, Modifier.size(size.dp))
    }
}

@Composable
fun Pill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = if (selected) Amber else SurfaceHigh, contentColor = if (selected) Color.Black else OnSurface) {
        Text(label, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 24.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (action != null) TextButton(onClick = onAction, colors = ButtonDefaults.textButtonColors(contentColor = Muted)) { Text(action, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
fun SectionTitle(title: String) { Text(title, Modifier.padding(8.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Amber) }

/**
 * A dark, readable tint taken from the artwork so headers and the player follow the cover,
 * the way the design screenshots do. Falls back to [fallback] without artwork.
 */
@Composable
fun rememberArtColor(path: String, fallback: Color = Color(0xFF3A2C0A)): Color {
    val context = LocalContext.current
    var color by remember(path) { mutableStateOf(fallback) }
    LaunchedEffect(path) {
        if (path.isEmpty()) return@LaunchedEffect
        val result = runCatching {
            context.imageLoader.execute(ImageRequest.Builder(context).data(artworkData(path)).size(32).allowHardware(false).build())
        }.getOrNull() ?: return@LaunchedEffect
        val bitmap = (result.drawable as? BitmapDrawable)?.bitmap ?: return@LaunchedEffect
        color = Color(dominantDarkColor(IntArray(bitmap.width * bitmap.height).also { bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height) }))
    }
    return animateColorAsState(color, tween(450), label = "artColor").value
}

/** Saturation-weighted average hue, clamped dark enough for white text. */
fun dominantDarkColor(pixels: IntArray): Int {
    var r = 0.0; var g = 0.0; var b = 0.0; var weight = 0.0
    val hsv = FloatArray(3)
    for (pixel in pixels) {
        if ((pixel ushr 24) < 128) continue
        android.graphics.Color.colorToHSV(pixel, hsv)
        val w = 0.08 + hsv[1] * hsv[2]
        r += ((pixel shr 16) and 0xFF) * w; g += ((pixel shr 8) and 0xFF) * w; b += (pixel and 0xFF) * w; weight += w
    }
    if (weight == 0.0) return 0xFF3A2C0A.toInt()
    android.graphics.Color.RGBToHSV((r / weight).toInt(), (g / weight).toInt(), (b / weight).toInt(), hsv)
    hsv[1] = (hsv[1] * 1.15f).coerceAtMost(0.85f)
    hsv[2] = hsv[2].coerceIn(0.22f, 0.42f)
    return android.graphics.Color.HSVToColor(hsv)
}
