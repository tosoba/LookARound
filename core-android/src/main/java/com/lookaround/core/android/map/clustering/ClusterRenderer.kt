package com.lookaround.core.android.map.clustering

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import com.mapzen.tangram.*

internal class ClusterRenderer<T : ClusterItem>(
    private val context: Context,
    private val mapController: MapController,
    private val iconGenerator: IconGenerator<T> = DefaultIconGenerator(context)
) : MarkerPickListener {
    private val clusters: MutableList<Cluster<T>> = ArrayList()
    private val markers: MutableMap<Cluster<T>, Marker> = HashMap()
    var callbacks: ClusterManager.Callbacks<T>? = null

    init {
        mapController.setMarkerPickListener(this)
    }

    override fun onMarkerPickComplete(markerPickResult: MarkerPickResult?) {
        if (markerPickResult == null || markerPickResult.marker == null) return
        val cluster = markerPickResult.marker.userData as? Cluster<T>
        if (cluster != null) {
            val clusterItems = cluster.items
            callbacks?.let {
                if (clusterItems.size > 1) it.onClusterClick(cluster)
                else it.onClusterItemClick(clusterItems[0])
            }
        }
    }

    fun render(clusters: List<Cluster<T>>) {
        val clustersToAdd = ArrayList<Cluster<T>>()
        val clustersToRemove = ArrayList<Cluster<T>>()

        for (cluster in clusters) {
            if (!markers.containsKey(cluster)) {
                clustersToAdd.add(cluster)
            }
        }

        for (cluster in markers.keys) {
            if (!clusters.contains(cluster)) {
                clustersToRemove.add(cluster)
            }
        }

        this.clusters.addAll(clustersToAdd)
        this.clusters.removeAll(clustersToRemove)

        for (clusterToRemove in clustersToRemove) {
            val markerToRemove = markers[clusterToRemove] ?: continue
            mapController.removeMarker(markerToRemove)
            markers.remove(clusterToRemove)
        }

        for (clusterToAdd in clustersToAdd) {
            val markerToAdd = mapController.addMarker()
            markerToAdd.setPoint(LngLat(clusterToAdd.longitude, clusterToAdd.latitude))
            markerToAdd.setStylingFromString(
                "{ style: 'points', size: [27px, 27px], order: 2000, collide: false, color: blue}"
            )
            val markerIcon = getMarkerIcon(clusterToAdd)
            markerToAdd.setDrawable(BitmapDrawable(context.resources, markerIcon))
            markerToAdd.isVisible = true
            markerToAdd.userData = clusterToAdd
            markers[clusterToAdd] = markerToAdd
        }
    }

    private fun getMarkerIcon(cluster: Cluster<T>): Bitmap {
        val clusterItems = cluster.items
        return if (clusterItems.size > 1) iconGenerator.getClusterIcon(cluster)
        else iconGenerator.getClusterItemIcon(clusterItems[0])
    }
}
