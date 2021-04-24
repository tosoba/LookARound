package com.lookaround.core.android.model

import android.location.Location
import android.os.Parcelable
import com.lookaround.core.model.PointDTO
import kotlinx.parcelize.Parcelize

@Parcelize
data class Point(val name: String, val location: Location) : Parcelable {
    constructor(point: PointDTO) : this(point.name, LocationFactory.create(point.lat, point.lng))
}
