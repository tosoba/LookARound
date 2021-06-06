package com.lookaround.core.android.model

import android.location.Location
import android.os.Parcelable
import com.lookaround.core.model.PointDTO
import kotlinx.parcelize.Parcelize

@Parcelize
data class Point(
    override val name: String,
    override val location: Location,
) : INamedLocation, Parcelable {
    constructor(point: PointDTO) : this(point.name, LocationFactory.create(point.lat, point.lng))
}
