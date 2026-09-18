package com.ammu.player.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ammu.player.data.model.TimestampEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimestampDao {
    @Query("SELECT * FROM timestamps WHERE trackId = :trackId ORDER BY timestampMs ASC")
    fun getTimestampsForTrack(trackId: Long): Flow<List<TimestampEntity>>

    @Query("SELECT * FROM timestamps WHERE trackId = :trackId ORDER BY timestampMs ASC")
    suspend fun getTimestampsForTrackDirect(trackId: Long): List<TimestampEntity>

    @Query("SELECT * FROM timestamps ORDER BY createdAt ASC")
    suspend fun getAllTimestampsDirect(): List<TimestampEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimestamp(timestamp: TimestampEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTimestamps(timestamps: List<TimestampEntity>): List<Long>

    @Update
    suspend fun updateTimestamp(timestamp: TimestampEntity)

    @Delete
    suspend fun deleteTimestamp(timestamp: TimestampEntity)

    @Query("DELETE FROM timestamps WHERE id = :id")
    suspend fun deleteTimestampById(id: Long)

    @Query("DELETE FROM timestamps WHERE trackId = :trackId")
    suspend fun deleteAllTimestampsForTrack(trackId: Long)
}
