package com.lookaround.core.android.model

import android.location.Location
import android.os.Parcelable
import java.util.*
import kotlinx.parcelize.Parcelize

@Parcelize
class Marker(
    val name: String,
    val location: Location,
    val tags: Map<String, String> = emptyMap(),
    val id: UUID = UUID.randomUUID()
) : Parcelable {
    override fun equals(other: Any?): Boolean =
        this === other || (other is Marker && other.id == id)

    override fun hashCode(): Int = Objects.hash(id)
}
