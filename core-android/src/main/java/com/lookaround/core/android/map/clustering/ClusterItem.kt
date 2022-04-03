package com.lookaround.core.android.map.clustering

interface ClusterItem : QuadTreePoint {
    val title: String?
    val snippet: String?
    val extra: Any?
}
