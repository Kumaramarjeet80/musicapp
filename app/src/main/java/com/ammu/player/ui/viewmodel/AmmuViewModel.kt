package com.ammu.player.ui.viewmodel

import android.app.Application
import android.content.Context
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.ammu.player.audio.AmmuAudioController
import com.ammu.player.audio.dsp.DualEqualizerEngine
import com.ammu.player.audio.dsp.EqPreset
import com.ammu.player.audio.dsp.EqualizerMode
import com.ammu.player.data.backup.BackupExportManager
import com.ammu.player.data.backup.BackupPayload
import com.ammu.player.data.backup.DualPhaseImportManager
import com.ammu.player.data.backup.ExportPermissions
import com.ammu.player.data.backup.Phase1Result
import com.ammu.player.data.backup.SecurityKeySuite
import com.ammu.player.data.backup.StorageMatchResult
import com.ammu.player.data.db.AmmuDatabase
import com.ammu.player.data.model.LyricsEntity
import com.ammu.player.data.model.PlaylistEntity
import com.ammu.player.data.model.PlaylistTrackCrossRef
import com.ammu.player.data.model.SmartPlaylistType
import com.ammu.player.data.model.TimestampEntity
import com.ammu.player.data.model.TrackEntity
import com.ammu.player.data.model.TrimmedClipEntity
import com.ammu.player.data.security.SecurityManager
import com.ammu.player.data.storage.DuplicateGroup
import com.ammu.player.data.storage.StorageAuditSummary
import com.ammu.player.data.storage.StorageAuditor
import com.ammu.player.data.trimmer.AudioTrimmer
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@UnstableApi
class AmmuViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val db = AmmuDatabase.getInstance(application, viewModelScope)
    val securityManager = SecurityManager(application)
    val audioController = AmmuAudioController(application, viewModelScope)
    val equalizerEngine = DualEqualizerEngine(application) { autoGainFactor ->
        // Handle auto-gain volume adjustments
    }
    private val exportManager = BackupExportManager(application, db)
    private val importManager = DualPhaseImportManager(application, db)
    private val storageAuditor = StorageAuditor(application, db)
    private val audioTrimmer = AudioTrimmer(application, db)

    // UI State: Search & Clutter Filter
    val searchQuery = MutableStateFlow("")
    val isCleanClutterEnabled = MutableStateFlow(false)

    // Selected Playlist Filter
    val selectedPlaylistId = MutableStateFlow<Long?>(null) // null = ALL
    val playlists: StateFlow<List<PlaylistEntity>> = db.playlistDao().getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tracks Flow reacting to playlist filter & search query
    val displayedTracks: StateFlow<List<TrackEntity>> = combine(
        selectedPlaylistId,
        searchQuery,
        isCleanClutterEnabled
    ) { plId, query, cleanEnabled ->
        Triple(plId, query, cleanEnabled)
    }.flatMapLatest { (plId, query, _) ->
        when {
            query.isNotBlank() -> db.trackDao().searchTracks(query)
            plId == null || plId == 1L -> db.trackDao().getAllTracks()
            plId == 2L -> db.trackDao().getHeavyRotation()
            plId == 3L -> db.trackDao().getRecentlyAdded()
            plId == 4L -> db.trackDao().getUnplayed()
            else -> db.trackDao().getTracksForPlaylist(plId)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Multi-Select Batch Actions State
    val isMultiSelectMode = MutableStateFlow(false)
    val selectedTrackIds = MutableStateFlow<Set<Long>>(emptySet())
    private var recentlyDeletedTracks: List<TrackEntity> = emptyList()
    val undoSnackbarMessage = MutableStateFlow<String?>(null)

    // Favorite micro-interaction trigger (heart burst + emotional toast)
    val favoriteAnimTrigger = MutableStateFlow(0L)
    val lastFavoriteState = MutableStateFlow(false)

    // Drawers & Dialogs State
    val currentLyrics = MutableStateFlow<LyricsEntity?>(null)
    val currentTimestamps = MutableStateFlow<List<TimestampEntity>>(emptyList())
    val currentClips = MutableStateFlow<List<TrimmedClipEntity>>(emptyList())

    // Vinyl Disc Mode state
    val isVinylMode = MutableStateFlow(true)

    // Storage Auditor State
    val storageAuditResult = MutableStateFlow<StorageAuditSummary?>(null)
    val isAuditingStorage = MutableStateFlow(false)

    // Import / Export Suite State
    val phase1Result = MutableStateFlow<Phase1Result?>(null)
    val storageMatchResults = MutableStateFlow<List<StorageMatchResult>>(emptyList())

    init {
        // Track play count increment
        audioController.onTrackCompletedListener = { track ->
            viewModelScope.launch(Dispatchers.IO) {
                db.trackDao().incrementPlayCount(track.id)
            }
        }

        // Observe current track to update lyrics, timestamps, and clips
        viewModelScope.launch {
            audioController.currentTrack.collect { track ->
                if (track != null) {
                    db.lyricsDao().getLyricsForTrack(track.id).collect {
                        currentLyrics.value = it
                    }
                } else {
                    currentLyrics.value = null
                }
            }
        }

        viewModelScope.launch {
            audioController.currentTrack.collect { track ->
                if (track != null) {
                    db.timestampDao().getTimestampsForTrack(track.id).collect {
                        currentTimestamps.value = it
                    }
                } else {
                    currentTimestamps.value = emptyList()
                }
            }
        }

        viewModelScope.launch {
            audioController.currentTrack.collect { track ->
                if (track != null) {
                    db.trimmedClipDao().getClipsForTrack(track.id).collect {
                        currentClips.value = it
                    }
                } else {
                    currentClips.value = emptyList()
                }
            }
        }

        // Scan local storage on startup
        scanDeviceAudioFiles()
    }

    // Media Scanning
    fun scanDeviceAudioFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATE_ADDED
            )
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

            val foundTracks = mutableListOf<TrackEntity>()
            val contentResolver = context.contentResolver
            val cursor = contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val sizeCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                while (it.moveToNext()) {
                    val rawTitle = it.getString(titleCol) ?: "Unknown"
                    val path = it.getString(dataCol) ?: ""
                    val duration = it.getLong(durationCol)
                    if (path.isNotBlank() && duration > 5000L) {
                        val cleanTitle = cleanClutterFromTitle(rawTitle)
                        foundTracks.add(
                            TrackEntity(
                                title = rawTitle,
                                artist = it.getString(artistCol) ?: "Unknown Artist",
                                album = it.getString(albumCol) ?: "Unknown Album",
                                durationMs = duration,
                                path = path,
                                uriString = "${MediaStore.Audio.Media.EXTERNAL_CONTENT_URI}/${it.getLong(idCol)}",
                                sizeBytes = it.getLong(sizeCol),
                                dateAdded = it.getLong(dateCol) * 1000L,
                                clutterCleanTitle = cleanTitle
                            )
                        )
                    }
                }
            }

            for (track in foundTracks) {
                val existing = db.trackDao().getTrackByUri(track.uriString)
                if (existing == null) {
                    db.trackDao().insertTrack(track)
                }
            }
        }
    }

    // Clutter cleaner regex
    fun cleanClutterFromTitle(rawTitle: String): String {
        return rawTitle
            .replace(Regex("""(?i)\s*[\(\[]\s*(official\s*(music\s*)?video|video|audio|lyrics?|remastered|hd|4k|hq|visualizer)\s*[\)\]]"""), "")
            .replace(Regex("""(?i)\s*(ft\.|feat\.).*"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    fun toggleCleanClutter() {
        val next = !isCleanClutterEnabled.value
        isCleanClutterEnabled.value = next
    }

    // Playback Wrappers
    fun playTrack(track: TrackEntity) {
        audioController.playTrack(track, displayedTracks.value)
    }

    fun playPause() = audioController.playPause()
    fun skipNext() = audioController.skipToNext()
    fun skipPrevious() = audioController.skipToPrevious()
    fun seekTo(positionMs: Long) = audioController.seekTo(positionMs)
    fun seekRelative(deltaMs: Long) = audioController.seekRelative(deltaMs)
    fun toggleShuffle() = audioController.toggleShuffle()
    fun toggleRepeat() = audioController.toggleRepeatMode()
    fun setVolume(vol: Float) = audioController.setVolume(vol)

    // Favorite Toggle with Micro-Interaction Reaction
    fun toggleFavorite(track: TrackEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val newFav = !track.isFavorite
            db.trackDao().updateFavoriteStatus(track.id, newFav)
            if (newFav) {
                db.favoriteDao().insertFavorite(com.ammu.player.data.model.FavoriteEntity(track.id))
            } else {
                db.favoriteDao().deleteFavoriteByTrackId(track.id)
            }
            lastFavoriteState.value = newFav
            favoriteAnimTrigger.value = System.currentTimeMillis()
        }
    }

    // Multi-Select Batch Actions
    fun toggleTrackSelection(trackId: Long) {
        val current = selectedTrackIds.value.toMutableSet()
        if (current.contains(trackId)) {
            current.remove(trackId)
        } else {
            current.add(trackId)
        }
        selectedTrackIds.value = current
        isMultiSelectMode.value = current.isNotEmpty()
    }

    fun selectAll() {
        val allIds = displayedTracks.value.map { it.id }.toSet()
        selectedTrackIds.value = allIds
        isMultiSelectMode.value = allIds.isNotEmpty()
    }

    fun clearSelection() {
        selectedTrackIds.value = emptySet()
        isMultiSelectMode.value = false
    }

    fun batchAddToPlaylist(playlistId: Long) {
        val ids = selectedTrackIds.value.toList()
        viewModelScope.launch(Dispatchers.IO) {
            val refs = ids.mapIndexed { idx, trackId ->
                PlaylistTrackCrossRef(playlistId = playlistId, trackId = trackId, sortOrder = idx)
            }
            db.playlistDao().insertCrossRefs(refs)
            clearSelection()
        }
    }

    fun batchDeleteTracks() {
        val ids = selectedTrackIds.value.toList()
        viewModelScope.launch(Dispatchers.IO) {
            val tracksToDelete = displayedTracks.value.filter { it.id in ids }
            recentlyDeletedTracks = tracksToDelete
            db.trackDao().deleteTracksByIds(ids)
            clearSelection()
            undoSnackbarMessage.value = "Deleted ${tracksToDelete.size} tracks. Undo available (5s)"
            delay(5000)
            undoSnackbarMessage.value = null
        }
    }

    fun undoLastDelete() {
        viewModelScope.launch(Dispatchers.IO) {
            if (recentlyDeletedTracks.isNotEmpty()) {
                db.trackDao().insertTracks(recentlyDeletedTracks)
                recentlyDeletedTracks = emptyList()
                undoSnackbarMessage.value = null
            }
        }
    }

    // Custom Playlists
    fun createPlaylist(name: String, description: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val playlist = PlaylistEntity(
                name = name,
                description = description,
                isSmartPlaylist = false,
                smartType = SmartPlaylistType.CUSTOM
            )
            db.playlistDao().insertPlaylist(playlist)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            db.playlistDao().deletePlaylistById(playlistId)
            if (selectedPlaylistId.value == playlistId) {
                selectedPlaylistId.value = 1L
            }
        }
    }

    // Timestamps
    fun addTimestamp(label: String, colorHex: String = "#00E5FF") {
        val current = audioController.currentTrack.value ?: return
        val pos = audioController.positionMs.value
        viewModelScope.launch(Dispatchers.IO) {
            db.timestampDao().insertTimestamp(
                TimestampEntity(
                    trackId = current.id,
                    timestampMs = pos,
                    label = label.ifBlank { "Mark @ %d:%02d".format((pos / 1000) / 60, (pos / 1000) % 60) },
                    colorHex = colorHex
                )
            )
        }
    }

    fun deleteTimestamp(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            db.timestampDao().deleteTimestampById(id)
        }
    }

    // Lyrics & Notes
    fun saveLyricsAndNotes(plainLyrics: String, notes: String) {
        val current = audioController.currentTrack.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val entity = LyricsEntity(
                trackId = current.id,
                plainLyrics = plainLyrics,
                notes = notes,
                lastEditedAt = System.currentTimeMillis()
            )
            db.lyricsDao().insertLyrics(entity)
        }
    }

    // Real Audio Trimmer
    fun sliceAudioClip(title: String, startMs: Long, endMs: Long, onResult: (Result<TrimmedClipEntity>) -> Unit) {
        val current = audioController.currentTrack.value ?: return
        viewModelScope.launch {
            val result = audioTrimmer.trimAudio(
                parentTrackId = current.id,
                sourcePathOrUri = current.path,
                clipTitle = title,
                startMs = startMs,
                endMs = endMs
            )
            onResult(result)
        }
    }

    // Storage Auditor
    fun runStorageAudit() {
        isAuditingStorage.value = true
        viewModelScope.launch {
            val summary = storageAuditor.auditLibrary()
            storageAuditResult.value = summary
            isAuditingStorage.value = false
        }
    }

    fun purgeDuplicates(duplicateGroups: List<DuplicateGroup>, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val res = storageAuditor.purgeDuplicates(duplicateGroups)
            runStorageAudit()
            onComplete(res.getOrDefault(0))
        }
    }

    // 4-Key Backup & Dual-Phase Import
    suspend fun exportBackup(
        selectedPlaylists: Set<Long>,
        permissions: ExportPermissions,
        securityKeys: SecurityKeySuite
    ): Result<String> {
        return exportManager.createExport(selectedPlaylists, permissions, securityKeys)
    }

    suspend fun runPhase1Import(rawInput: String, keyAttempt: String): Phase1Result {
        val result = importManager.executePhase1(rawInput, keyAttempt)
        phase1Result.value = result
        if (result is Phase1Result.Success) {
            storageMatchResults.value = importManager.auditStorageMatch(result.payload)
        }
        return result
    }

    suspend fun runPhase2Import(
        payload: BackupPayload,
        importPlaylists: Boolean,
        importTimestamps: Boolean,
        importLyrics: Boolean,
        importClips: Boolean
    ): Result<Int> {
        val result = importManager.executePhase2(
            payload,
            importPlaylists,
            importTimestamps,
            importLyrics,
            importClips
        )
        scanDeviceAudioFiles()
        return result
    }
}
