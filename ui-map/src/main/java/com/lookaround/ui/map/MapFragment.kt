package com.lookaround.ui.map

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Rect
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.lookaround.core.android.ext.*
import com.lookaround.core.android.map.scene.MapSceneViewModel
import com.lookaround.core.android.map.scene.model.MapScene
import com.lookaround.core.android.map.scene.model.MapSceneIntent
import com.lookaround.core.android.map.scene.model.MapSceneSignal
import com.lookaround.core.android.model.Marker
import com.lookaround.core.android.model.WithValue
import com.lookaround.core.android.model.WithoutValue
import com.lookaround.core.delegate.lazyAsync
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.model.MainState
import com.lookaround.ui.map.databinding.FragmentMapBinding
import com.mapzen.tangram.*
import com.mapzen.tangram.networking.HttpHandler
import com.mapzen.tangram.viewholder.GLViewHolderFactory
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
@WithFragmentBindings
class MapFragment : Fragment(R.layout.fragment_map), MapController.SceneLoadListener {
    private val binding: FragmentMapBinding by viewBinding(FragmentMapBinding::bind)

    private val mapSceneViewModel: MapSceneViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()

    @Inject internal lateinit var mapTilesHttpHandler: HttpHandler
    @Inject internal lateinit var glViewHolderFactory: GLViewHolderFactory
    private val mapController: Deferred<MapController> by
        lifecycleScope.lazyAsync { binding.map.init(mapTilesHttpHandler, glViewHolderFactory) }

    private val markerArgument: Marker? by nullableArgument(Arguments.MARKER.name)
    private var currentMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentMarker =
            savedInstanceState?.getParcelable(SavedStateKeys.CURRENT_MARKER.name) ?: markerArgument
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mapController.launch {
            setSceneLoadListener(this@MapFragment)
            loadScene(MapScene.BUBBLE_WRAP)
            initCameraPosition(savedInstanceState)
            currentMarker?.location?.let { addMarker(it) }
            zoomOnDoubleTap()
        }

        mainViewModel
            .states
            .filter { it.markers is WithoutValue }
            .map(MainState::locationState::get)
            .filterIsInstance<WithValue<Location>>()
            .onEach { location ->
                mapController.launch {
                    moveCameraPositionTo(
                        location.value.latitude,
                        location.value.longitude,
                        MARKER_FOCUSED_ZOOM
                    )
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        mainViewModel
            .states
            .map(MainState::markers::get)
            .distinctUntilChanged()
            .onEach { markers ->
                mapSceneViewModel.awaitSceneLoaded()
                mapController.launch {
                    removeAllMarkers()
                    if (markers !is WithValue) return@launch
                    markers.value.items.forEach { marker -> addMarker(marker.location) }
                    if (markers.value.items.size > 1) {
                        calculateAndZoomToBoundsOf(markers.value.items.map(Marker::location::get))
                    } else if (markers.value.items.size == 1) {
                        val marker = markers.value.items.first()
                        moveCameraPositionTo(
                            marker.location.latitude,
                            marker.location.longitude,
                            MARKER_FOCUSED_ZOOM
                        )
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
    }

    override fun onSceneReady(sceneId: Int, sceneError: SceneError?) {
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

    fun updateMarker(marker: Marker) {
        currentMarker = marker
        mapController.launch {
            val (_, location) = marker
            removeAllMarkers()
            moveCameraPositionTo(location.latitude, location.longitude, MARKER_FOCUSED_ZOOM, 250)
            addMarker(location)
        }
    }

    private fun MapController.initCameraPosition(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            restoreCameraPosition(savedInstanceState)
        } else {
            currentMarker?.let { (_, location) ->
                moveCameraPositionTo(location.latitude, location.longitude, MARKER_FOCUSED_ZOOM)
            }
        }
    }

    private suspend fun MapController.loadScene(scene: MapScene) {
        binding.blurBackground.visibility = View.VISIBLE
        binding.shimmerLayout.showAndStart()

        mapSceneViewModel.intent(MapSceneIntent.LoadingScene(scene))
        loadSceneFile(
            scene.url,
            listOf(SceneUpdate("global.sdk_api_key", BuildConfig.NEXTZEN_API_KEY))
        )
    }

    private fun MapController.addMarker(location: Location) {
        addMarker().apply {
            setPoint(LngLat(location.longitude, location.latitude))
            isVisible = true
            setStylingFromString(
                "{ style: 'points', size: [27px, 27px], order: 2000, collide: false, color: blue}"
            )
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
                Rect(1, 1, 1, 1)
            ),
        )
    }

    private fun launchGoogleMapsForNavigation() {
        currentMarker?.let { marker ->
            val mapIntent =
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                        "google.navigation:q=${marker.location.latitude},${marker.location.longitude}"
                    )
                )
            mapIntent.setPackage("com.google.android.apps.maps")
            try {
                startActivity(mapIntent)
            } catch (ex: ActivityNotFoundException) {
                Toast.makeText(
                        requireContext(),
                        getString(R.string.unable_to_launch_google_maps_for_navigation),
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }
        }
    }

    private fun launchGoogleMapsForStreetView() {
        currentMarker?.let { marker ->
            val mapIntent =
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                        "google.streetview:cbll=${marker.location.latitude},${marker.location.longitude}"
                    )
                )
            mapIntent.setPackage("com.google.android.apps.maps")
            try {
                startActivity(mapIntent)
            } catch (ex: ActivityNotFoundException) {
                Toast.makeText(requireContext(), "Unable to launch StreetView", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun Deferred<MapController>.launch(block: suspend MapController.() -> Unit) {
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
