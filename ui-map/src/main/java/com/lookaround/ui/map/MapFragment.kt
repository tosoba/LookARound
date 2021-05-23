package com.lookaround.ui.map

import android.location.Location
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.lookaround.core.android.ext.*
import com.lookaround.core.android.map.scene.MapSceneViewModel
import com.lookaround.core.android.map.scene.model.MapScene
import com.lookaround.core.android.map.scene.model.MapSceneIntent
import com.lookaround.core.android.map.scene.model.MapSceneSignal
import com.lookaround.core.android.model.Marker
import com.lookaround.core.delegate.lazyAsync
import com.lookaround.ui.map.databinding.FragmentMapBinding
import com.mapzen.tangram.LngLat
import com.mapzen.tangram.MapController
import com.mapzen.tangram.SceneError
import com.mapzen.tangram.SceneUpdate
import com.mapzen.tangram.networking.HttpHandler
import com.mapzen.tangram.viewholder.GLViewHolderFactory
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
@WithFragmentBindings
class MapFragment : Fragment(R.layout.fragment_map), MapController.SceneLoadListener {
    private val binding: FragmentMapBinding by viewBinding(FragmentMapBinding::bind)

    @Inject internal lateinit var viewModelFactory: MapSceneViewModel.Factory
    private val viewModel: MapSceneViewModel by assistedViewModel { viewModelFactory.create(it) }

    @Inject internal lateinit var mapTilesHttpHandler: HttpHandler
    @Inject internal lateinit var glViewHolderFactory: GLViewHolderFactory
    private val mapController: Deferred<MapController> by lifecycleScope.lazyAsync {
        binding.map.init(mapTilesHttpHandler, glViewHolderFactory)
    }

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

        viewModel
            .signals
            .filterIsInstance<MapSceneSignal.RetryLoadScene>()
            .onEach { mapController.await().loadScene(it.scene) }
            .launchIn(lifecycleScope)
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
            lifecycleScope.launch { viewModel.intent(MapSceneIntent.SceneLoaded) }

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
            moveCameraPositionTo(location.latitude, location.longitude, 15f, 250)
            addMarker(location)
        }
    }

    private fun MapController.initCameraPosition(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            restoreCameraPosition(savedInstanceState)
        } else {
            currentMarker?.let { (_, location) ->
                moveCameraPositionTo(location.latitude, location.longitude, 15f)
            }
        }
    }

    private suspend fun MapController.loadScene(scene: MapScene) {
        binding.blurBackground.visibility = View.VISIBLE
        binding.shimmerLayout.showAndStart()

        viewModel.intent(MapSceneIntent.LoadingScene(scene))
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

    private fun Deferred<MapController>.launch(block: suspend MapController.() -> Unit) {
        lifecycleScope.launch(Dispatchers.Main.immediate) { this@launch.await().block() }
    }

    companion object {
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
