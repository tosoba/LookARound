package com.lookaround.repo.photon.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "autocomplete_search",
    indices = [Index(value = ["query", "priority_lat", "priority_lon"], unique = true)]
)
data class AutocompleteSearchEntity(@Embedded val input: AutocompleteSearchInput) {
    @PrimaryKey(autoGenerate = true) var id: Long = 0
}
