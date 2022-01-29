package com.lookaround.core.android.map.clustering

/** An object representing a single cluster item (marker) on the map. */
interface ClusterItem : QuadTreePoint {
    /**
     * The title of the item.
     *
     * @return the title of the item
     */
    val title: String?

    /**
     * The snippet of the item.
     *
     * @return the snippet of the item
     */
    val snippet: String?
}
