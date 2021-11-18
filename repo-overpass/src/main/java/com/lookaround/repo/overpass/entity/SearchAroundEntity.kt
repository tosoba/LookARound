package com.lookaround.repo.overpass.entity

import androidx.room.*
import java.util.*

@Entity(
    tableName = "search_around",
    indices =
        [
            Index(
                value = ["lat", "lng", "radius_in_meters", "key", "value", "filter"],
                unique = true,
            ),
            Index(value = ["last_searched_at"]),
        ]
)
data class SearchAroundEntity(
    @Embedded val input: SearchAroundInput,
    @ColumnInfo(name = "last_searched_at") val lastSearchedAt: Date = Date()
) {
    @PrimaryKey(autoGenerate = true) var id: Long = 0
}
