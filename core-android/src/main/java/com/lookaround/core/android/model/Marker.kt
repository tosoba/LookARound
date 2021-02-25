package com.lookaround.core.android.model

import android.location.Location
import java.util.*

data class Marker(val name: String, val location: Location) {
    val id: UUID = UUID.randomUUID()
}
