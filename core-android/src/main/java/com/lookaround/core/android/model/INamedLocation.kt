package com.lookaround.core.android.model

import android.location.Location

interface INamedLocation {
    val name: String
    val location: Location
}
