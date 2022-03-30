package com.lookaround.core.android.map.clustering

import java.util.*

interface ClusterItem : QuadTreePoint {
    val title: String?
    val snippet: String?
    val uuid: UUID
}
