package com.ammu.player.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ammu.player.data.model.LyricsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LyricsDao {
    @Query("SELECT * FROM lyrics WHERE trackId = :trackId LIMIT 1")
    fun getLyricsForTrack(trackId: Long): Flow<LyricsEntity?>

    @Query("SELECT * FROM lyrics WHERE trackId = :trackId LIMIT 1")
    suspend fun getLyricsForTrackDirect(trackId: Long): LyricsEntity?

    @Query("SELECT * FROM lyrics")
    suspend fun getAllLyricsDirect(): List<LyricsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLyrics(lyrics: LyricsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLyrics(lyricsList: List<LyricsEntity>)

    @Update
    suspend fun updateLyrics(lyrics: LyricsEntity)

    @Delete
    suspend fun deleteLyrics(lyrics: LyricsEntity)

    @Query("DELETE FROM lyrics WHERE trackId = :trackId")
    suspend fun deleteLyricsByTrackId(trackId: Long)
}
