# AGENTS.md — Lyra Android

Lyra is an offline Kotlin/Compose music player with a Storage Access Framework (SAF) library and Media3 background playback. It runs on Android API 26 and newer.

## Commands

- Run commands from the repository root in PowerShell. The Gradle wrapper uses 8.13, the build needs Android SDK Platform 36 and a JDK 21 toolchain, and app bytecode targets Java 17.
- Debug APK: `./gradlew.bat :app:assembleDebug`
- Host tests, including Robolectric and Compose UI tests: `./gradlew.bat :app:testDebugUnitTest`
- Single test class: `./gradlew.bat :app:testDebugUnitTest --tests 'com.lyra.sona.android.LibraryTest'`
- Android lint: `./gradlew.bat :app:lintDebug`
- Device scan benchmark, with the fixture tree already selected: `./gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.lyra.sona.android.DeviceScanBenchmarkTest`

## Architecture

- `app/src/main/java/com/lyra/sona/android/MainActivity.kt` hosts Compose, launches the SAF folder picker, forwards the URI and grant flags, and requests a refresh on start.
- `app/src/main/java/com/lyra/sona/android/LyraApplication.kt` owns the Room, DataStore, document, and artwork dependencies and records scan metrics.
- `app/src/main/java/com/lyra/sona/android/ui/` renders library, search, and playback state. LyraViewModel handles folder selection and talks to the service through a MediaController.
- `app/src/main/java/com/lyra/sona/android/library/LibraryRepository.kt` exposes cached Room snapshots and coordinates scan cancellation, publication, and selection changes.
- `app/src/main/java/com/lyra/sona/android/library/ScanEngine.kt` orchestrates traversal and incremental metadata work through DocumentSource and MetadataSource. AndroidDocuments implements the platform I/O.
- `app/src/main/java/com/lyra/sona/android/playback/` owns the ExoPlayer, MediaSession, queue operations, and failure policy. PlaybackService restores saved track IDs against the current Room library.
- `app/src/main/java/com/lyra/sona/android/data/` stores the selected tree and playback snapshot in DataStore and logs scan and cached-library visibility timings.

## Hard Rules & Gotchas

- **Offline and privacy.** Preserve the manifest removals for INTERNET and ACCESS_NETWORK_STATE and keep app backup disabled.
- **Folder grants.** Use persisted read grants for SAF tree URIs. Keep access to the previous tree until the new tree has scanned and committed successfully.
- **Scan publication.** Treat null cursors, provider errors, and loading states as failed listings. Preserve cached tracks and folders on failed or cancelled scans, read and replace both tables transactionally, and prevent superseded scans from publishing.
- **Track identity and cache reuse.** Derive stable IDs from the tree URI and document ID. Reuse metadata only for matching filename, size, and positive modification time, a current metadata version, a successful prior read, and any referenced artwork files still present.
- **Artwork.** Persist intrinsic track artwork and resolve album and folder fallbacks when building library snapshots. Keep current playback artwork protected from eviction.
- **Room upgrades.** Keep the registered v1-to-v2 artwork migration. Existing rows receive metadata version zero so they can be refreshed.
- **Session access.** Keep the service discoverable by system controllers. Accept the verified app or trusted controllers, grant standard commands explicitly in onConnectAsync, and reserve enqueue and move commands for the verified app.
- **Playback errors.** Reset the failure counter only after playback position advances on the same item. A READY state alone can occur before an unreadable track fails.
- **Playback restoration.** Restore paused, reconcile missing tracks, and preserve the distinction between pending explicit queue entries and the rest of the player timeline.

## Done Criteria

- For app code changes, run `./gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` and report any checks you could not run.
- For scan performance, follow `benchmark/README.md` with a connected phone. Report synthetic host results separately from device scan and cached-library visibility timings.
- Validate system media controls and audio interruptions on a real device. The scanner instrumentation covers first and incremental SAF scans.
