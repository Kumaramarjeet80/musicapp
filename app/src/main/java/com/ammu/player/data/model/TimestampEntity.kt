package com.ammu.player.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "timestamps",
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["trackId"]),
        Index(value = ["trackId", "timestampMs"])
    ]
)
data class TimestampEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val trackId: Long,
    val timestampMs: Long,
    val label: String,
    val colorHex: String = "#00E5FF",
    val createdAt: Long = System.currentTimeMillis()
)
