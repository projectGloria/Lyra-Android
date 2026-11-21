package com.lyra.sona.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.lyra.sona.android.library.FOLDER_HINT
import com.lyra.sona.android.library.Track

private data class Tab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)
private val Tabs = listOf(
    Tab("home", "Home", Icons.Outlined.Home, Icons.Filled.Home),
    Tab("search", "Search", Icons.Outlined.Search, Icons.Filled.Search),
    Tab("library", "Your Library", Icons.Outlined.LibraryMusic, Icons.Filled.LibraryMusic),
    Tab("settings", "Settings", Icons.Outlined.Settings, Icons.Filled.Settings),
)

/** Everything a screen needs to browse and start playback. */
class LyraActions(val model: LyraViewModel, val navigate: (String) -> Unit, val back: () -> Unit, val choose: () -> Unit,
    private val source: MutableState<String>) {
    fun play(tracks: List<Track>, index: Int = 0, from: String = "") { source.value = from; model.play(tracks, index) }
    val playingFrom: String get() = source.value
}

@Composable
fun LyraApp(model: LyraViewModel, openPicker: () -> Unit) {
    MaterialTheme(colorScheme = LyraColors) {
        val nav = rememberNavController()
        val backStack by nav.currentBackStackEntryAsState()
        val route = backStack?.destination?.route ?: "home"
        val library by model.library.snapshot.collectAsStateWithLifecycle()
        val tree by model.library.tree.collectAsStateWithLifecycle()
        val progress by model.library.progress.collectAsStateWithLifecycle()
        val scanError by model.library.error.collectAsStateWithLifecycle()
        val selectionError by model.selectionError.collectAsStateWithLifecycle()
        val playbackMessage by model.graph.playbackMessage.collectAsStateWithLifecycle()
        val playback by model.playback.collectAsStateWithLifecycle()
        val selecting by model.selecting.collectAsStateWithLifecycle()
        val collections = remember(library) { buildCollections(library) }
        var showHint by rememberSaveable { mutableStateOf(false) }
        val source = rememberSaveable { mutableStateOf("") }
        val actions = remember(model, nav) {
            LyraActions(model, navigate = { target -> nav.navigate(target) { launchSingleTop = true } }, back = { nav.popBackStack() },
                choose = { showHint = true }, source = source)
        }
        val browsing = route in Tabs.map { it.route } || route.startsWith("collection/")
        Scaffold(
            bottomBar = {
                Column(Modifier.background(Brush.verticalGradient(listOf(Color.Transparent, Background.copy(alpha = 0.92f), Background)))) {
                    if (playback.current != null && route != "player") MiniPlayer(playback, model) { actions.navigate("player") }
                    if (browsing) BottomTabs(route) { target ->
                        nav.navigate(target) { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true }
                    }
                }
            }, containerColor = Background, contentWindowInsets = WindowInsets(0)
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
                val message = selectionError.ifEmpty { scanError }.ifEmpty { playbackMessage }
                if (message.isNotEmpty()) MessageCard(message, onChoose = if (selectionError.isNotEmpty() || scanError.isNotEmpty()) actions.choose else null) {
                    model.selectionError.value = ""; model.library.error.value = ""; model.graph.playbackMessage.value = ""
                }
                if (progress.running) ScanBanner(progress.label, progress.processed, progress.total) { model.library.cancelScan() }
                NavHost(nav, startDestination = "home", modifier = Modifier.weight(1f)) {
                    composable("home") { HomeScreen(library, collections, tree.isNotEmpty(), selecting, progress.running, playback, actions) }
                    composable("search") { SearchScreen(library, collections, playback, actions) }
                    composable("library") { LibraryScreen(collections, tree.isNotEmpty(), progress.running, actions) }
                    composable("settings") { SettingsScreen(tree, model, actions.choose) }
                    composable("collection/{kind}/{key}", arguments = listOf(navArgument("kind") { type = NavType.StringType },
                        navArgument("key") { type = NavType.StringType })) { entry ->
                        val kind = entry.arguments?.getString("kind").orEmpty()
                        val key = entry.arguments?.getString("key").orEmpty()
                        CollectionScreen(collections.find(kind, key), playback, actions)
                    }
                    composable("player") { PlayerScreen(playback, actions) { actions.navigate("queue") } }
                    composable("queue") {
                        LaunchedEffect(playback.explicitQueue.isEmpty()) { if (playback.explicitQueue.isEmpty()) nav.popBackStack() }
                        QueueScreen(playback, actions)
                    }
                }
            }
        }
        if (showHint) AlertDialog(onDismissRequest = { showHint = false }, title = { Text("Choose your music folder") },
            text = { Text(FOLDER_HINT) }, confirmButton = { TextButton(onClick = { showHint = false; openPicker() }) { Text("Open folder picker") } },
            dismissButton = { TextButton(onClick = { showHint = false }) { Text("Cancel") } }, containerColor = SurfaceHigh)
    }
}

@Composable
private fun BottomTabs(route: String, select: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 6.dp, bottom = 4.dp)) {
        Tabs.forEach { tab ->
            val selected = route == tab.route || (tab.route == "library" && route.startsWith("collection/"))
            Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Tab) { select(tab.route) }.padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(if (selected) tab.selectedIcon else tab.icon, null, tint = if (selected) OnSurface else Muted, modifier = Modifier.size(26.dp))
                Spacer(Modifier.height(3.dp))
                Text(tab.label, fontSize = 11.sp, color = if (selected) OnSurface else Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ScanBanner(label: String, processed: Int, total: Int, cancel: () -> Unit) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(if (total > 0) "$label · $processed/$total" else label, style = MaterialTheme.typography.labelMedium, color = Muted)
            Spacer(Modifier.height(6.dp))
            if (total > 0) LinearProgressIndicator(progress = { processed.toFloat() / total }, modifier = Modifier.fillMaxWidth(), trackColor = SurfaceHigh)
            else LinearProgressIndicator(Modifier.fillMaxWidth(), trackColor = SurfaceHigh)
        }
        Control(Icons.Outlined.Close, "Cancel scan", tint = Muted, onClick = cancel)
    }
}

@Composable
private fun MessageCard(message: String, onChoose: (() -> Unit)?, dismiss: () -> Unit) {
    Surface(color = Color(0xFF3A2E12), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(message, style = MaterialTheme.typography.bodySmall)
                if (onChoose != null) TextButton(onClick = onChoose, contentPadding = PaddingValues(0.dp)) { Text("Choose another folder", color = Amber) }
            }
            Control(Icons.Outlined.Close, "Dismiss message", onClick = dismiss)
        }
    }
}
