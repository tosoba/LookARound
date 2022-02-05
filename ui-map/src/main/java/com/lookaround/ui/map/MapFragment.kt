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
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.hoko.blur.HokoBlur
import com.hoko.blur.processor.BlurProcessor
import com.lookaround.core.android.ext.*
import com.lookaround.core.android.map.clustering.ClusterManager
import com.lookaround.core.android.map.clustering.DefaultClusterItem
import com.lookaround.core.android.map.scene.MapSceneViewModel
import com.lookaround.core.android.map.scene.model.MapScene
import com.lookaround.core.android.map.scene.model.MapSceneIntent
import com.lookaround.core.android.map.scene.model.MapSceneSignal
import com.lookaround.core.android.model.Marker
import com.lookaround.core.android.model.WithValue
import com.lookaround.core.android.model.WithoutValue
import com.lookaround.core.android.view.BlurAnimator
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
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
@WithFragmentBindings
class MapFragment :
    Fragment(R.layout.fragment_map), MapController.SceneLoadListener, MapChangeListener {
    private val binding: FragmentMapBinding by viewBinding(FragmentMapBinding::bind)

    private val mapSceneViewModel: MapSceneViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()

    @Inject internal lateinit var mapTilesHttpHandler: HttpHandler
    @Inject internal lateinit var glViewHolderFactory: GLViewHolderFactory
    private val mapController: Deferred<MapController> by
        lifecycleScope.lazyAsync { binding.map.init(mapTilesHttpHandler, glViewHolderFactory) }

    private val markerArgument: Marker? by nullableArgument(Arguments.MARKER.name)
    private var currentMarker: Marker? = null

    private var blurAnimator: BlurAnimator? = null
    private var clusterManager: ClusterManager<DefaultClusterItem>? = null

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
            savedInstanceState?.getParcelable(SavedStateKeys.CURRENT_MARKER.name) ?: markerArgument
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
            loadScene(MapScene.BUBBLE_WRAP)

            zoomOnDoubleTap()
            toggleSearchBarVisibilityOnTap()

            initCameraPosition(savedInstanceState)
            currentMarker?.location?.let { addMarkerFor(it) }
        }

        mainViewModel
            .states
            .filter { it.markers is WithoutValue }
            .map(MainState::locationState::get)
            .filterIsInstance<WithValue<Location>>()
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

        initMapImageBlurring(savedInstanceState)

        mainViewModel
            .states
            .map(MainState::markers::get)
            .distinctUntilChanged()
            .onEach { loadable ->
                mapSceneViewModel.awaitSceneLoaded()
                mapController.launch {
                    removeAllMarkers()
                    if (loadable !is WithValue) return@launch

                    val markers = loadable.value.items
                    if (markers.size > 1) {
                        calculateAndZoomToBoundsOf(markers.map(Marker::location::get))
                        addMarkerClusters(markers)
                    } else if (markers.size == 1) {
                        val marker = markers.first()
                        moveCameraPositionTo(
                            lat = marker.location.latitude,
                            lng = marker.location.longitude,
                            zoom = MARKER_FOCUSED_ZOOM
                        )
                        markers.forEach { addMarkerFor(it.location) }
                    }
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        mapSceneViewModel
            .signals
            .filterIsInstance<MapSceneSignal.RetryLoadScene>()
            .onEach { (scene) -> mapController.await().loadScene(scene) }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        binding.navigateFab.setOnClickListener { launchGoogleMapsForNavigation() }
        binding.streetViewFab.setOnClickListener { launchGoogleMapsForStreetView() }
    }

    override fun onDestroyView() {
        blurAnimator?.cancel()
        blurAnimator = null
        clusterManager?.cancel()
        clusterManager = null
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

    override fun onSaveInstanceState(outState: Bundle) {
        currentMarker?.let { outState.putParcelable(SavedStateKeys.CURRENT_MARKER.name, it) }
        mapController.launch { saveCameraPosition(outState) }
        blurAnimator?.saveInstanceState(outState)
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

    override fun onViewComplete() = Unit
    override fun onRegionWillChange(animated: Boolean) = Unit
    override fun onRegionIsChanging() = Unit
    override fun onRegionDidChange(animated: Boolean) {
        clusterManager?.onRegionDidChange(animated)
        if (!isRunningOnEmulator()) updateBlurBackground()
    }

    fun updateMarker(marker: Marker?) {
        currentMarker = marker
        if (marker != null) {
            showFABs()
            mapController.launch {
                val (_, location) = marker
                removeAllMarkers()
                moveCameraPositionTo(
                    lat = location.latitude,
                    lng = location.longitude,
                    zoom = MARKER_FOCUSED_ZOOM,
                    durationMs = 250
                )
                addMarkerFor(location)
            }
        } else {
            hideFABs()
        }
    }

    private fun MapController.initCameraPosition(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            restoreCameraPosition(savedInstanceState)
        } else {
            currentMarker?.let { (_, location) ->
                moveCameraPositionTo(
                    lat = location.latitude,
                    lng = location.longitude,
                    zoom = MARKER_FOCUSED_ZOOM
                )
            }
        }
    }

    private suspend fun MapController.loadScene(scene: MapScene) {
        hideFABs()
        binding.blurBackground.visibility = View.VISIBLE
        binding.shimmerLayout.showAndStart()

        mapSceneViewModel.intent(MapSceneIntent.LoadingScene(scene))
        loadSceneFile(
            scene.url,
            listOf(SceneUpdate("global.sdk_api_key", BuildConfig.NEXTZEN_API_KEY))
        )
    }

    private fun MapController.addMarkerClusters(markers: SortedSet<Marker>) {
        val clusterItems = markers.map { DefaultClusterItem(it.location.latLon) }
        clusterManager?.setItems(clusterItems)
            ?: run {
                clusterManager =
                    ClusterManager<DefaultClusterItem>(requireContext(), this).apply {
                        setItems(clusterItems)
                    }
            }
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

    private fun MapController.toggleSearchBarVisibilityOnTap() {
        touchInput.setTapResponder(
            object : TouchInput.TapResponder {
                override fun onSingleTapUp(x: Float, y: Float): Boolean = false
                override fun onSingleTapConfirmed(x: Float, y: Float): Boolean {
                    val targetVisibility = binding.visibilityToggleView.toggleVisibility()
                    viewLifecycleOwner.lifecycleScope.launchWhenResumed {
                        mainViewModel.signal(MainSignal.ToggleSearchBarVisibility(targetVisibility))
                    }
                    return true
                }
            }
        )
    }

    private fun launchGoogleMapsForNavigation() {
        launchGoogleMapForCurrentMarker(
            failureMsgRes = R.string.unable_to_launch_google_maps_for_navigation
        ) { "google.navigation:q=${location.latitude},${location.longitude}" }
    }

    private fun launchGoogleMapsForStreetView() {
        launchGoogleMapForCurrentMarker(failureMsgRes = R.string.unable_to_launch_street_view) {
            "google.streetview:cbll=${location.latitude},${location.longitude}"
        }
    }

    private fun launchGoogleMapForCurrentMarker(
        @StringRes failureMsgRes: Int,
        uriString: Marker.() -> String
    ) {
        currentMarker?.let { marker ->
            val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(marker.uriString()))
            mapIntent.setPackage("com.google.android.apps.maps")
            try {
                startActivity(mapIntent)
            } catch (ex: ActivityNotFoundException) {
                Toast.makeText(requireContext(), getString(failureMsgRes), Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun initMapImageBlurring(savedInstanceState: Bundle?) {
        if (isRunningOnEmulator()) return

        val blurredMapImageDrawable = binding.blurredMapImageView.drawable
        var skipFirstBottomSheetSignal = false
        if (savedInstanceState != null &&
                blurredMapImageDrawable is BitmapDrawable &&
                blurredMapImageDrawable.bitmap != null
        ) {
            blurAnimator =
                BlurAnimator.fromSavedInstanceState(
                        requireContext(),
                        blurredMapImageDrawable.bitmap,
                        savedInstanceState
                    )
                    ?.apply { animate() }
            skipFirstBottomSheetSignal = true
        } else if (mainViewModel.state.lastLiveBottomSheetState ==
                BottomSheetBehavior.STATE_EXPANDED
        ) {
            viewLifecycleOwner.lifecycleScope.launch { showAndBlurMapImage() }
            skipFirstBottomSheetSignal = true
        }

        mainViewModel
            .signals
            .filterIsInstance<MainSignal.BottomSheetStateChanged>()
            .map(MainSignal.BottomSheetStateChanged::state::get)
            .run { if (skipFirstBottomSheetSignal) drop(1) else this }
            .onEach { sheetState ->
                if (sheetState == BottomSheetBehavior.STATE_EXPANDED) {
                    showAndBlurMapImage()
                } else if (sheetState == BottomSheetBehavior.STATE_HIDDEN) {
                    reverseBlurAndHideMapImage()
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private suspend fun showAndBlurMapImage() {
        val isReversing =
            blurAnimator?.let { currentAnimator ->
                if (currentAnimator.inProgress) {
                    currentAnimator.cancel()
                    blurAnimator = currentAnimator.reversed(requireContext()).apply { animate() }
                    true
                } else {
                    false
                }
            }
        if (isReversing == true) return

        mapSceneViewModel.awaitSceneLoaded()
        val bitmap = mapController.await().captureFrame(true)
        binding.blurredMapImageView.setImageBitmap(bitmap)
        blurAnimator =
            BlurAnimator(
                    requireContext(),
                    initialBitmap = bitmap,
                    initialRadius = 0,
                    targetRadius = 10
                )
                .apply { animate() }
    }

    private fun reverseBlurAndHideMapImage() {
        blurAnimator?.let { currentAnimator ->
            currentAnimator.cancel()
            blurAnimator = currentAnimator.reversed(requireContext()).apply { animate() }
        }
    }

    private fun BlurAnimator.animate() {
        animateIn(viewLifecycleOwner.lifecycleScope)
        if (animationType == BlurAnimator.AnimationType.BLUR) {
            binding.blurredMapImageView.visibility = View.VISIBLE
        }
        animationStates
            .onEach { (blurredBitmap, inProgress) ->
                binding.blurredMapImageView.setImageBitmap(blurredBitmap)
                if (animationType == BlurAnimator.AnimationType.REVERSE_BLUR && !inProgress) {
                    binding.blurredMapImageView.visibility = View.GONE
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun updateBlurBackground() {
        viewLifecycleOwner.lifecycleScope.launch {
            mapSceneViewModel.awaitSceneLoaded()
            val bitmap = mapController.await().captureFrame(true)
            val blurredBackground = withContext(Dispatchers.Default) { blurProcessor.blur(bitmap) }
            binding.blurBackground.background = BitmapDrawable(resources, blurredBackground)
            mainViewModel.state.bitmapCache.put(this@MapFragment.javaClass.name, blurredBackground)
        }
    }

    private fun Deferred<MapController>.launch(block: suspend MapController.() -> Unit) {
        if (view == null) return
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main.immediate) {
            this@launch.await().block()
        }
    }

    companion object {
        private const val MARKER_FOCUSED_ZOOM = 15f

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
