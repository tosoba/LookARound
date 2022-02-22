package com.lookaround.core.android.model

import android.location.Location
import android.os.Parcelable
import com.lookaround.core.android.ext.locationWith
import com.lookaround.core.model.NodeDTO
import com.lookaround.core.model.PointDTO
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class Marker(
    override val name: String,
    override val location: Location,
    val tags: Map<String, String> = emptyMap(),
    val id: UUID = UUID.randomUUID()
) : INamedLocation, Parcelable {
    constructor(
        node: NodeDTO
    ) : this(
        name = node.name,
        location = locationWith(latitude = node.lat, longitude = node.lon),
        tags = node.tags
    )

    constructor(
        point: PointDTO
    ) : this(
        name = point.name,
        location = locationWith(latitude = point.lat, longitude = point.lng)
    )

    override fun equals(other: Any?): Boolean =
        this === other || (other is Marker && other.id == id)

    override fun hashCode(): Int = Objects.hash(id)
}
