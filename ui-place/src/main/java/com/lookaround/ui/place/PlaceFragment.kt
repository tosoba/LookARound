package com.lookaround.ui.place

import android.os.Bundle
import android.view.View
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
import com.lookaround.core.delegate.lazyAsync
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.place.databinding.FragmentPlaceBinding
import com.mapzen.tangram.MapChangeListener
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
import timber.log.Timber

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
@WithFragmentBindings
class PlaceFragment :
    Fragment(R.layout.fragment_place), MapController.SceneLoadListener {
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
        mapController.launch {
            setSceneLoadListener(this@PlaceFragment)
            loadScene(if (requireContext().darkMode) MapScene.DARK else MapScene.LIGHT)
            initCameraPosition()
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

    override fun onSceneReady(sceneId: Int, sceneError: SceneError?) {
        if (view == null) return

        if (sceneError == null) {
            viewLifecycleOwner.lifecycleScope.launch {
                mapSceneViewModel.intent(MapSceneIntent.SceneLoaded)
            }
            if (mapReady.isCompleted) return else mapReady.complete(Unit)

        } else {
            Timber.e("Failed to load scene: $sceneId. Scene error: $sceneError")
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
