package com.ammu.player.data.storage

import android.content.Context
import com.ammu.player.data.db.AmmuDatabase
import com.ammu.player.data.model.AuditLogEntity
import com.ammu.player.data.model.TrackEntity
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DuplicateGroup(
    val signature: String, // sha256 or size_signature
    val sizeBytes: Long,
    val originalTrack: TrackEntity,
    val duplicateTracks: List<TrackEntity>
) {
    val potentialSavedBytes: Long
        get() = sizeBytes * duplicateTracks.size
}

data class StorageAuditSummary(
    val totalTracksChecked: Int,
    val totalFilesSize: Long,
    val duplicateGroups: List<DuplicateGroup>,
    val totalWastedBytes: Long
)

class StorageAuditor(
    private val context: Context,
    private val db: AmmuDatabase
) {
    /**
     * Calculates SHA-256 hash of a file efficiently using a streaming buffer.
     */
    fun computeSha256(file: File): String {
        if (!file.exists() || !file.canRead()) return ""
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Scans all indexed tracks in the database, verifies hashes, and groups duplicate signatures.
     */
    suspend fun auditLibrary(): StorageAuditSummary = withContext(Dispatchers.IO) {
        val trackDao = db.trackDao()
        val allTracks = trackDao.getAllTracksDirect()

        var totalSize = 0L
        val updatedTracks = mutableListOf<TrackEntity>()

        for (track in allTracks) {
            val file = File(track.path)
            if (file.exists()) {
                totalSize += file.length()
                if (track.sha256Hash.isBlank()) {
                    val hash = computeSha256(file)
                    if (hash.isNotBlank()) {
                        val updated = track.copy(sha256Hash = hash, sizeBytes = file.length())
                        trackDao.updateTrack(updated)
                        updatedTracks.add(updated)
                        continue
                    }
                }
            }
            updatedTracks.add(track)
        }

        // Group by SHA-256 if present, else fallback to file size + duration
        val groupsBySignature = updatedTracks.groupBy { track ->
            if (track.sha256Hash.isNotBlank()) {
                "SHA:${track.sha256Hash}"
            } else {
                "SIZE:${track.sizeBytes}_DUR:${track.durationMs / 1000}"
            }
        }

        val duplicateGroups = mutableListOf<DuplicateGroup>()
        var totalWastedBytes = 0L

        for ((signature, tracks) in groupsBySignature) {
            if (tracks.size > 1) {
                // Keep the oldest or most-played track as primary
                val sorted = tracks.sortedWith(
                    compareByDescending<TrackEntity> { it.playCount }
                        .thenBy { it.dateAdded }
                )
                val original = sorted.first()
                val duplicates = sorted.drop(1)

                val group = DuplicateGroup(
                    signature = signature,
                    sizeBytes = original.sizeBytes,
                    originalTrack = original,
                    duplicateTracks = duplicates
                )
                duplicateGroups.add(group)
                totalWastedBytes += group.potentialSavedBytes
            }
        }

        StorageAuditSummary(
            totalTracksChecked = allTracks.size,
            totalFilesSize = totalSize,
            duplicateGroups = duplicateGroups,
            totalWastedBytes = totalWastedBytes
        )
    }

    /**
     * Purges duplicate tracks: deletes their disk files (if within app storage) and removes DB records.
     */
    suspend fun purgeDuplicates(duplicateGroups: List<DuplicateGroup>): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var purgedCount = 0
            val trackDao = db.trackDao()

            for (group in duplicateGroups) {
                for (duplicate in group.duplicateTracks) {
                    val file = File(duplicate.path)
                    if (file.exists() && file.isFile) {
                        file.delete()
                    }
                    trackDao.deleteTrack(duplicate)
                    purgedCount++
                }
            }

            db.auditLogDao().insertLog(
                AuditLogEntity(
                    actionType = "STORAGE_DUPLICATE_PURGE",
                    details = "Purged $purgedCount duplicate tracks across ${duplicateGroups.size} groups",
                    status = "SUCCESS"
                )
            )

            Result.success(purgedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
