package com.lookaround.ui.map

import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.hoko.blur.HokoBlur
import com.hoko.blur.processor.BlurProcessor
import com.imxie.exvpbs.ViewPagerBottomSheetBehavior
import com.lookaround.core.android.ar.orientation.Orientation
import com.lookaround.core.android.ar.orientation.OrientationManager
import com.lookaround.core.android.ext.*
import com.lookaround.core.android.ext.MarkerPickResult
import com.lookaround.core.android.map.UserLocationMapComponent
import com.lookaround.core.android.map.clustering.ClusterManager
import com.lookaround.core.android.map.clustering.DefaultClusterItem
import com.lookaround.core.android.map.scene.MapSceneViewModel
import com.lookaround.core.android.map.scene.model.MapScene
import com.lookaround.core.android.map.scene.model.MapSceneIntent
import com.lookaround.core.android.map.scene.model.MapSceneSignal
import com.lookaround.core.android.model.Marker
import com.lookaround.core.android.model.Ready
import com.lookaround.core.android.model.WithValue
import com.lookaround.core.android.model.hasNoValueOrEmpty
import com.lookaround.core.delegate.lazyAsync
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.locationReadyUpdates
import com.lookaround.ui.main.model.MainSignal
import com.lookaround.ui.main.model.MainState
import com.lookaround.ui.main.userLocationFabVisibilityUpdates
import com.lookaround.ui.map.databinding.FragmentMapBinding
import com.mapzen.tangram.*
import com.mapzen.tangram.networking.HttpHandler
import com.mapzen.tangram.viewholder.GLViewHolderFactory
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.math.PI
import kotlin.math.abs
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
@WithFragmentBindings
class MapFragment :
    Fragment(R.layout.fragment_map),
    MapController.SceneLoadListener,
    MarkerPickListener,
    MapChangeListener,
    OrientationManager.OnOrientationChangedListener {
    private val binding: FragmentMapBinding by viewBinding(FragmentMapBinding::bind)

    private val mapSceneViewModel: MapSceneViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()

    @Inject internal lateinit var mapTilesHttpHandler: HttpHandler
    @Inject internal lateinit var glViewHolderFactory: GLViewHolderFactory
    private val mapController: Deferred<MapController> by
        lifecycleScope.lazyAsync { binding.map.init(mapTilesHttpHandler, glViewHolderFactory) }
    private val mapReady = CompletableDeferred<Unit>()

    private val orientationManager: OrientationManager by
        lazy(LazyThreadSafetyMode.NONE) {
            OrientationManager(requireContext()).apply {
                axisMode = OrientationManager.Mode.COMPASS
                onOrientationChangedListener = this@MapFragment
            }
        }

    private var userLocationMapComponent: UserLocationMapComponent? = null

    private val markerArgument: Marker? by nullableArgument(Arguments.MARKER.name)
    private var currentMarker: Marker? = null
    private var unsetCurrentMarker: Boolean = true

    private var clusterManager: ClusterManager<DefaultClusterItem>? = null

    private val markerPickContinuations = ConcurrentLinkedQueue<Continuation<MarkerPickResult?>>()

    private var latestMarkers: Iterable<Marker>? = null

    private val blurProcessor: BlurProcessor by
        lazy(LazyThreadSafetyMode.NONE) {
            HokoBlur.with(requireContext())
                .sampleFactor(8f)
                .scheme(HokoBlur.SCHEME_OPENGL)
                .mode(HokoBlur.MODE_GAUSSIAN)
                .radius(10)
                .processor()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentMarker =
            if (savedInstanceState == null) markerArgument
            else savedInstanceState.getParcelable(SavedStateKeys.CURRENT_MARKER.name)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mainViewModel.state.bitmapCache.get(MainState.BlurredBackgroundType.MAP)?.let {
            (blurredBackground) ->
            binding.blurBackground.background = BitmapDrawable(resources, blurredBackground)
        }

        lifecycleScope.launchWhenResumed {
            mainViewModel.signal(
                if (mainViewModel.state.lastLiveBottomSheetState !=
                        ViewPagerBottomSheetBehavior.STATE_EXPANDED
                ) {
                    MainSignal.ToggleSearchBarVisibility(View.VISIBLE)
                } else {
                    MainSignal.BottomSheetStateChanged(ViewPagerBottomSheetBehavior.STATE_EXPANDED)
                }
            )
        }

        mapController.launch {
            setSceneLoadListener(this@MapFragment)
            setMapChangeListener(this@MapFragment)
            setMarkerPickListener(this@MapFragment)
            loadScene(if (requireContext().darkMode) MapScene.DARK else MapScene.LIGHT)
            setSingleTapResponder()
            zoomOnDoubleTap()
            val cameraPositionInitialized = initCameraPosition(savedInstanceState)
            syncMarkerChangesWithMap(cameraPositionInitialized)
        }

        initBlurringOnBottomSheetStateChanged()

        mapSceneViewModel
            .onEachSignal(MapSceneSignal.RetryLoadScene::scene) { scene ->
                mapController.await().loadScene(scene)
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        mainViewModel
            .onEachSignal(MainSignal.UpdateSelectedMarker::marker, ::updateCurrentMarker)
            .launchIn(viewLifecycleOwner.lifecycleScope)

        mainViewModel
            .filterSignals<MainSignal.TopFragmentChanged>()
            .drop(1)
            .filter { (clazz) -> clazz.isAssignableFrom(this::class.java) && this.view != null }
            .onEach {
                latestMarkers?.let {
                    mapController.launch {
                        removeAllMarkers()
                        userLocationMapComponent = UserLocationMapComponent(requireContext(), this)
                        addMarkerClusters(it)
                    }
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        initUserLocationFab()
        initCompassFab()
    }

    override fun onDestroyView() {
        clusterManager?.cancel()
        clusterManager = null
        super.onDestroyView()
        binding.map.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        binding.map.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.map.onPause()
        orientationManager.stopSensor()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.map.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        currentMarker?.let { outState.putParcelable(SavedStateKeys.CURRENT_MARKER.name, it) }
        mapController.launch { saveCameraPosition(outState) }
    }

    override fun onSceneReady(sceneId: Int, sceneError: SceneError?) {
        if (view == null) return

        if (sceneError == null) {
            viewLifecycleOwner.lifecycleScope.launch {
                mapSceneViewModel.intent(MapSceneIntent.SceneLoaded)
            }

            binding.shimmerLayout.stopAndHide()
            binding.blurBackground.visibility = View.GONE
        } else {
            Timber.e("Failed to load scene: $sceneId. Scene error: $sceneError")
        }
    }

    override fun onViewComplete() {
        if (mapReady.isCompleted) return else mapReady.complete(Unit)
    }

    override fun onRegionWillChange(animated: Boolean) = Unit

    override fun onRegionIsChanging() {
        mapController.launch { updateCompassFab() }
    }

    override fun onRegionDidChange(animated: Boolean) {
        if (unsetCurrentMarker) currentMarker = null else unsetCurrentMarker = true
        clusterManager?.onRegionDidChange(animated)
        mapController.launch {
            userLocationMapComponent?.currentMapZoom = cameraPosition.zoom
            updateCompassFab()
        }
        if (!isRunningOnEmulator()) updateBlurBackground()
    }

    private fun MapController.updateCompassFab() {
        binding.compassFab.rotation = (180 * cameraPosition.rotation / PI).toFloat()
        binding.compassFab.rotationX = (180 * cameraPosition.tilt / PI).toFloat()
        val margin = 2 * PI / 180
        binding.compassFab.visibility =
            if (abs(cameraPosition.rotation) < margin && cameraPosition.tilt < margin) View.GONE
            else View.VISIBLE
    }

    override fun onMarkerPickComplete(result: TangramMarkerPickResult?) {
        val markerPickResult = clusterManager?.onMarkerPickComplete(result)
        markerPickContinuations.poll()?.resume(markerPickResult)
    }

    override fun onOrientationChanged(orientation: Orientation) {
        userLocationMapComponent?.rotation = orientation.azimuth.toDouble()
    }

    private suspend fun pickMarker(posX: Float, posY: Float): MarkerPickResult? =
        suspendCancellableCoroutine { continuation ->
            markerPickContinuations.offer(continuation)
            continuation.invokeOnCancellation { markerPickContinuations.remove(continuation) }
            mapController.launch { pickMarker(posX, posY) }
        }

    fun updateCurrentMarker(marker: Marker?) {
        if (marker != null) {
            unsetCurrentMarker = false
            currentMarker = marker
            mapController.launch {
                moveCameraPositionTo(
                    lat = marker.location.latitude,
                    lng = marker.location.longitude,
                    zoom =
                        if (MARKER_FOCUSED_ZOOM > cameraPosition.zoom) MARKER_FOCUSED_ZOOM
                        else cameraPosition.zoom,
                    durationMs = CAMERA_POSITION_ANIMATION_DURATION_MS
                )
            }
        } else {
            currentMarker = null
        }
    }

    private suspend fun MapController.initCameraPosition(savedInstanceState: Bundle?): Boolean {
        mapReady.await()

        if (savedInstanceState != null) {
            restoreCameraPosition(savedInstanceState)
            return true
        }

        currentMarker?.let {
            moveCameraPositionTo(
                lat = it.location.latitude,
                lng = it.location.longitude,
                zoom = MARKER_FOCUSED_ZOOM
            )
            return true
        }

        if (mainViewModel.state.markers.hasNoValueOrEmpty()) {
            val location = mainViewModel.state.locationState
            if (location is WithValue<Location>) {
                moveCameraPositionTo(
                    lat = location.value.latitude,
                    lng = location.value.longitude,
                    zoom = USER_LOCATION_FOCUSED_ZOOM
                )
                return true
            }
        }

        return false
    }

    private suspend fun MapController.loadScene(scene: MapScene) {
        binding.blurBackground.visibility = View.VISIBLE
        binding.shimmerLayout.showAndStart()

        mapSceneViewModel.intent(MapSceneIntent.LoadingScene(scene))
        loadSceneFile(
            scene.path,
            listOf(SceneUpdate("global.sdk_api_key", BuildConfig.NEXTZEN_API_KEY))
        )
    }

    private fun MapController.addMarkerClusters(markers: Iterable<Marker>) {
        val clusterItems =
            markers.map { DefaultClusterItem(latLon = it.location.latLon, extra = it) }
        clusterManager?.cancel()
        clusterManager = ClusterManager(requireContext(), this, clusterItems)
    }

    private fun MapController.calculateAndZoomToBoundsOf(locations: Iterable<Location>) {
        val north = locations.maxOf(Location::getLatitude)
        val south = locations.minOf(Location::getLatitude)
        val west = locations.minOf(Location::getLongitude)
        val east = locations.maxOf(Location::getLongitude)
        updateCameraPosition(
            CameraUpdateFactory.newLngLatBounds(
                LngLat(west, south),
                LngLat(east, north),
                EdgePadding(10, 10, 10, 10)
            ),
        )
    }

    private fun MapController.setSingleTapResponder() {
        touchInput.setTapResponder(
            object : TouchInput.TapResponder {
                override fun onSingleTapUp(x: Float, y: Float): Boolean = false
                override fun onSingleTapConfirmed(x: Float, y: Float): Boolean {
                    viewLifecycleOwner.lifecycleScope.launchWhenResumed {
                        val pickResult = this@MapFragment.pickMarker(posX = x, posY = y)
                        if (pickResult != null) {
                            val marker = pickResult.extra as? Marker
                            if (marker != null) {
                                updateCurrentMarker(marker)
                                mainViewModel.signal(MainSignal.ShowPlaceInBottomSheet(marker.id))
                            } else {
                                moveCameraPositionTo(
                                    lat = pickResult.position.latitude,
                                    lng = pickResult.position.longitude,
                                    zoom = mapController.await().cameraPosition.zoom + 1f,
                                    durationMs = CAMERA_POSITION_ANIMATION_DURATION_MS
                                )
                            }
                        } else {
                            val targetVisibility = binding.visibilityToggleView.toggleVisibility()
                            mainViewModel.signal(
                                MainSignal.ToggleSearchBarVisibility(targetVisibility)
                            )
                            mainViewModel.signal(MainSignal.HidePlacesListBottomSheet)
                            updateCurrentMarker(null)
                        }
                    }
                    return true
                }
            }
        )
    }

    private fun syncMarkerChangesWithMap(cameraPositionInitialized: Boolean) {
        mainViewModel
            .mapStates(MainState::markers)
            .distinctUntilChanged()
            .withIndex()
            .onEach { (index, loadable) ->
                mapReady.await()

                mapController.launch {
                    removeAllMarkers()
                    initUserLocationComponent()
                    if (loadable !is WithValue) return@launch

                    val markers = loadable.value.items
                    latestMarkers = markers
                    val changeCameraPosition = index != 0 || !cameraPositionInitialized
                    if (markers.size > 0) {
                        if (changeCameraPosition) {
                            if (markers.size > 1) {
                                calculateAndZoomToBoundsOf(markers.map(Marker::location::get))
                            } else {
                                val marker = markers.first()
                                moveCameraPositionTo(
                                    lat = marker.location.latitude,
                                    lng = marker.location.longitude,
                                    zoom = MARKER_FOCUSED_ZOOM
                                )
                            }
                        }
                        addMarkerClusters(markers)
                    }
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private var userLocationComponentJob: Job? = null
    private fun MapController.initUserLocationComponent() {
        userLocationComponentJob?.cancel()
        userLocationMapComponent = UserLocationMapComponent(requireContext(), this)
        userLocationComponentJob =
            mainViewModel.locationReadyUpdates
                .onEach { userLocationMapComponent?.location = it }
                .launchIn(lifecycleScope)
        orientationManager.startSensor(requireContext())
    }

    private fun initUserLocationFab() {
        binding.userLocationFab.setOnClickListener {
            when (val userLocationState = mainViewModel.state.locationState) {
                is Ready -> {
                    mapController.launch {
                        moveCameraPositionTo(
                            lat = userLocationState.value.latitude,
                            lng = userLocationState.value.longitude,
                            zoom = cameraPosition.zoom
                        )
                    }
                }
                else -> return@setOnClickListener
            }
        }

        mainViewModel.userLocationFabVisibilityUpdates
            .onEach { binding.userLocationFab.visibility = if (it) View.VISIBLE else View.GONE }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun followUserLocation() {
        mainViewModel.locationReadyUpdates
            .onEach { location ->
                mapController.launch {
                    moveCameraPositionTo(
                        lat = location.latitude,
                        lng = location.longitude,
                        zoom = MARKER_FOCUSED_ZOOM
                    )
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun initCompassFab() {
        binding.compassFab.setOnClickListener {
            mapController.launch {
                updateCameraPosition(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition().apply {
                            set(cameraPosition)
                            tilt = 0f
                            rotation = 0f
                        }
                    )
                )
            }
        }
    }

    private var blurBackgroundJob: Job? = null
    private fun updateBlurBackground() {
        blurBackgroundJob?.cancel()
        blurBackgroundJob =
            viewLifecycleOwner.lifecycleScope.launch {
                mapReady.await()

                val bitmap = mapController.await().captureFrame(true)
                val (blurred, palette) =
                    withContext(Dispatchers.Default) {
                        val blurred = blurProcessor.blur(bitmap)
                        blurred to blurred.palette
                    }
                mainViewModel.state.bitmapCache.put(
                    MainState.BlurredBackgroundType.MAP,
                    blurred to palette
                )
                val blurBackgroundDrawable = BitmapDrawable(resources, blurred)
                binding.blurBackground.background = blurBackgroundDrawable
                mainViewModel.signal(
                    MainSignal.BlurBackgroundUpdated(blurBackgroundDrawable, palette)
                )
                val dominantSwatch =
                    withContext(Dispatchers.Default) { bitmap.dominantSwatch } ?: return@launch
                val contrastingColor = colorContrastingTo(dominantSwatch.rgb)
                mainViewModel.signal(MainSignal.ContrastingColorUpdated(contrastingColor))
            }
    }

    private fun initBlurringOnBottomSheetStateChanged() {
        if (isRunningOnEmulator()) return

        mainViewModel
            .filterSignals(MainSignal.BottomSheetStateChanged::state)
            .onEach { sheetState ->
                binding.blurBackground.fadeSetVisibility(
                    when (sheetState) {
                        ViewPagerBottomSheetBehavior.STATE_EXPANDED -> View.VISIBLE
                        ViewPagerBottomSheetBehavior.STATE_HIDDEN -> View.GONE
                        else -> return@onEach
                    }
                )
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun Deferred<MapController>.launch(block: suspend MapController.() -> Unit) {
        if (view == null) return
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main.immediate) {
            this@launch.await().block()
        }
    }

    companion object {
        private const val USER_LOCATION_FOCUSED_ZOOM = 14f
        private const val MARKER_FOCUSED_ZOOM = 17f
        private const val CAMERA_POSITION_ANIMATION_DURATION_MS = 150

        enum class SavedStateKeys {
            CURRENT_MARKER
        }

        enum class Arguments {
            MARKER
        }

        fun new(marker: Marker): MapFragment =
            MapFragment().apply { arguments = bundleOf(Arguments.MARKER.name to marker) }
    }
}
