package com.ammu.player.data.backup

import com.ammu.player.data.model.FavoriteEntity
import com.ammu.player.data.model.LyricsEntity
import com.ammu.player.data.model.PlaylistEntity
import com.ammu.player.data.model.PlaylistTrackCrossRef
import com.ammu.player.data.model.TimestampEntity
import com.ammu.player.data.model.TrackEntity
import com.ammu.player.data.model.TrimmedClipEntity
import kotlinx.serialization.Serializable

@Serializable
data class SecurityKeySuite(
    val masterKey: String = "",
    val aesGcmKey: String = "",
    val creatorPasskey: String = "",
    val downloadKey: String = ""
)

@Serializable
data class ExportPermissions(
    val allowTrackDownloads: Boolean = true,
    val includeAudioBlobs: Boolean = false,
    val includeTrimmedClips: Boolean = true,
    val includeTimestamps: Boolean = true,
    val includeLyricsAndNotes: Boolean = true,
    val includePlaylists: Boolean = true,
    val trackRestrictions: Map<Long, Boolean> = emptyMap() // trackId -> canDownload
)

@Serializable
data class BackupPayload(
    val schemaVersion: Int = 1,
    val appSignature: String = "AMMU_VAULT_BACKUP",
    val exportedAt: Long = System.currentTimeMillis(),
    val creatorTag: String = "Ammu Studio Engine",
    val permissions: ExportPermissions = ExportPermissions(),
    val tracks: List<TrackEntity> = emptyList(),
    val playlists: List<PlaylistEntity> = emptyList(),
    val crossRefs: List<PlaylistTrackCrossRef> = emptyList(),
    val favorites: List<FavoriteEntity> = emptyList(),
    val lyrics: List<LyricsEntity> = emptyList(),
    val timestamps: List<TimestampEntity> = emptyList(),
    val trimmedClips: List<TrimmedClipEntity> = emptyList(),
    val audioBlobsBase64: Map<Long, String> = emptyMap() // clipId -> base64 blob
)

data class StorageMatchResult(
    val trackId: Long,
    val title: String,
    val artist: String,
    val sha256Hash: String,
    val isAvailableLocally: Boolean,
    val matchedPath: String? = null
)
