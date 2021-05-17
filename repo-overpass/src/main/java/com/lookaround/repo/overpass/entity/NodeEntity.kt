package com.lookaround.repo.overpass.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "node")
data class NodeEntity(
    @PrimaryKey val id: Long,
    val lat: Double,
    val lon: Double,
    val name: String,
    val tags: Map<String, String>
)
