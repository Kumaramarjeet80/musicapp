package com.ammu.player.data.backup

import android.content.Context
import android.util.Base64
import com.ammu.player.data.db.AmmuDatabase
import com.ammu.player.data.model.AuditLogEntity
import com.ammu.player.data.model.TrackEntity
import com.ammu.player.data.security.AesGcmEngine
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

sealed class Phase1Result {
    data class Success(val payload: BackupPayload, val wasEncrypted: Boolean) : Phase1Result()
    data class SuperKeyRejected(val message: String = "There is no super access. You have to put keys to get access.") : Phase1Result()
    data class DecryptionFailed(val error: String) : Phase1Result()
    data class CorruptedPayload(val error: String) : Phase1Result()
}

class DualPhaseImportManager(
    private val context: Context,
    private val db: AmmuDatabase
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Phase 1: Decrypts if needed and verifies the integrity of the payload.
     * Enforces the Admin Super-Key security guard check.
     */
    suspend fun executePhase1(rawInput: String, keyAttempt: String): Phase1Result = withContext(Dispatchers.IO) {
        val trimmed = rawInput.trim()
        val key = keyAttempt.trim()

        // Admin Super-Key guard check
        val superKeyTriggers = listOf("super", "admin", "root", "bypass", "masteradmin", "override")
        if (superKeyTriggers.any { key.equals(it, ignoreCase = true) }) {
            db.auditLogDao().insertLog(
                AuditLogEntity(
                    actionType = "SUPER_KEY_ATTEMPT_BLOCKED",
                    details = "Attempted unauthorized bypass with keyword '$keyAttempt'",
                    status = "BLOCKED"
                )
            )
            return@withContext Phase1Result.SuperKeyRejected()
        }

        // Try parsing directly as JSON
        if (trimmed.startsWith("{") && trimmed.contains("AMMU_VAULT_BACKUP")) {
            return@withContext try {
                val payload = json.decodeFromString<BackupPayload>(trimmed)
                Phase1Result.Success(payload, wasEncrypted = false)
            } catch (e: Exception) {
                Phase1Result.CorruptedPayload("Invalid JSON format: ${e.localizedMessage}")
            }
        }

        // Otherwise assume AES-GCM encrypted envelope
        if (key.isBlank()) {
            return@withContext Phase1Result.DecryptionFailed("This backup is AES-256-GCM encrypted. A decryption passkey is required.")
        }

        return@withContext try {
            val decryptedJson = AesGcmEngine.decrypt(trimmed, key)
            val payload = json.decodeFromString<BackupPayload>(decryptedJson)
            Phase1Result.Success(payload, wasEncrypted = true)
        } catch (e: Exception) {
            Phase1Result.DecryptionFailed("Decryption failed. Invalid passkey or corrupted data.")
        }
    }

    /**
     * Storage Matcher: Matches incoming tracks against existing local audio files by SHA256 hash or file path.
     */
    suspend fun auditStorageMatch(payload: BackupPayload): List<StorageMatchResult> = withContext(Dispatchers.IO) {
        val localTracks = db.trackDao().getAllTracksDirect()
        val localHashMap = localTracks.filter { it.sha256Hash.isNotBlank() }.associateBy { it.sha256Hash }
        val localPathMap = localTracks.associateBy { it.path }

        payload.tracks.map { importTrack ->
            val matchByHash = if (importTrack.sha256Hash.isNotBlank()) localHashMap[importTrack.sha256Hash] else null
            val matchByPath = localPathMap[importTrack.path]
            val matchedTrack = matchByHash ?: matchByPath

            val fileOnDiskExists = matchedTrack?.let { File(it.path).exists() }
                ?: File(importTrack.path).exists()

            StorageMatchResult(
                trackId = importTrack.id,
                title = importTrack.title,
                artist = importTrack.artist,
                sha256Hash = importTrack.sha256Hash,
                isAvailableLocally = fileOnDiskExists,
                matchedPath = matchedTrack?.path ?: if (File(importTrack.path).exists()) importTrack.path else null
            )
        }
    }

    /**
     * Phase 2: Restores selected items from verified payload into Room Database.
     */
    suspend fun executePhase2(
        payload: BackupPayload,
        importPlaylists: Boolean,
        importTimestamps: Boolean,
        importLyrics: Boolean,
        importClips: Boolean
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var itemsRestored = 0
            val trackDao = db.trackDao()
            val playlistDao = db.playlistDao()
            val lyricsDao = db.lyricsDao()
            val timestampDao = db.timestampDao()
            val clipDao = db.trimmedClipDao()

            // 1. Tracks (Merge by URI or Hash)
            for (track in payload.tracks) {
                val existing = trackDao.getTrackByUri(track.uriString)
                    ?: if (track.sha256Hash.isNotBlank()) trackDao.getTrackByHash(track.sha256Hash) else null

                if (existing == null) {
                    trackDao.insertTrack(track.copy(id = 0L))
                    itemsRestored++
                }
            }

            // 2. Playlists & CrossRefs
            if (importPlaylists) {
                for (pl in payload.playlists) {
                    if (!pl.isSmartPlaylist) {
                        val newPlId = playlistDao.insertPlaylist(pl.copy(id = 0L))
                        // map matching cross refs
                        val refs = payload.crossRefs.filter { it.playlistId == pl.id }
                        for (ref in refs) {
                            playlistDao.insertCrossRef(ref.copy(playlistId = newPlId))
                        }
                        itemsRestored++
                    }
                }
            }

            // 3. Lyrics
            if (importLyrics) {
                for (lyric in payload.lyrics) {
                    lyricsDao.insertLyrics(lyric)
                    itemsRestored++
                }
            }

            // 4. Timestamps
            if (importTimestamps) {
                for (ts in payload.timestamps) {
                    timestampDao.insertTimestamp(ts.copy(id = 0L))
                    itemsRestored++
                }
            }

            // 5. Trimmed Clips & Audio Blobs
            if (importClips) {
                for (clip in payload.trimmedClips) {
                    var targetPath = clip.clipPath
                    // If audio blob was exported, write blob to internal cache/files dir
                    payload.audioBlobsBase64[clip.id]?.let { base64Blob ->
                        val blobBytes = Base64.decode(base64Blob, Base64.NO_WRAP)
                        val clipFile = File(context.filesDir, "clips/clip_${System.currentTimeMillis()}_${clip.id}.mp3")
                        clipFile.parentFile?.mkdirs()
                        clipFile.writeBytes(blobBytes)
                        targetPath = clipFile.absolutePath
                    }
                    clipDao.insertClip(clip.copy(id = 0L, clipPath = targetPath))
                    itemsRestored++
                }
            }

            // Audit log
            db.auditLogDao().insertLog(
                AuditLogEntity(
                    actionType = "BACKUP_IMPORT",
                    details = "Phase 2 completed: restored $itemsRestored entities",
                    status = "SUCCESS"
                )
            )

            Result.success(itemsRestored)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
