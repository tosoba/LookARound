package com.lookaround.ui.map

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.lookaround.core.android.ext.*
import com.lookaround.core.delegate.lazyAsync
import com.lookaround.ui.map.databinding.FragmentMapBinding
import com.mapzen.tangram.*
import com.mapzen.tangram.networking.HttpHandler
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
class MapFragment : Fragment(R.layout.fragment_map), MapController.SceneLoadListener {
    private val binding: FragmentMapBinding by viewBinding(FragmentMapBinding::bind)

    @Inject
    internal lateinit var viewModelFactory: MapViewModel.Factory
    private val viewModel: MapViewModel by assistedViewModel { viewModelFactory.create(it) }

    @Inject
    internal lateinit var mapTilesHttpHandler: HttpHandler
    private val mapController: Deferred<MapController> by lifecycleScope.lazyAsync {
        binding.map.init(mapTilesHttpHandler)
    }

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
        mapController.launch { saveCameraPosition(outState) }
    }

    override fun onSceneReady(sceneId: Int, sceneError: SceneError?) {
        if (sceneError == null) {
            lifecycleScope.launch { viewModel.intent(MapIntent.SceneLoaded) }

            with(binding.shimmerLayout) {
                stopShimmer()
                visibility = View.GONE
            }

            binding.blurBackground.animate()
                .setDuration(500L)
                .alpha(0f)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        binding.blurBackground.visibility = View.GONE
                    }
                })
        } else {
            Timber.e("Failed to load scene: $sceneId. Scene error: $sceneError")
        }
    }

    private fun Deferred<MapController>.launch(block: suspend MapController.() -> Unit) {
        lifecycleScope.launch(Dispatchers.Main.immediate) { this@launch.await().block() }
    }

    private suspend fun MapController.loadScene(scene: MapScene) {
        with(binding.blurBackground) {
            if (visibility != View.VISIBLE) {
                alpha = 0f
                visibility = View.VISIBLE
                animate().setDuration(500L).alpha(1f)
            }
        }

        with(binding.shimmerLayout) {
            visibility = View.VISIBLE
            startShimmer()
        }

        viewModel.intent(MapIntent.LoadingScene(scene))
        loadSceneFile(
            scene.url,
            listOf(SceneUpdate("global.sdk_api_key", BuildConfig.NEXTZEN_API_KEY))
        )
    }
}
