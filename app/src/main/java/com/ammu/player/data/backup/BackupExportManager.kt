package com.ammu.player.data.backup

import android.content.Context
import android.util.Base64
import com.ammu.player.data.db.AmmuDatabase
import com.ammu.player.data.model.AuditLogEntity
import com.ammu.player.data.model.PlaylistEntity
import com.ammu.player.data.model.PlaylistTrackCrossRef
import com.ammu.player.data.model.TrackEntity
import com.ammu.player.data.security.AesGcmEngine
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BackupExportManager(
    private val context: Context,
    private val db: AmmuDatabase
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Builds and exports a scoped backup JSON or encrypted string using the 4-Key Security Suite.
     */
    suspend fun createExport(
        selectedPlaylistIds: Set<Long>,
        permissions: ExportPermissions,
        securityKeys: SecurityKeySuite,
        includeAllTracksIfNoPlaylistSelected: Boolean = true
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val trackDao = db.trackDao()
            val playlistDao = db.playlistDao()
            val favoriteDao = db.favoriteDao()
            val lyricsDao = db.lyricsDao()
            val timestampDao = db.timestampDao()
            val clipDao = db.trimmedClipDao()
            val auditDao = db.auditLogDao()

            // 1. Gather Playlists & CrossRefs
            val playlistsToExport = mutableListOf<PlaylistEntity>()
            val crossRefsToExport = mutableListOf<PlaylistTrackCrossRef>()
            val relevantTrackIds = mutableSetOf<Long>()

            if (permissions.includePlaylists && selectedPlaylistIds.isNotEmpty()) {
                for (plId in selectedPlaylistIds) {
                    playlistDao.getPlaylistById(plId)?.let { pl ->
                        playlistsToExport.add(pl)
                        val refs = playlistDao.getCrossRefsForPlaylist(plId)
                        crossRefsToExport.addAll(refs)
                        relevantTrackIds.addAll(refs.map { it.trackId })
                    }
                }
            }

            // 2. Gather Tracks
            val allTracks = trackDao.getAllTracksDirect()
            val tracksToExport = if (relevantTrackIds.isNotEmpty()) {
                allTracks.filter { it.id in relevantTrackIds }
            } else if (includeAllTracksIfNoPlaylistSelected) {
                allTracks
            } else {
                emptyList()
            }
            val targetTrackIds = tracksToExport.map { it.id }.toSet()

            // 3. Gather Favorites
            val favorites = favoriteDao.getAllFavoritesDirect().filter { it.trackId in targetTrackIds }

            // 4. Gather Lyrics & Notes
            val lyrics = if (permissions.includeLyricsAndNotes) {
                lyricsDao.getAllLyricsDirect().filter { it.trackId in targetTrackIds }
            } else emptyList()

            // 5. Gather Timestamps
            val timestamps = if (permissions.includeTimestamps) {
                timestampDao.getAllTimestampsDirect().filter { it.trackId in targetTrackIds }
            } else emptyList()

            // 6. Gather Trimmed Clips & Optional Blobs
            val clips = if (permissions.includeTrimmedClips) {
                clipDao.getAllClipsDirect().filter { it.parentTrackId in targetTrackIds }
            } else emptyList()

            val blobs = mutableMapOf<Long, String>()
            if (permissions.includeAudioBlobs) {
                for (clip in clips) {
                    val file = File(clip.clipPath)
                    if (file.exists() && file.canRead()) {
                        val bytes = file.readBytes()
                        blobs[clip.id] = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    }
                }
            }

            // 7. Assemble Payload
            val payload = BackupPayload(
                schemaVersion = 1,
                exportedAt = System.currentTimeMillis(),
                permissions = permissions,
                tracks = tracksToExport,
                playlists = playlistsToExport,
                crossRefs = crossRefsToExport,
                favorites = favorites,
                lyrics = lyrics,
                timestamps = timestamps,
                trimmedClips = clips,
                audioBlobsBase64 = blobs
            )

            val plainJson = json.encodeToString(payload)

            // 8. Security Suite Encryption
            val finalOutput = if (securityKeys.aesGcmKey.isNotBlank()) {
                AesGcmEngine.encrypt(plainJson, securityKeys.aesGcmKey)
            } else {
                plainJson
            }

            // 9. Log Audit Trail
            auditDao.insertLog(
                AuditLogEntity(
                    actionType = "BACKUP_EXPORT",
                    details = "Exported ${tracksToExport.size} tracks, ${playlistsToExport.size} playlists, encrypted=${securityKeys.aesGcmKey.isNotBlank()}",
                    status = "SUCCESS"
                )
            )

            Result.success(finalOutput)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
