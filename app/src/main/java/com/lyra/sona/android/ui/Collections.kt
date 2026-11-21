package com.lyra.sona.android.ui

import com.lyra.sona.android.library.LibrarySnapshot
import com.lyra.sona.android.library.Track
import com.lyra.sona.android.library.naturalCompare
import com.lyra.sona.android.library.trackComparator

enum class CollectionKind(val label: String) { Playlist("Playlist"), Album("Album"), Artist("Artist") }

/** A browsable group of tracks derived from the cached library snapshot; never persisted. */
data class MusicCollection(val kind: CollectionKind, val key: String, val title: String, val subtitle: String,
    val tracks: List<Track>, val pinned: Boolean = false) {
    val route: String get() = "collection/${kind.name}/${android.net.Uri.encode(key)}"
    val artwork: String get() = tracks.firstOrNull { it.thumbnail.isNotEmpty() }?.thumbnail.orEmpty()
    /** Up to four distinct covers for playlist mosaics. */
    val mosaic: List<String> get() = tracks.map { it.thumbnail }.filter { it.isNotEmpty() }.distinct().take(4)
    val latest: Long get() = tracks.maxOfOrNull { it.modified ?: 0L } ?: 0L
    val durationMs: Long get() = tracks.sumOf { it.durationMs }
}

data class LibraryCollections(val playlists: List<MusicCollection> = emptyList(), val albums: List<MusicCollection> = emptyList(),
    val artists: List<MusicCollection> = emptyList()) {
    val all: List<MusicCollection> get() = playlists + albums + artists
    fun find(kind: String, key: String): MusicCollection? = when (kind) {
        CollectionKind.Playlist.name -> playlists; CollectionKind.Album.name -> albums; else -> artists
    }.firstOrNull { it.key == key }
}

private fun Track.artistKey() = albumArtist.ifBlank { artist }

fun buildCollections(library: LibrarySnapshot): LibraryCollections {
    val byFolder = library.tracks.groupBy { it.folderId }
    val playlists = library.folders.mapNotNull { folder ->
        val tracks = byFolder[folder.id].orEmpty()
        if (tracks.isEmpty() && !folder.isRoot) return@mapNotNull null
        MusicCollection(CollectionKind.Playlist, folder.id, folder.name, "${tracks.size} ${songs(tracks.size)}", tracks, pinned = folder.isRoot)
    }.sortedWith(compareByDescending<MusicCollection> { it.pinned }.thenComparator { a, b -> naturalCompare(a.title, b.title) })
    val albums = library.tracks.filter { it.album.isNotBlank() }
        .groupBy { it.artistKey().lowercase() + "|" + it.album.lowercase() }
        .map { (key, tracks) ->
            val sorted = tracks.sortedWith(trackComparator)
            MusicCollection(CollectionKind.Album, key, sorted.first().album, sorted.first().artistKey(), sorted)
        }.sortedWith { a, b -> naturalCompare(a.title, b.title) }
    val artists = library.tracks.groupBy { it.artistKey().lowercase() }
        .map { (key, tracks) ->
            val sorted = tracks.sortedWith(compareBy<Track> { it.album.lowercase() }.then(trackComparator))
            MusicCollection(CollectionKind.Artist, key, sorted.first().artistKey(), "${tracks.size} ${songs(tracks.size)}", sorted)
        }.sortedWith { a, b -> naturalCompare(a.title, b.title) }
    return LibraryCollections(playlists, albums, artists)
}

fun songs(count: Int) = if (count == 1) "song" else "songs"

fun time(ms: Long): String { val seconds = ms.coerceAtLeast(0) / 1000; return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}" }

fun longTime(ms: Long): String {
    val minutes = ms.coerceAtLeast(0) / 60000
    return if (minutes >= 60) "${minutes / 60} hr ${minutes % 60} min" else "$minutes min"
}
