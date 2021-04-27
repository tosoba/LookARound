package com.lookaround.ui.place.list

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.lookaround.core.android.ext.*
import com.lookaround.core.android.map.MapScene
import com.lookaround.core.android.model.WithValue
import com.lookaround.core.delegate.lazyAsync
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.place.list.databinding.FragmentPlaceMapListBinding
import com.mapzen.tangram.*
import com.mapzen.tangram.networking.HttpHandler
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
@WithFragmentBindings
class PlaceMapListFragment :
    Fragment(R.layout.fragment_place_map_list), MapController.SceneLoadListener {
    private val binding: FragmentPlaceMapListBinding by viewBinding(
        FragmentPlaceMapListBinding::bind
    )

    @Inject internal lateinit var mainViewModelFactory: MainViewModel.Factory
    private val mainViewModel: MainViewModel by assistedActivityViewModel {
        mainViewModelFactory.create(it)
    }

    @Inject internal lateinit var mapTilesHttpHandler: HttpHandler
    private val mapController: Deferred<MapController> by lifecycleScope.lazyAsync {
        binding.map.init(mapTilesHttpHandler)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mapController.launch {
            setSceneLoadListener(this@PlaceMapListFragment)
            loadScene(MapScene.BUBBLE_WRAP)
        }
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

    override fun onSceneReady(sceneId: Int, sceneError: SceneError?) {
        val markers = mainViewModel.state.markers as WithValue
        markers
            .value
            .items
            .asFlow()
            .onEach { marker ->
                val location = marker.location
                val bitmap =
                    mapController.await().run {
                        updateCameraPosition(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition().apply {
                                    latitude = location.latitude
                                    longitude = location.longitude
                                }
                            )
                        )
                        captureFrame()
                    }
                Timber.tag("BIT").e(bitmap.byteCount.toString())
                binding.captureImageView.setImageBitmap(bitmap)
            }
            .launchIn(lifecycleScope)
    }

    private fun Deferred<MapController>.launch(block: suspend MapController.() -> Unit) {
        lifecycleScope.launch(Dispatchers.Main.immediate) { this@launch.await().block() }
    }

    private fun MapController.loadScene(scene: MapScene) {
        loadSceneFile(
            scene.url,
            listOf(SceneUpdate("global.sdk_api_key", BuildConfig.NEXTZEN_API_KEY))
        )
    }
}
