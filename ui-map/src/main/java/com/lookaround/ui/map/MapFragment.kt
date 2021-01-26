package com.lookaround.ui.map

import android.animation.TimeAnimator
import android.graphics.Bitmap
import android.graphics.Interpolator
import android.graphics.PointF
import android.graphics.RectF
import android.hardware.camera2.CameraManager
import android.os.Looper
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.fragment.app.Fragment
import com.lookaround.core.android.delegate.viewBinding
import com.lookaround.ui.map.databinding.FragmentMapBinding
import com.mapzen.tangram.*
import com.mapzen.tangram.networking.HttpHandler
import com.mapzen.tangram.viewholder.GLSurfaceViewHolderFactory
import com.mapzen.tangram.viewholder.GLViewHolder
import com.mapzen.tangram.viewholder.GLViewHolderFactory
import java.lang.Math.log10
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MapFragment : Fragment(R.layout.fragment_map) {
    private val binding: FragmentMapBinding by viewBinding(FragmentMapBinding::bind)

    override fun onDestroyView() {
        binding.map.onDestroy()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        binding.map.onResume()
    }

    override fun onPause() {
        binding.map.onPause()
        super.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.map.onLowMemory()
    }
}

class KtMapController(private val c: MapController, contentResolver: ContentResolver) {
    private val cameraManager = CameraManager(c, contentResolver)
    private val markerManager = MarkerManager(c)
    private val gestureManager = TouchGestureManager(c)

    private val defaultInterpolator = AccelerateDecelerateInterpolator()

    private val sceneUpdateContinuations = mutableMapOf<Int, Continuation<Int>>()
    private val pickLabelContinuations = ConcurrentLinkedQueue<Continuation<LabelPickResult?>>()
    private val featurePickContinuations = ConcurrentLinkedQueue<Continuation<FeaturePickResult?>>()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var mapChangingListener: MapChangingListener? = null

    private val flingAnimator: TimeAnimator = TimeAnimator()

    init {
        c.setSceneLoadListener { sceneId, sceneError ->
            val cont = sceneUpdateContinuations.remove(sceneId)
            if (sceneError != null) {
                cont?.resumeWithException(sceneError.toException())
            } else {
                markerManager.recreateMarkers()
                cont?.resume(sceneId)
            }
        }

        c.setLabelPickListener { labelPickResult: LabelPickResult? ->
            pickLabelContinuations.poll()?.resume(labelPickResult)
        }

        c.setFeaturePickListener { featurePickResult: FeaturePickResult? ->
            featurePickContinuations.poll()?.resume(featurePickResult)
        }

        flingAnimator.setTimeListener { _, _, _ ->
            mapChangingListener?.onMapIsChanging()
        }

        cameraManager.listener = object : CameraManager.AnimationsListener {
            override fun onAnimationsStarted() {
                mapChangingListener?.onMapWillChange()
            }

            override fun onAnimating() {
                mapChangingListener?.onMapIsChanging()
            }

            override fun onAnimationsEnded() {
                mapChangingListener?.onMapDidChange()
            }
        }

        c.setMapChangeListener(object : MapChangeListener {
            private var calledOnMapIsChangingOnce = false

            override fun onViewComplete() { /* not interested*/
            }

            override fun onRegionWillChange(animated: Boolean) {
                // may not be called on ui thread, see https://github.com/tangrams/tangram-es/issues/2157
                mainHandler.post {
                    calledOnMapIsChangingOnce = false
                    if (!cameraManager.isAnimating) {
                        mapChangingListener?.onMapWillChange()
                        if (animated) flingAnimator.start()
                    }
                }
            }

            override fun onRegionIsChanging() {
                mainHandler.post {
                    if (!cameraManager.isAnimating) mapChangingListener?.onMapIsChanging()
                    calledOnMapIsChangingOnce = true
                }
            }

            override fun onRegionDidChange(animated: Boolean) {
                mainHandler.post {
                    if (!cameraManager.isAnimating) {
                        if (!calledOnMapIsChangingOnce) mapChangingListener?.onMapIsChanging()
                        mapChangingListener?.onMapDidChange()
                        if (animated) flingAnimator.end()
                    }
                }
            }
        })
    }

    /* ----------------------------- Loading and Updating Scene --------------------------------- */

    suspend fun loadSceneFile(
        path: String,
        sceneUpdates: List<SceneUpdate>? = null
    ): Int = suspendCoroutine { cont ->
        markerManager.invalidateMarkers()
        val sceneId = c.loadSceneFileAsync(path, sceneUpdates)
        sceneUpdateContinuations[sceneId] = cont
    }

    suspend fun loadSceneYaml(
        yaml: String,
        resourceRoot: String,
        sceneUpdates: List<SceneUpdate>? = null
    ): Int = suspendCoroutine { cont ->
        markerManager.invalidateMarkers()
        val sceneId = c.loadSceneYamlAsync(yaml, resourceRoot, sceneUpdates)
        sceneUpdateContinuations[sceneId] = cont
    }

    /* ----------------------------------------- Camera ----------------------------------------- */

    val cameraPosition: CameraPosition get() = cameraManager.camera

    fun updateCameraPosition(
        duration: Long = 0,
        interpolator: Interpolator = defaultInterpolator,
        builder: CameraUpdate.() -> Unit
    ) {
        updateCameraPosition(duration, interpolator, CameraUpdate().apply(builder))
    }

    fun updateCameraPosition(
        duration: Long = 0,
        interpolator: Interpolator = defaultInterpolator,
        update: CameraUpdate
    ) {
        cameraManager.updateCamera(duration, interpolator, update)
    }

    fun setCameraPosition(camera: CameraPosition) {
        val update = CameraUpdate()
        update.position = camera.position
        update.rotation = camera.rotation
        update.tilt = camera.tilt
        update.zoom = camera.zoom
        updateCameraPosition(0L, defaultInterpolator, update)
    }

    fun cancelAllCameraAnimations() = cameraManager.cancelAllCameraAnimations()

    var cameraType: MapController.CameraType
        set(value) {
            c.cameraType = value
        }
        get() = c.cameraType

    var minimumZoomLevel: Float
        set(value) {
            c.minimumZoomLevel = value
        }
        get() = c.minimumZoomLevel

    var maximumZoomLevel: Float
        set(value) {
            c.maximumZoomLevel = value
        }
        get() = c.maximumZoomLevel

    fun screenPositionToLatLon(screenPosition: PointF): LatLon? =
        c.screenPositionToLngLat(screenPosition)?.toLatLon()

    fun latLonToScreenPosition(latLon: LatLon): PointF = c.lngLatToScreenPosition(latLon.toLngLat())
    fun latLonToScreenPosition(latLon: LatLon, screenPositionOut: PointF, clipToViewport: Boolean) =
        c.lngLatToScreenPosition(latLon.toLngLat(), screenPositionOut, clipToViewport)

    fun screenCenterToLatLon(padding: RectF): LatLon? {
        val view = glViewHolder?.view ?: return null
        val w = view.width
        val h = view.height
        if (w == 0 || h == 0) return null

        return screenPositionToLatLon(
            PointF(
                padding.left + (w - padding.left - padding.right) / 2f,
                padding.top + (h - padding.top - padding.bottom) / 2f
            )
        )
    }

    fun screenAreaToBoundingBox(padding: RectF): BoundingBox? {
        val view = glViewHolder?.view ?: return null
        val w = view.width
        val h = view.height
        if (w == 0 || h == 0) return null

        val size = PointF(w - padding.left - padding.right, h - padding.top - padding.bottom)

        // the special cases here are: map tilt and map rotation:
        // * map tilt makes the screen area -> world map area into a trapezoid
        // * map rotation makes the screen area -> world map area into a rotated rectangle
        // dealing with tilt: this method is just not defined if the tilt is above a certain limit
        if (cameraPosition.tilt > Math.PI / 4f) return null // 45°

        val positions = arrayOf(
            screenPositionToLatLon(PointF(padding.left, padding.top)),
            screenPositionToLatLon(PointF(padding.left + size.x, padding.top)),
            screenPositionToLatLon(PointF(padding.left, padding.top + size.y)),
            screenPositionToLatLon(PointF(padding.left + size.x, padding.top + size.y))
        ).filterNotNull()

        return positions.enclosingBoundingBox()
    }

    fun getEnclosingCameraPosition(bounds: BoundingBox, padding: RectF): CameraPosition? {
        val zoom = getMaxZoomThatContainsBounds(bounds, padding) ?: return null
        val boundsCenter = listOf(bounds.min, bounds.max).centerPointOfPolyline()
        val pos = getLatLonThatCentersLatLon(boundsCenter, padding, zoom) ?: return null
        val camera = cameraPosition
        return CameraPosition(pos, camera.rotation, camera.tilt, zoom)
    }

    private fun getMaxZoomThatContainsBounds(bounds: BoundingBox, padding: RectF): Float? {
        val screenBounds: BoundingBox
        val currentZoom: Float
        synchronized(c) {
            screenBounds = screenAreaToBoundingBox(padding) ?: return null
            currentZoom = cameraPosition.zoom
        }
        val screenWidth = normalizeLongitude(screenBounds.maxLongitude - screenBounds.minLongitude)
        val screenHeight = screenBounds.maxLatitude - screenBounds.minLatitude
        val objectWidth = normalizeLongitude(bounds.maxLongitude - bounds.minLongitude)
        val objectHeight = bounds.maxLatitude - bounds.minLatitude

        val zoomDeltaX = log10(screenWidth / objectWidth) / log10(2.0)
        val zoomDeltaY = log10(screenHeight / objectHeight) / log10(2.0)
        val zoomDelta = min(zoomDeltaX, zoomDeltaY)
        return max(1.0, min(currentZoom + zoomDelta, 21.0)).toFloat()
    }

    fun getLatLonThatCentersLatLon(
        position: LatLon,
        padding: RectF,
        zoom: Float = cameraPosition.zoom
    ): LatLon? {
        val view = glViewHolder?.view ?: return null
        val w = view.width
        val h = view.height
        if (w == 0 || h == 0) return null

        val screenCenter = screenPositionToLatLon(PointF(w / 2f, h / 2f)) ?: return null
        val offsetScreenCenter = screenPositionToLatLon(
            PointF(
                padding.left + (w - padding.left - padding.right) / 2,
                padding.top + (h - padding.top - padding.bottom) / 2
            )
        ) ?: return null

        val zoomDelta = zoom.toDouble() - cameraPosition.zoom
        val distance = offsetScreenCenter.distanceTo(screenCenter)
        val angle = offsetScreenCenter.initialBearingTo(screenCenter)
        val distanceAfterZoom = distance * (2.0).pow(-zoomDelta)
        return position.translate(distanceAfterZoom, angle)
    }

    /* -------------------------------------- Data Layers --------------------------------------- */

    fun addDataLayer(name: String, generateCentroid: Boolean = false): MapData =
        c.addDataLayer(name, generateCentroid)

    /* ---------------------------------------- Markers ----------------------------------------- */

    fun addMarker(): Marker = markerManager.addMarker()
    fun removeMarker(marker: Marker): Boolean = removeMarker(marker.markerId)
    fun removeMarker(markerId: Long): Boolean = markerManager.removeMarker(markerId)
    fun removeAllMarkers() = markerManager.removeAllMarkers()

    /* ------------------------------------ Map interaction ------------------------------------- */

    fun setPickRadius(radius: Float) = c.setPickRadius(radius)

    suspend fun pickLabel(posX: Float, posY: Float): LabelPickResult? = suspendCoroutine { cont ->
        pickLabelContinuations.offer(cont)
        c.pickLabel(posX, posY)
    }

    suspend fun pickMarker(posX: Float, posY: Float): MarkerPickResult? =
        markerManager.pickMarker(posX, posY)

    suspend fun pickFeature(posX: Float, posY: Float): FeaturePickResult? =
        suspendCoroutine { cont ->
            featurePickContinuations.offer(cont)
            c.pickFeature(posX, posY)
        }

    fun setMapChangingListener(listener: MapChangingListener?) {
        mapChangingListener = listener
    }

    /* -------------------------------------- Touch input --------------------------------------- */

    fun setShoveResponder(responder: TouchInput.ShoveResponder?) {
        gestureManager.setShoveResponder(responder)
    }

    fun setScaleResponder(responder: TouchInput.ScaleResponder?) {
        gestureManager.setScaleResponder(responder)
    }

    fun setRotateResponder(responder: TouchInput.RotateResponder?) {
        gestureManager.setRotateResponder(responder)
    }

    fun setPanResponder(responder: TouchInput.PanResponder?) {
        gestureManager.setPanResponder(responder)
    }

    fun setTapResponder(responder: TouchInput.TapResponder?) {
        c.touchInput.setTapResponder(responder)
    }

    fun setDoubleTapResponder(responder: TouchInput.DoubleTapResponder?) {
        c.touchInput.setDoubleTapResponder(responder)
    }

    fun setLongPressResponder(responder: TouchInput.LongPressResponder?) {
        c.touchInput.setLongPressResponder(responder)
    }

    fun isGestureEnabled(g: TouchInput.Gestures): Boolean = c.touchInput.isGestureEnabled(g)
    fun setGestureEnabled(g: TouchInput.Gestures) {
        c.touchInput.setGestureEnabled(g)
    }

    fun setGestureDisabled(g: TouchInput.Gestures) {
        c.touchInput.setGestureDisabled(g)
    }

    fun setAllGesturesEnabled() {
        c.touchInput.setAllGesturesEnabled()
    }

    fun setAllGesturesDisabled() {
        c.touchInput.setAllGesturesDisabled()
    }

    fun setSimultaneousDetectionEnabled(first: TouchInput.Gestures, second: TouchInput.Gestures) {
        c.touchInput.setSimultaneousDetectionEnabled(first, second)
    }

    fun setSimultaneousDetectionDisabled(first: TouchInput.Gestures, second: TouchInput.Gestures) {
        c.touchInput.setSimultaneousDetectionDisabled(first, second)
    }

    fun isSimultaneousDetectionAllowed(
        first: TouchInput.Gestures,
        second: TouchInput.Gestures
    ): Boolean =
        c.touchInput.isSimultaneousDetectionAllowed(first, second)

    /* ------------------------------------------ Misc ------------------------------------------ */

    suspend fun captureFrame(waitForCompleteView: Boolean): Bitmap = suspendCoroutine { cont ->
        c.captureFrame({ bitmap -> cont.resume(bitmap) }, waitForCompleteView)
    }

    fun requestRender() = c.requestRender()
    fun setRenderMode(renderMode: Int) = c.setRenderMode(renderMode)

    fun queueEvent(block: () -> Unit) = c.queueEvent(block)

    val glViewHolder: GLViewHolder? get() = c.glViewHolder

    fun setDebugFlag(flag: MapController.DebugFlag, on: Boolean) = c.setDebugFlag(flag, on)

    fun useCachedGlState(use: Boolean) = c.useCachedGlState(use)

    fun setDefaultBackgroundColor(red: Float, green: Float, blue: Float) =
        c.setDefaultBackgroundColor(red, green, blue)
}

class LoadSceneException(message: String, val sceneUpdate: SceneUpdate) : RuntimeException(message)

private fun SceneError.toException() =
    LoadSceneException(error.name.toLowerCase(Locale.US).replace("_", " "), sceneUpdate)


suspend fun MapView.initMap(
    httpHandler: HttpHandler? = null,
    glViewHolderFactory: GLViewHolderFactory = GLSurfaceViewHolderFactory()
) =

    suspendCoroutine<KtMapController?> { cont ->
        getMapAsync({ mapController ->
            cont.resume(mapController?.let {
                KtMapController(it, context.contentResolver)
            })
        }, glViewHolderFactory, httpHandler)
    }