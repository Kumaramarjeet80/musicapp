package com.ammu.player.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "trimmed_clips",
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentTrackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["parentTrackId"]),
        Index(value = ["createdAt"])
    ]
)
data class TrimmedClipEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val parentTrackId: Long,
    val clipTitle: String,
    val startMs: Long,
    val endMs: Long,
    val clipPath: String,
    val clipSizeBytes: Long,
    val createdAt: Long = System.currentTimeMillis()
)
