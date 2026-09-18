package com.ammu.player.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "lyrics",
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["trackId"], unique = true)]
)
data class LyricsEntity(
    @PrimaryKey
    val trackId: Long,
    val plainLyrics: String = "",
    val syncedLrc: String = "",
    val notes: String = "",
    val lastEditedAt: Long = System.currentTimeMillis()
)
