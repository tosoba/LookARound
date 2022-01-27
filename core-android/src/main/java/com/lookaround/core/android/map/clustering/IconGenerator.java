package com.lookaround.core.android.map.clustering;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

/**
 * Generates icons for clusters and cluster items. Note that its implementations
 * should cache generated icons for subsequent use. For the example implementation see
 * {@link DefaultIconGenerator}.
 */
public interface IconGenerator<T extends ClusterItem> {
    /**
     * Returns an icon for the given cluster.
     *
     * @param cluster the cluster to return an icon for
     * @return the icon for the given cluster
     */
    @NonNull
    Bitmap getClusterIcon(@NonNull Cluster<T> cluster);

    /**
     * Returns an icon for the given cluster item.
     *
     * @param clusterItem the cluster item to return an icon for
     * @return the icon for the given cluster item
     */
    @NonNull
    Bitmap getClusterItemIcon(@NonNull T clusterItem);
}
