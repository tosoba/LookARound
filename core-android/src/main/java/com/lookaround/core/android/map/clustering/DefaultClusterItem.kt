package com.lookaround.core.android.map.clustering

import com.lookaround.core.android.map.model.LatLon
import java.util.*

data class DefaultClusterItem(override val uuid: UUID, private val latLon: LatLon) : ClusterItem {
    override val latitude: Double
        get() = latLon.latitude
    override val longitude: Double
        get() = latLon.longitude
    override val title: String?
        get() = null
    override val snippet: String?
        get() = null
}
