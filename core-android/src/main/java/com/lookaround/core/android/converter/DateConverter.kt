package com.lookaround.core.android.converter

import androidx.room.TypeConverter
import java.util.*

object DateConverter {
    @TypeConverter fun toDate(timestamp: Long?): Date? = timestamp?.let(::Date)
    @TypeConverter fun toTimestamp(date: Date?): Long? = date?.time
}
