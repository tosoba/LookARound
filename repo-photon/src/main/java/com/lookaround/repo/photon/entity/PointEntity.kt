package com.lookaround.repo.photon.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "point",
    indices =
        [
            Index(
                value = ["name", "lat", "lng"],
                unique = true,
            ),
        ]
)
data class PointEntity(val name: String, val lat: Double, val lng: Double) {
    @PrimaryKey(autoGenerate = true) var id: Long = 0
}
