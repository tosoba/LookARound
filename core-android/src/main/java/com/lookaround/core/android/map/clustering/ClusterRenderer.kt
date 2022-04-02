package com.lookaround.core.android.map.clustering

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import com.lookaround.core.android.ext.MarkerPickResult
import com.lookaround.core.android.ext.TangramMarkerPickResult
import com.lookaround.core.android.ext.latLon
import com.lookaround.core.android.map.model.LatLon
import com.mapzen.tangram.LngLat
import com.mapzen.tangram.MapController
import com.mapzen.tangram.Marker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import timber.log.Timber

@ExperimentalCoroutinesApi
internal class ClusterRenderer<T : ClusterItem>(
    private val context: Context,
    private val mapController: MapController,
    private val iconGenerator: IconGenerator<T> = DefaultIconGenerator(context)
) {
    private val clusters: MutableList<Cluster<T>> = ArrayList()
    private val markers: MutableMap<Cluster<T>, Marker> = HashMap()

    fun onMarkerPickComplete(markerPickResult: TangramMarkerPickResult?): MarkerPickResult? {
        val marker = markerPickResult?.marker ?: return null
        @Suppress("UNCHECKED_CAST") val cluster = marker.userData as? Cluster<T> ?: return null
        val clusterItems = cluster.items
        return if (clusterItems.size > 1) {
            MarkerPickResult(markerPickResult.coordinates.latLon)
        } else {
            val item = clusterItems[0]
            MarkerPickResult(LatLon(item.latitude, item.longitude), item.uuid)
        }
    }

    fun render(clusters: List<Cluster<T>>) {
        val clustersToAdd = mutableListOf<Cluster<T>>()
        val clustersToRemove = mutableListOf<Cluster<T>>()

        for (cluster in clusters) {
            if (!markers.containsKey(cluster)) clustersToAdd.add(cluster)
        }

        for (cluster in markers.keys) {
            if (!clusters.contains(cluster)) clustersToRemove.add(cluster)
        }

        this.clusters.addAll(clustersToAdd)
        this.clusters.removeAll(clustersToRemove)

        for (clusterToRemove in clustersToRemove) {
            val markerToRemove = markers[clusterToRemove] ?: continue
            mapController.removeMarker(markerToRemove)
            try {
                markers.remove(clusterToRemove)
            } catch (throwable: Throwable) {
                Timber.e(throwable)
            }
        }

        for (clusterToAdd in clustersToAdd) {
            markers[clusterToAdd] =
                mapController.addMarker().apply {
                    setPoint(LngLat(clusterToAdd.longitude, clusterToAdd.latitude))
                    setStylingFromString(
                        "{ style: 'points', size: [27px, 27px], order: 2000, flat: true, collide: false, color: 'white', interactive: true}"
                    )
                    val markerIcon = getMarkerIcon(clusterToAdd)
                    setDrawable(BitmapDrawable(context.resources, markerIcon))
                    setDrawOrder(10)
                    isVisible = true
                    userData = clusterToAdd
                }
        }
    }

    private fun getMarkerIcon(cluster: Cluster<T>): Bitmap {
        val clusterItems = cluster.items
        return if (clusterItems.size > 1) iconGenerator.getClusterIcon(cluster)
        else iconGenerator.getClusterItemIcon(clusterItems[0])
    }
}
