package com.lookaround.ui.map

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.hoko.blur.HokoBlur
import com.hoko.blur.processor.BlurProcessor
import com.imxie.exvpbs.ViewPagerBottomSheetBehavior
import com.lookaround.core.android.ext.*
import com.lookaround.core.android.ext.MarkerPickResult
import com.lookaround.core.android.map.clustering.ClusterManager
import com.lookaround.core.android.map.clustering.DefaultClusterItem
import com.lookaround.core.android.map.scene.MapSceneViewModel
import com.lookaround.core.android.map.scene.model.MapScene
import com.lookaround.core.android.map.scene.model.MapSceneIntent
import com.lookaround.core.android.map.scene.model.MapSceneSignal
import com.lookaround.core.android.model.Marker
import com.lookaround.core.android.model.WithValue
import com.lookaround.core.android.model.hasNoValueOrEmpty
import com.lookaround.core.delegate.lazyAsync
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.model.MainSignal
import com.lookaround.ui.main.model.MainState
import com.lookaround.ui.map.databinding.FragmentMapBinding
import com.mapzen.tangram.*
import com.mapzen.tangram.networking.HttpHandler
import com.mapzen.tangram.viewholder.GLViewHolderFactory
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
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
    MapChangeListener {
    private val binding: FragmentMapBinding by viewBinding(FragmentMapBinding::bind)

    private val mapSceneViewModel: MapSceneViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()

    @Inject internal lateinit var mapTilesHttpHandler: HttpHandler
    @Inject internal lateinit var glViewHolderFactory: GLViewHolderFactory
    private val mapController: Deferred<MapController> by
        lifecycleScope.lazyAsync { binding.map.init(mapTilesHttpHandler, glViewHolderFactory) }
    private val mapReady = CompletableDeferred<Unit>()

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
        mainViewModel.state.bitmapCache.get(javaClass.name)?.let { blurredBackground ->
            binding.blurBackground.background = BitmapDrawable(resources, blurredBackground)
        }

        lifecycleScope.launchWhenResumed {
            mainViewModel.signal(MainSignal.ToggleSearchBarVisibility(View.VISIBLE))
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
            .onEachSignal(MainSignal.CaptureMapImage::marker) { marker ->
                mapController.launch {
                    removeAllMarkers()
                    addMarkerFor(marker.location)
                    val markerImage = captureFrame(false)
                    mainViewModel.signal(MainSignal.ShowPlaceFragment(marker, markerImage))
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        mainViewModel
            .filterSignals<MainSignal.TopFragmentChanged>()
            .drop(1)
            .filter { (clazz) -> clazz.isAssignableFrom(this::class.java) && this.view != null }
            .onEach {
                latestMarkers?.let {
                    mapController.launch {
                        removeAllMarkers()
                        addMarkerClusters(it)
                    }
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        binding.navigateFab.setOnClickListener { launchGoogleMapsForNavigation() }
        binding.streetViewFab.setOnClickListener { launchGoogleMapsForStreetView() }
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
            if (currentMarker != null) showFABs() else hideFABs()

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
    override fun onRegionIsChanging() = Unit
    override fun onRegionDidChange(animated: Boolean) {
        if (unsetCurrentMarker) currentMarker = null else unsetCurrentMarker = true
        clusterManager?.onRegionDidChange(animated)
        if (!isRunningOnEmulator()) updateBlurBackground()
    }

    override fun onMarkerPickComplete(result: TangramMarkerPickResult?) {
        val markerPickResult = clusterManager?.onMarkerPickComplete(result)
        markerPickContinuations.poll()?.resume(markerPickResult)
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
            showFABs()
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
            hideFABs()
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
        hideFABs()
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

    private fun showFABs() {
        binding.streetViewFab.visibility = View.VISIBLE
        binding.navigateFab.visibility = View.VISIBLE
    }

    private fun hideFABs() {
        binding.streetViewFab.visibility = View.GONE
        binding.navigateFab.visibility = View.GONE
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

    private fun followUserLocation() {
        mainViewModel
            .states
            .map(MainState::locationState::get)
            .filterIsInstance<WithValue<Location>>()
            .distinctUntilChangedBy { Objects.hash(it.value.latitude, it.value.longitude) }
            .onEach { location ->
                mapController.launch {
                    moveCameraPositionTo(
                        lat = location.value.latitude,
                        lng = location.value.longitude,
                        zoom = MARKER_FOCUSED_ZOOM
                    )
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun launchGoogleMapsForNavigation() {
        launchGoogleMapForCurrentMarker(
            failureMsgRes = R.string.unable_to_launch_google_maps_for_navigation
        ) { location -> "google.navigation:q=${location.latitude},${location.longitude}" }
    }

    private fun launchGoogleMapsForStreetView() {
        launchGoogleMapForCurrentMarker(failureMsgRes = R.string.unable_to_launch_street_view) {
            location ->
            "google.streetview:cbll=${location.latitude},${location.longitude}"
        }
    }

    private fun launchGoogleMapForCurrentMarker(
        @StringRes failureMsgRes: Int,
        uriStringFor: (Location) -> String
    ) {
        currentMarker?.let {
            val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStringFor(it.location)))
            mapIntent.setPackage("com.google.android.apps.maps")
            try {
                startActivity(mapIntent)
            } catch (ex: ActivityNotFoundException) {
                Toast.makeText(requireContext(), getString(failureMsgRes), Toast.LENGTH_SHORT)
                    .show()
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
                val blurredBackground =
                    withContext(Dispatchers.Default) { blurProcessor.blur(bitmap) }
                mainViewModel.state.bitmapCache.put(
                    this@MapFragment.javaClass.name,
                    blurredBackground
                )
                val blurBackgroundDrawable = BitmapDrawable(resources, blurredBackground)
                binding.blurBackground.background = blurBackgroundDrawable
                mainViewModel.signal(MainSignal.BlurBackgroundUpdated(blurBackgroundDrawable))
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
