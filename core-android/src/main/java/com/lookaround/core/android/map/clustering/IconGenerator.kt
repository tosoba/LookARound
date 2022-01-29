package com.lookaround.core.android.map.clustering

import android.graphics.Bitmap

/**
 * Generates icons for clusters and cluster items. Note that its implementations should cache
 * generated icons for subsequent use. For the example implementation see [DefaultIconGenerator].
 */
interface IconGenerator<T : ClusterItem> {
    /**
     * Returns an icon for the given cluster.
     *
     * @param cluster the cluster to return an icon for
     * @return the icon for the given cluster
     */
    fun getClusterIcon(cluster: Cluster<T>): Bitmap

    /**
     * Returns an icon for the given cluster item.
     *
     * @param clusterItem the cluster item to return an icon for
     * @return the icon for the given cluster item
     */
    fun getClusterItemIcon(clusterItem: T): Bitmap
}
