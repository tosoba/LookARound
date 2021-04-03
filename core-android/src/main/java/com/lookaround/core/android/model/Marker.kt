package com.lookaround.core.android.model

import android.location.Location
import android.os.Parcelable
import com.lookaround.core.model.NodeDTO
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
class Marker(
    val name: String,
    val location: Location,
    val tags: Map<String, String> = emptyMap(),
    val id: UUID = UUID.randomUUID()
) : Parcelable {
    constructor(
        node: NodeDTO
    ) : this(node.name, LocationFactory.create(node.lat, node.lon), node.tags)

    override fun equals(other: Any?): Boolean =
        this === other || (other is Marker && other.id == id)

    override fun hashCode(): Int = Objects.hash(id)
}
