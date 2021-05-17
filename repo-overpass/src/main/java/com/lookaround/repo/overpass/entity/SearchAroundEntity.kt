package com.lookaround.repo.overpass.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lookaround.repo.overpass.model.SearchAroundInput

@Entity(
    tableName = "search_around",
    indices =
        [
            Index(
                value = ["lat", "lng", "radius_in_meters", "key", "value", "filter"],
                unique = true,
            ),
        ]
)
data class SearchAroundEntity(@Embedded val input: SearchAroundInput) {
    @PrimaryKey(autoGenerate = true) var id: Long = 0
}
