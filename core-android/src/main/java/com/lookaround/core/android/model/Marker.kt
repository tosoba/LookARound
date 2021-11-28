package com.lookaround.core.android.model

import android.location.Location
import android.os.Parcelable
import com.lookaround.core.model.NodeDTO
import com.lookaround.core.model.PointDTO
import java.util.*
import kotlinx.parcelize.Parcelize

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
        location = LocationFactory.create(latitude = node.lat, longitude = node.lon),
        tags = node.tags
    )

    constructor(
        point: PointDTO
    ) : this(
        name = point.name,
        location = LocationFactory.create(latitude = point.lat, longitude = point.lng)
    )

    override fun equals(other: Any?): Boolean =
        this === other || (other is Marker && other.id == id)

    override fun hashCode(): Int = Objects.hash(id)
}
