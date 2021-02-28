package com.lookaround.core.android.model

import android.location.Location
import java.util.*

class Marker(val name: String, val location: Location) {
    val id: UUID = UUID.randomUUID()

    override fun equals(other: Any?): Boolean =
        this === other || (other is Marker && other.id == id)

    override fun hashCode(): Int = Objects.hash(id)
}
