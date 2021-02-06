package com.lookaround.ui.map

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import by.kirich1409.viewbindingdelegate.viewBinding
import com.lookaround.core.android.ext.*
import com.lookaround.core.delegate.lazyAsync
import com.lookaround.ui.map.databinding.FragmentMapBinding
import com.mapzen.tangram.*
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
@WithFragmentBindings
class MapFragment :
    Fragment(R.layout.fragment_map),
    CoroutineScope by CoroutineScope(Dispatchers.Main),
    MapController.SceneLoadListener {

    @Inject
    internal lateinit var viewModelFactory: MapViewModel.Factory
    private val viewModel: MapViewModel by assistedViewModel { savedState ->
        viewModelFactory.create(MapState(), savedState)
    }

    private val binding: FragmentMapBinding by viewBinding(FragmentMapBinding::bind)
    private val mapController: Deferred<MapController> by lazyAsync { binding.map.init() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mapController.launch {
            loadScene(MapScene.BUBBLE_WRAP)
            setSceneLoadListener(this@MapFragment)
            restoreCameraPosition(savedInstanceState)
            zoomOnDoubleTap()
        }

        viewModel.signals
            .filterIsInstance<MapSignal.RetryLoadScene>()
            .onEach { mapController.await().loadScene(it.scene) }
            .launchIn(this)
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
        mapController.launch { saveCameraPosition(outState) }
    }

    override fun onSceneReady(sceneId: Int, sceneError: SceneError?) {
        if (sceneError == null) launch {
            viewModel.intent(MapIntent.SceneLoaded)
        } else {
            Timber.e("Failed to load scene: $sceneId. Scene error: $sceneError")
        }
    }

    private fun Deferred<MapController>.launch(block: suspend MapController.() -> Unit) {
        this@MapFragment.launch(Dispatchers.Main.immediate) { this@launch.await().block() }
    }

    private suspend fun MapController.loadScene(scene: MapScene) {
        viewModel.intent(MapIntent.LoadingScene(scene))
        loadSceneFile(
            scene.url,
            listOf(SceneUpdate("global.sdk_api_key", BuildConfig.NEXTZEN_API_KEY))
        )
    }
}
