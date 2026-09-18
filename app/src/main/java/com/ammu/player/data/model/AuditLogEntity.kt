package com.ammu.player.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "audit_logs",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["actionType"])
    ]
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val actionType: String,
    val timestamp: Long = System.currentTimeMillis(),
    val details: String,
    val affectedEntityId: String? = null,
    val status: String = "SUCCESS"
)
