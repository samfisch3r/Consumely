package com.android.consumely.data.local.converter

import androidx.room.TypeConverter
import com.android.consumely.data.local.model.LocationType

class Converters {
    @TypeConverter
    fun fromLocationType(type: LocationType): String = type.name

    @TypeConverter
    fun toLocationType(value: String): LocationType {
        return runCatching { LocationType.valueOf(value) }.getOrDefault(LocationType.PANTRY)
    }
}
