package com.ammu.player.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class SmartPlaylistType {
    ALL,
    HEAVY_ROTATION,
    RECENTLY_ADDED,
    UNPLAYED,
    CUSTOM
}

@Serializable
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isSmartPlaylist: Boolean = false,
    val smartType: SmartPlaylistType = SmartPlaylistType.CUSTOM,
    val iconEmoji: String = "🎵"
)
