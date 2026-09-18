package com.ammu.player.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "tracks",
    indices = [
        Index(value = ["uriString"], unique = true),
        Index(value = ["sha256Hash"]),
        Index(value = ["isFavorite"]),
        Index(value = ["dateAdded"])
    ]
)
data class TrackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val path: String,
    val uriString: String,
    val sizeBytes: Long,
    val sha256Hash: String = "",
    val mimeType: String = "audio/mpeg",
    val dateAdded: Long = System.currentTimeMillis(),
    val playCount: Int = 0,
    val lastPlayed: Long = 0L,
    val clutterCleanTitle: String = "",
    val isFavorite: Boolean = false,
    val albumArtUri: String? = null
) {
    /**
     * Returns the cleaned title if clutter cleaning is enabled and available,
     * otherwise falls back to the original title.
     */
    fun displayTitle(useCleanClutter: Boolean = false): String {
        return if (useCleanClutter && clutterCleanTitle.isNotBlank()) clutterCleanTitle else title
    }
}
