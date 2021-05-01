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
import kotlinx.coroutines.flow.*

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
@WithFragmentBindings
class PlaceMapListFragment : Fragment(R.layout.fragment_place_map_list), MapChangeListener {
    private val binding by viewBinding(FragmentPlaceMapListBinding::bind)

    @Inject internal lateinit var mainViewModelFactory: MainViewModel.Factory
    private val mainViewModel: MainViewModel by assistedActivityViewModel {
        mainViewModelFactory.create(it)
    }

    @Inject internal lateinit var mapTilesHttpHandler: HttpHandler
    private val mapController: Deferred<MapController> by lifecycleScope.lazyAsync {
        binding.map.init(mapTilesHttpHandler)
    }

    private var processingPlaces: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mapController.launch {
            setMapChangeListener(this@PlaceMapListFragment)
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

    private fun Deferred<MapController>.launch(block: suspend MapController.() -> Unit) {
        lifecycleScope.launch(Dispatchers.Main.immediate) { this@launch.await().block() }
    }

    private fun MapController.loadScene(scene: MapScene) {
        loadSceneFile(
            scene.url,
            listOf(SceneUpdate("global.sdk_api_key", BuildConfig.NEXTZEN_API_KEY))
        )
    }

    override fun onViewComplete() {
        if (processingPlaces) return
        processPlaces()
        processingPlaces = true
    }

    private fun processPlaces() {
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
                        captureFrame(false)
                    }
                binding.captureImageView.setImageBitmap(bitmap)
            }
            .launchIn(lifecycleScope)
    }

    override fun onRegionWillChange(animated: Boolean) = Unit
    override fun onRegionIsChanging() = Unit
    override fun onRegionDidChange(animated: Boolean) = Unit
}
