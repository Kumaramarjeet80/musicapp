package com.ammu.player.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ammu.player.data.model.AuditLogEntity
import com.ammu.player.data.model.FavoriteEntity
import com.ammu.player.data.model.LyricsEntity
import com.ammu.player.data.model.PlaylistEntity
import com.ammu.player.data.model.PlaylistTrackCrossRef
import com.ammu.player.data.model.SmartPlaylistType
import com.ammu.player.data.model.TimestampEntity
import com.ammu.player.data.model.TrackEntity
import com.ammu.player.data.model.TrimmedClipEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        FavoriteEntity::class,
        LyricsEntity::class,
        TimestampEntity::class,
        TrimmedClipEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AmmuDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun lyricsDao(): LyricsDao
    abstract fun timestampDao(): TimestampDao
    abstract fun trimmedClipDao(): TrimmedClipDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: AmmuDatabase? = null

        fun getInstance(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AmmuDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AmmuDatabase::class.java,
                    "ammu_music.db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch {
                        populateDefaultSmartPlaylists(database.playlistDao())
                    }
                }
            }

            private suspend fun populateDefaultSmartPlaylists(playlistDao: PlaylistDao) {
                val defaults = listOf(
                    PlaylistEntity(
                        id = 1L,
                        name = "All",
                        description = "All audio tracks in your library",
                        isSmartPlaylist = true,
                        smartType = SmartPlaylistType.ALL,
                        iconEmoji = "🎵"
                    ),
                    PlaylistEntity(
                        id = 2L,
                        name = "Heavy Rotation ⚡",
                        description = "Frequently and most played songs",
                        isSmartPlaylist = true,
                        smartType = SmartPlaylistType.HEAVY_ROTATION,
                        iconEmoji = "⚡"
                    ),
                    PlaylistEntity(
                        id = 3L,
                        name = "Recently Added 🕒",
                        description = "Latest audio added to library",
                        isSmartPlaylist = true,
                        smartType = SmartPlaylistType.RECENTLY_ADDED,
                        iconEmoji = "🕒"
                    ),
                    PlaylistEntity(
                        id = 4L,
                        name = "Unplayed 💤",
                        description = "Tracks waiting to be explored",
                        isSmartPlaylist = true,
                        smartType = SmartPlaylistType.UNPLAYED,
                        iconEmoji = "💤"
                    )
                )
                playlistDao.insertPlaylists(defaults)
            }
        }
    }
}
