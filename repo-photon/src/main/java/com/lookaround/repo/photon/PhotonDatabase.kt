package com.lookaround.repo.photon

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lookaround.core.android.converter.DateConverter
import com.lookaround.repo.photon.dao.AutocompleteSearchDao
import com.lookaround.repo.photon.entity.AutocompleteSearchEntity
import com.lookaround.repo.photon.entity.AutocompleteSearchResultEntity
import com.lookaround.repo.photon.entity.PointEntity

@Database(
    version = 1,
    exportSchema = false,
    entities =
        [
            PointEntity::class,
            AutocompleteSearchEntity::class,
            AutocompleteSearchResultEntity::class,
        ]
)
@TypeConverters(value = [DateConverter::class])
abstract class PhotonDatabase : RoomDatabase() {
    abstract fun autocompleteSearchDao(): AutocompleteSearchDao
}
