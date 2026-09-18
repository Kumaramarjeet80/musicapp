package com.ammu.player.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ammu.player.data.model.TrimmedClipEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrimmedClipDao {
    @Query("SELECT * FROM trimmed_clips ORDER BY createdAt DESC")
    fun getAllClips(): Flow<List<TrimmedClipEntity>>

    @Query("SELECT * FROM trimmed_clips ORDER BY createdAt DESC")
    suspend fun getAllClipsDirect(): List<TrimmedClipEntity>

    @Query("SELECT * FROM trimmed_clips WHERE parentTrackId = :trackId ORDER BY startMs ASC")
    fun getClipsForTrack(trackId: Long): Flow<List<TrimmedClipEntity>>

    @Query("SELECT * FROM trimmed_clips WHERE id = :id LIMIT 1")
    suspend fun getClipById(id: Long): TrimmedClipEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClip(clip: TrimmedClipEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllClips(clips: List<TrimmedClipEntity>): List<Long>

    @Update
    suspend fun updateClip(clip: TrimmedClipEntity)

    @Delete
    suspend fun deleteClip(clip: TrimmedClipEntity)

    @Query("DELETE FROM trimmed_clips WHERE id = :id")
    suspend fun deleteClipById(id: Long)
}
