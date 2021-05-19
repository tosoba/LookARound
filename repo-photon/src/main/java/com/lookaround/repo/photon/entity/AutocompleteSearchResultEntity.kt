package com.lookaround.repo.photon.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "autocomplete_search_result",
    primaryKeys = ["autocomplete_search_id", "point_id"],
    foreignKeys =
        [
            ForeignKey(
                entity = AutocompleteSearchEntity::class,
                parentColumns = ["id"],
                childColumns = ["autocomplete_search_id"],
                onDelete = ForeignKey.CASCADE
            ),
            ForeignKey(
                entity = PointEntity::class,
                parentColumns = ["id"],
                childColumns = ["point_id"],
                onDelete = ForeignKey.CASCADE
            ),
        ],
    indices = [Index(value = ["autocomplete_search_id", "point_id"], unique = true)]
)
data class AutocompleteSearchResultEntity(
    @ColumnInfo(name = "autocomplete_search_id", index = true) val autocompleteSearchId: Long,
    @ColumnInfo(name = "point_id", index = true) val pointId: Long
)
