package com.lookaround.ui.place

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
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
import com.lookaround.core.android.view.theme.colorPalette
import com.lookaround.core.delegate.lazyAsync
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.locationReadyUpdates
import com.lookaround.ui.place.databinding.FragmentPlaceBinding
import com.mapzen.tangram.MapController
import com.mapzen.tangram.SceneError
import com.mapzen.tangram.SceneUpdate
import com.mapzen.tangram.networking.HttpHandler
import com.mapzen.tangram.viewholder.GLViewHolderFactory
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
@WithFragmentBindings
class PlaceFragment : Fragment(R.layout.fragment_place), MapController.SceneLoadListener {
    private val binding: FragmentPlaceBinding by viewBinding(FragmentPlaceBinding::bind)

    private val markerArgument: Marker by argument(Arguments.MARKER.name)

    private val mapSceneViewModel: MapSceneViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()

    @Inject internal lateinit var mapTilesHttpHandler: HttpHandler
    @Inject internal lateinit var glViewHolderFactory: GLViewHolderFactory
    private val mapController: Deferred<MapController> by
        lifecycleScope.lazyAsync {
            binding.placeMapView.init(mapTilesHttpHandler, glViewHolderFactory)
        }
    private val mapReady = CompletableDeferred<Unit>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.placeBackButton.setOnClickListener { activity?.onBackPressed() }
        binding.navigateFab.setOnClickListener { launchGoogleMapsForNavigation() }
        binding.streetViewFab.setOnClickListener { launchGoogleMapsForStreetView() }
        binding.placeGoogleMapsFab.setOnClickListener { launchGoogleMaps() }

        initPlaceInfo()

        mapController.launch {
            setSceneLoadListener(this@PlaceFragment)
            loadScene(if (requireContext().darkMode) MapScene.DARK else MapScene.LIGHT)
            initCameraPosition()
            touchInput.setAllGesturesDisabled()
            addMarkerFor(markerArgument.location)
        }

        mapSceneViewModel
            .onEachSignal(MapSceneSignal.RetryLoadScene::scene) { scene ->
                mapController.await().loadScene(scene)
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    override fun onDestroyView() {
        binding.placeMapView.onDestroy()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        binding.placeMapView.onResume()
    }

    override fun onPause() {
        binding.placeMapView.onPause()
        super.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.placeMapView.onLowMemory()
    }

    override fun onSceneReady(sceneId: Int, sceneError: SceneError?) {
        if (view == null) return

        if (sceneError == null) {
            viewLifecycleOwner.lifecycleScope.launch {
                mapSceneViewModel.intent(MapSceneIntent.SceneLoaded)
            }
            if (!mapReady.isCompleted) mapReady.complete(Unit)
        } else {
            Timber.e("Failed to load scene: $sceneId. Scene error: $sceneError")
        }
    }

    private suspend fun MapController.loadScene(scene: MapScene) {
        mapSceneViewModel.intent(MapSceneIntent.LoadingScene(scene))
        loadSceneFile(
            scene.path,
            listOf(SceneUpdate("global.sdk_api_key", BuildConfig.NEXTZEN_API_KEY))
        )
    }

    private suspend fun MapController.initCameraPosition() {
        mapReady.await()

        moveCameraPositionTo(
            lat = markerArgument.location.latitude,
            lng = markerArgument.location.longitude,
            zoom = MARKER_FOCUSED_ZOOM
        )
    }

    private fun initPlaceInfo() {
        binding.placeInfoCardView.setCardBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                if (requireContext().darkMode) R.color.cardview_dark_background
                else R.color.cardview_light_background
            )
        )

        markerArgument.tags["opening_hours"]?.let(binding.placeOpeningHoursTextView::setText)
            ?: run { binding.placeOpeningHoursTextView.visibility = View.GONE }
        binding.placeOpeningHoursTextView.setTextColor(
            requireContext().colorPalette.textHelp.toArgb()
        )

        binding.placeNameTextView.text = markerArgument.name
        binding.placeNameTextView.setTextColor(requireContext().colorPalette.textPrimary.toArgb())

        markerArgument.address?.let(binding.placeAddressTextView::setText)
            ?: run { binding.placeAddressTextView.visibility = View.GONE }
        binding.placeAddressTextView.setTextColor(
            requireContext().colorPalette.textSecondary.toArgb()
        )

        binding.placeDistanceTextView.setTextColor(
            requireContext().colorPalette.textSecondary.toArgb()
        )
        mainViewModel.locationReadyUpdates
            .map(markerArgument.location::preciseFormattedDistanceTo)
            .onEach(binding.placeDistanceTextView::setText)
            .launchIn(viewLifecycleOwner.lifecycleScope)

        markerArgument.tags["description"]?.let(binding.placeDescriptionTextView::setText)
            ?: run { binding.placeDescriptionTextView.visibility = View.GONE }
    }

    private fun launchGoogleMaps() {
        launchGoogleMapForCurrentMarker(failureMsgRes = R.string.unable_to_launch_google_maps) {
            marker ->
            val query = Uri.encode(if (marker.address != null) marker.address else marker.name)
            "geo:${marker.location.latitude},${marker.location.longitude}?q=$query&z=21"
        }
    }

    private fun launchGoogleMapsForNavigation() {
        launchGoogleMapForCurrentMarker(
            failureMsgRes = R.string.unable_to_launch_google_maps_for_navigation
        ) { (_, location) -> "google.navigation:q=${location.latitude},${location.longitude}" }
    }

    private fun launchGoogleMapsForStreetView() {
        launchGoogleMapForCurrentMarker(failureMsgRes = R.string.unable_to_launch_street_view) {
            (_, location) ->
            "google.streetview:cbll=${location.latitude},${location.longitude}"
        }
    }

    private fun launchGoogleMapForCurrentMarker(
        @StringRes failureMsgRes: Int,
        uriStringFor: (Marker) -> String
    ) {
        val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStringFor(markerArgument)))
        mapIntent.setPackage("com.google.android.apps.maps")
        try {
            startActivity(mapIntent)
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(requireContext(), getString(failureMsgRes), Toast.LENGTH_SHORT).show()
        }
    }

    private fun Deferred<MapController>.launch(block: suspend MapController.() -> Unit) {
        if (view == null) return
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main.immediate) {
            this@launch.await().block()
        }
    }

    companion object {
        private const val MARKER_FOCUSED_ZOOM = 17f

        enum class Arguments {
            MARKER
        }

        fun new(marker: Marker): PlaceFragment =
            PlaceFragment().apply { arguments = bundleOf(Arguments.MARKER.name to marker) }
    }
}
