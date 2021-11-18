package com.lookaround.repo.overpass

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lookaround.core.android.converter.DateConverter
import com.lookaround.repo.overpass.converter.OverpassConverters
import com.lookaround.repo.overpass.dao.SearchAroundDao
import com.lookaround.repo.overpass.entity.NodeEntity
import com.lookaround.repo.overpass.entity.SearchAroundEntity
import com.lookaround.repo.overpass.entity.SearchAroundResultEntity

@Database(
    version = 1,
    exportSchema = false,
    entities = [NodeEntity::class, SearchAroundEntity::class, SearchAroundResultEntity::class]
)
@TypeConverters(value = [OverpassConverters::class, DateConverter::class])
abstract class OverpassDatabase : RoomDatabase() {
    abstract fun searchAroundDao(): SearchAroundDao
}
