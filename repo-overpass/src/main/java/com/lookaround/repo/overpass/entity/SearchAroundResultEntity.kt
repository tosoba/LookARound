package com.lookaround.repo.overpass.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "search_around_result",
    primaryKeys = ["search_around_id", "node_id"],
    foreignKeys =
        [
            ForeignKey(
                entity = SearchAroundEntity::class,
                parentColumns = ["id"],
                childColumns = ["search_around_id"],
                onDelete = ForeignKey.CASCADE
            ),
            ForeignKey(
                entity = NodeEntity::class,
                parentColumns = ["id"],
                childColumns = ["node_id"],
                onDelete = ForeignKey.CASCADE
            ),
        ],
    indices =
        [
            Index(
                value = ["search_around_id", "node_id"],
                unique = true,
            ),
        ]
)
data class SearchAroundResultEntity(
    @ColumnInfo(name = "search_around_id", index = true) val searchAroundId: Long,
    @ColumnInfo(name = "node_id", index = true) val nodeId: Long
)
