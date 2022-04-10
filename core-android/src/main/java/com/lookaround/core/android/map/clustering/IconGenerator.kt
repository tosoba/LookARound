package com.lookaround.core.android.map.clustering

import android.graphics.Bitmap

interface IconGenerator<T : ClusterItem> {
    fun getClusterIcon(cluster: Cluster<T>): Bitmap
    fun getClusterItemIcon(clusterItem: T): Bitmap
}
