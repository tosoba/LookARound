package com.lookaround.repo.photon.entity

import androidx.room.*
import java.util.*

@Entity(
    tableName = "autocomplete_search",
    indices =
        [
            Index(value = ["query", "priority_lat", "priority_lon"], unique = true),
            Index(value = ["last_searched_at"]),
        ]
)
data class AutocompleteSearchEntity(
    @Embedded val input: AutocompleteSearchInput,
    @ColumnInfo(name = "last_searched_at") val lastSearchedAt: Date = Date()
) {
    @PrimaryKey(autoGenerate = true) var id: Long = 0
}
