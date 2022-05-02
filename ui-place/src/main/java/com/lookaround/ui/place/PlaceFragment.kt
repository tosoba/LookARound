package com.lookaround.ui.place

import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.marginTop
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
import com.lookaround.ui.main.model.MainState
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
        binding.navigateFab.setOnClickListener { startGoogleMapsForNavigation() }
        binding.streetViewFab.setOnClickListener { startGoogleMapsForStreetView() }
        binding.placeGoogleMapsFab.setOnClickListener { startGoogleMaps() }
        binding.placeNestedScrollView.apply {
            mainViewModel.state.bitmapCache[MainState.BlurredBackgroundType.MAP]?.let { bitmap ->
                background = BitmapDrawable(resources, bitmap)
            }
                ?: run { setBackgroundColor(requireContext().colorPalette.uiBackground.toArgb()) }
        }

        binding.initPlaceInfo()

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

    private fun FragmentPlaceBinding.initPlaceInfo() {
        placeInfoCardView.setCardBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                if (requireContext().darkMode) R.color.cardview_dark_background
                else R.color.cardview_light_background
            )
        )
        placeInfoCardView.runOnPreDrawOnce {
            val height = placeInfoCardView.height
            placeNestedScrollView.setPadding(
                0,
                height - placeInfoCardView.marginTop + requireContext().dpToPx(20f).toInt(),
                0,
                0
            )
        }

        markerArgument.tags["opening_hours"]?.let(placeOpeningHoursTextView::setText)
            ?: run { placeOpeningHoursTextView.visibility = View.GONE }
        placeOpeningHoursTextView.setTextColor(requireContext().colorPalette.textHelp.toArgb())

        placeNameTextView.text = markerArgument.name
        placeNameTextView.setTextColor(requireContext().colorPalette.textPrimary.toArgb())

        markerArgument.address?.let(placeAddressTextView::setText)
            ?: run { placeAddressTextView.visibility = View.GONE }
        placeAddressTextView.setTextColor(requireContext().colorPalette.textSecondary.toArgb())

        placeDistanceTextView.setTextColor(requireContext().colorPalette.textSecondary.toArgb())
        mainViewModel.locationReadyUpdates
            .map(markerArgument.location::preciseFormattedDistanceTo)
            .onEach(placeDistanceTextView::setText)
            .launchIn(viewLifecycleOwner.lifecycleScope)

        markerArgument.tags["description"]?.let {
            placeDescriptionHeaderTextView.setTextColor(
                requireContext().colorPalette.textPrimary.toArgb()
            )
            placeDescriptionTextView.text = it
            placeDescriptionTextView.setTextColor(
                requireContext().colorPalette.textSecondary.toArgb()
            )
        }
            ?: run {
                placeDescriptionHeaderTextView.visibility = View.GONE
                placeDescriptionTextView.visibility = View.GONE
            }

        if (markerArgument.hasContacts) {
            placeContactsHeaderTextView.setTextColor(
                requireContext().colorPalette.textPrimary.toArgb()
            )

            markerArgument.contactPhone?.let {
                binding.placeContactPhone.setOnClickListener { _ ->
                    startActivityOrShowFailureMsg(
                        Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${it.filterNot(Char::isWhitespace)}")
                        },
                        R.string.unable_to_launch_app_for_contact
                    )
                }
            }
                ?: run { binding.placeContactPhone.visibility = View.GONE }

            markerArgument.contactWebsite?.let {
                binding.placeContactWebsite.setOnClickListener { _ ->
                    startActivityOrShowFailureMsg(
                        Intent(Intent.ACTION_VIEW, it.uri),
                        R.string.unable_to_launch_app_for_contact
                    )
                }
            }
                ?: run { binding.placeContactWebsite.visibility = View.GONE }

            markerArgument.contactEmail?.let {
                binding.placeContactEmail.setOnClickListener { _ ->
                    try {
                        val intent =
                            Intent(
                                "android.intent.action.SENDTO",
                                Uri.fromParts("mailto", it, null)
                            )
                        startActivity(
                            Intent.createChooser(intent, getString(R.string.select_email_client))
                        )
                    } catch (ex: Exception) {
                        Toast.makeText(
                                requireContext(),
                                getString(R.string.unable_to_launch_app_for_contact),
                                Toast.LENGTH_SHORT
                            )
                            .show()
                    }
                }
            }
                ?: run { binding.placeContactEmail.visibility = View.GONE }
        } else {
            binding.placeContactsHeaderTextView.visibility = View.GONE
            binding.placeContactsLayout.visibility = View.GONE
        }

        markerArgument.tags["contact:facebook"]
            ?.takeIf { it.startsWith("https://") }
            ?.let {
                binding.placeContactFacebook.setOnClickListener { _ ->
                    startActivityOrShowFailureMsg(
                        Intent(Intent.ACTION_VIEW, it.uri),
                        R.string.unable_to_launch_app_for_contact
                    )
                }
            }
            ?: run { binding.placeContactFacebook.visibility = View.GONE }

        markerArgument.tags["contact:instagram"]
            ?.takeIf { it.startsWith("https://") }
            ?.let {
                binding.placeContactInstagram.setOnClickListener { _ ->
                    startActivityOrShowFailureMsg(
                        Intent(Intent.ACTION_VIEW, it.uri),
                        R.string.unable_to_launch_app_for_contact
                    )
                }
            }
            ?: run { binding.placeContactInstagram.visibility = View.GONE }

        if (!markerArgument.hasContacts && !markerArgument.tags.containsKey("description")) {
            placeNoInformationTextView.visibility = View.VISIBLE
            placeNoInformationTextView.setTextColor(
                requireContext().colorPalette.textSecondary.toArgb()
            )
        }
    }

    private fun startGoogleMaps() {
        startGoogleMapForMarker(failureMsgRes = R.string.unable_to_launch_google_maps) { marker ->
            val query = Uri.encode(if (marker.address != null) marker.address else marker.name)
            "geo:${marker.location.latitude},${marker.location.longitude}?q=$query&z=21"
        }
    }

    private fun startGoogleMapsForNavigation() {
        startGoogleMapForMarker(
            failureMsgRes = R.string.unable_to_launch_google_maps_for_navigation
        ) { (_, location) -> "google.navigation:q=${location.latitude},${location.longitude}" }
    }

    private fun startGoogleMapsForStreetView() {
        startGoogleMapForMarker(failureMsgRes = R.string.unable_to_launch_street_view) {
            (_, location) ->
            "google.streetview:cbll=${location.latitude},${location.longitude}"
        }
    }

    private fun startGoogleMapForMarker(
        @StringRes failureMsgRes: Int,
        uriStringFor: (Marker) -> String
    ) {
        val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStringFor(markerArgument)))
        mapIntent.setPackage("com.google.android.apps.maps")
        startActivityOrShowFailureMsg(mapIntent, failureMsgRes)
    }

    private fun startActivityOrShowFailureMsg(mapIntent: Intent, @StringRes failureMsgRes: Int) {
        try {
            startActivity(mapIntent)
        } catch (ex: Exception) {
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
