package com.ammu.player.data.db

import androidx.room.TypeConverter
import com.ammu.player.data.model.SmartPlaylistType

class Converters {
    @TypeConverter
    fun fromSmartPlaylistType(type: SmartPlaylistType?): String? {
        return type?.name
    }

    @TypeConverter
    fun toSmartPlaylistType(value: String?): SmartPlaylistType? {
        return value?.let {
            try {
                SmartPlaylistType.valueOf(it)
            } catch (e: Exception) {
                SmartPlaylistType.CUSTOM
            }
        }
    }
}
