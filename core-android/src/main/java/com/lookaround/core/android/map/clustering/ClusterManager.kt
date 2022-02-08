package com.lookaround.core.android.map.clustering

import android.content.Context
import com.lookaround.core.android.ext.screenAreaToBoundingBox
import com.lookaround.core.android.map.model.LatLon
import com.mapzen.tangram.MapChangeListener
import com.mapzen.tangram.MapController
import kotlin.coroutines.CoroutineContext
import kotlin.math.pow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@ExperimentalCoroutinesApi
class ClusterManager<T : ClusterItem>(
    context: Context,
    private val mapController: MapController,
    private val clusterItems: Iterable<T> = emptyList(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : MapChangeListener, CoroutineScope {
    private val supervisorJob = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = supervisorJob + dispatcher

    private val renderer: ClusterRenderer<T> = ClusterRenderer(context, mapController)

    private val clusterTrigger = MutableSharedFlow<Unit>()

    init {
        launch {
            val quadTree = QuadTree<T>(QUAD_TREE_BUCKET_CAPACITY)
            clusterItems.forEach(quadTree::insert)

            clusterTrigger
                .onStart { emit(Unit) }
                .mapLatest {
                    val boundingBox =
                        mapController.screenAreaToBoundingBox() ?: return@mapLatest null
                    getClusters(
                        quadTree = quadTree,
                        northEast = boundingBox.max,
                        southWest = boundingBox.min,
                        zoomLevel = mapController.cameraPosition.getZoom()
                    )
                }
                .filterNotNull()
                .onEach { withContext(Dispatchers.Main) { renderer.render(it) } }
                .launchIn(this)
        }
    }

    var minClusterSize = DEFAULT_MIN_CLUSTER_SIZE
        set(value) {
            require(value > 0)
            field = value
        }

    var callbacks: Callbacks<T>?
        get() = renderer.callbacks
        set(value) {
            renderer.callbacks = value
        }

    override fun onViewComplete() = Unit
    override fun onRegionWillChange(animated: Boolean) = Unit
    override fun onRegionIsChanging() = Unit
    override fun onRegionDidChange(animated: Boolean) {
        launch { clusterTrigger.emit(Unit) }
    }

    /**
     * Defines signatures for methods that are called when a cluster or a cluster item is clicked.
     *
     * @param <T> the type of an item managed by [ClusterManager]. </T>
     */
    interface Callbacks<T : ClusterItem> {
        /**
         * Called when a marker representing a cluster has been clicked.
         *
         * @param cluster the cluster that has been clicked
         * @return `true` if the listener has consumed the event (i.e., the default behavior should
         * not occur); `false` otherwise (i.e., the default behavior should occur). The default
         * behavior is for the camera to move to the marker and an info window to appear.
         */
        fun onClusterClick(cluster: Cluster<T>)

        /**
         * Called when a marker representing a cluster item has been clicked.
         *
         * @param clusterItem the cluster item that has been clicked
         * @return `true` if the listener has consumed the event (i.e., the default behavior should
         * not occur); `false` otherwise (i.e., the default behavior should occur). The default
         * behavior is for the camera to move to the marker and an info window to appear.
         */
        fun onClusterItemClick(clusterItem: T)
    }

    fun cancel() {
        supervisorJob.cancel()
    }

    private fun getClusters(
        quadTree: QuadTree<T>,
        northEast: LatLon,
        southWest: LatLon,
        zoomLevel: Float
    ): List<Cluster<T>> {
        val clusters = mutableListOf<Cluster<T>>()
        val tileCount = (2.0.pow(zoomLevel.toDouble()) * 2).toLong()
        val startLatitude = northEast.latitude
        val endLatitude = southWest.latitude
        val startLongitude = southWest.longitude
        val endLongitude = northEast.longitude
        val stepLatitude = 180.0 / tileCount
        val stepLongitude = 360.0 / tileCount
        if (startLongitude > endLongitude) { // Longitude +180°/-180° overlap.
            // [start longitude; 180]
            getClustersInsideBounds(
                quadTree,
                clusters,
                startLatitude,
                endLatitude,
                startLongitude,
                180.0,
                stepLatitude,
                stepLongitude
            )
            // [-180; end longitude]
            getClustersInsideBounds(
                quadTree,
                clusters,
                startLatitude,
                endLatitude,
                -180.0,
                endLongitude,
                stepLatitude,
                stepLongitude
            )
        } else {
            getClustersInsideBounds(
                quadTree,
                clusters,
                startLatitude,
                endLatitude,
                startLongitude,
                endLongitude,
                stepLatitude,
                stepLongitude
            )
        }
        return clusters
    }

    private fun getClustersInsideBounds(
        quadTree: QuadTree<T>,
        clusters: MutableList<Cluster<T>>,
        startLatitude: Double,
        endLatitude: Double,
        startLongitude: Double,
        endLongitude: Double,
        stepLatitude: Double,
        stepLongitude: Double
    ) {
        val startX = ((startLongitude + 180.0) / stepLongitude).toLong()
        val startY = ((90.0 - startLatitude) / stepLatitude).toLong()
        val endX = ((endLongitude + 180.0) / stepLongitude).toLong() + 1
        val endY = ((90.0 - endLatitude) / stepLatitude).toLong() + 1
        for (tileX in startX..endX) {
            for (tileY in startY..endY) {
                val north = 90.0 - tileY * stepLatitude
                val west = tileX * stepLongitude - 180.0
                val south = north - stepLatitude
                val east = west + stepLongitude
                val points = quadTree.queryRange(north, west, south, east)
                if (points.isEmpty()) continue
                if (points.size >= minClusterSize) {
                    var totalLatitude = 0.0
                    var totalLongitude = 0.0
                    for (point in points) {
                        totalLatitude += point.latitude
                        totalLongitude += point.longitude
                    }
                    val latitude = totalLatitude / points.size
                    val longitude = totalLongitude / points.size
                    clusters.add(Cluster(latitude, longitude, points, north, west, south, east))
                } else {
                    for (point in points) {
                        clusters.add(
                            Cluster(
                                point.latitude,
                                point.longitude,
                                listOf(point),
                                north,
                                west,
                                south,
                                east
                            )
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val QUAD_TREE_BUCKET_CAPACITY = 4
        private const val DEFAULT_MIN_CLUSTER_SIZE = 1
    }
}
