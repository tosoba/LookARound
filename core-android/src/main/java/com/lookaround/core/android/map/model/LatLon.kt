package com.lookaround.core.android.map.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LatLon(val latitude: Double, val longitude: Double) : Parcelable {
    init {
        checkValidity(latitude, longitude)
    }

    companion object {
        fun checkValidity(latitude: Double, longitude: Double) {
            require(
                latitude >= -90.0 && latitude <= +90 && longitude >= -180 && longitude <= +180
            ) { "Latitude $latitude, longitude $longitude is not a valid position" }
        }
    }
}
