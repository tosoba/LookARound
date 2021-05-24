package com.lookaround.ui.place.map.list

import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.lookaround.core.android.ext.*
import com.lookaround.core.android.map.MapCaptureCache
import com.lookaround.core.android.map.scene.MapSceneViewModel
import com.lookaround.core.android.map.scene.model.MapScene
import com.lookaround.core.android.map.scene.model.MapSceneIntent
import com.lookaround.core.android.map.scene.model.MapSceneSignal
import com.lookaround.core.android.view.composable.BottomSheetHeaderText
import com.lookaround.core.android.view.theme.LookARoundTheme
import com.lookaround.core.delegate.lazyAsync
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.locationReadyUpdates
import com.lookaround.ui.main.markerUpdates
import com.lookaround.ui.place.map.list.databinding.FragmentPlaceMapListBinding
import com.mapzen.tangram.*
import com.mapzen.tangram.networking.HttpHandler
import com.mapzen.tangram.viewholder.GLViewHolderFactory
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import timber.log.Timber
import uk.co.senab.bitmapcache.CacheableBitmapDrawable

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
@WithFragmentBindings
class PlaceMapListFragment :
    Fragment(R.layout.fragment_place_map_list), MapController.SceneLoadListener, MapChangeListener {
    private val binding by viewBinding(FragmentPlaceMapListBinding::bind)

    @Inject internal lateinit var mainViewModelFactory: MainViewModel.Factory
    private val mainViewModel: MainViewModel by assistedActivityViewModel {
        mainViewModelFactory.create(it)
    }

    @Inject internal lateinit var viewModelFactory: MapSceneViewModel.Factory
    private val viewModel: MapSceneViewModel by assistedViewModel { viewModelFactory.create(it) }

    @Inject internal lateinit var mapTilesHttpHandler: HttpHandler
    @Inject internal lateinit var glViewHolderFactory: GLViewHolderFactory
    private val mapController: Deferred<MapController> by lifecycleScope.lazyAsync {
        binding.map.init(mapTilesHttpHandler, glViewHolderFactory)
    }

    @Inject internal lateinit var mapCaptureCache: MapCaptureCache
    private val mapCaptureRequestChannel =
        BroadcastChannel<PlaceMapCaptureRequest>(Channel.BUFFERED)
    private var processingPlaces: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.placeMapListHeaderView.setContent {
            LookARoundTheme { BottomSheetHeaderText("Place maps") }
        }

        mapController.launch {
            setSceneLoadListener(this@PlaceMapListFragment)
            setMapChangeListener(this@PlaceMapListFragment)
            loadScene(MapScene.BUBBLE_WRAP)
        }

        viewModel
            .signals
            .filterIsInstance<MapSceneSignal.RetryLoadScene>()
            .onEach { mapController.await().loadScene(it.scene) }
            .launchIn(lifecycleScope)
    }

    override fun onDestroyView() {
        mapCaptureRequestChannel.close()
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
        if (sceneError == null) {
            lifecycleScope.launch { viewModel.intent(MapSceneIntent.SceneLoaded) }
        } else {
            Timber.e("Failed to load scene: $sceneId. Scene error: $sceneError")
        }
    }

    override fun onViewComplete() {
        if (processingPlaces) return
        if (!viewModel.state.sceneLoaded) {
            Timber.d("Scene is not loaded")
            return
        }
        processPlaces()
        processingPlaces = true
    }

    private fun processPlaces() {
        mapCaptureRequestChannel
            .asFlow()
            .onEach(::processMapCaptureRequest)
            .launchIn(lifecycleScope)
        initPlaceMapList()
    }

    private suspend fun processMapCaptureRequest(request: PlaceMapCaptureRequest) {
        val (location, bitmapCallback, cacheableBitmapDrawableCallback) = request
        val cached = getCachedBitmap(location)
        if (cached != null) {
            cacheableBitmapDrawableCallback(cached)
            return
        }
        val bitmap = captureBitmap(location)
        bitmapCallback(bitmap)
        cacheBitmap(location, bitmap)
    }

    private suspend fun getCachedBitmap(location: Location): CacheableBitmapDrawable? =
        if (mapCaptureCache.isEnabled) withContext(Dispatchers.IO) { mapCaptureCache[location] }
        else null

    private suspend fun cacheBitmap(location: Location, bitmap: Bitmap) {
        if (!mapCaptureCache.isEnabled) return
        withContext(Dispatchers.IO) { mapCaptureCache[location] = bitmap }
    }

    private suspend fun captureBitmap(location: Location): Bitmap =
        mapController.await().run {
            updateCameraPosition(
                CameraUpdateFactory.newLngLatZoom(
                    LngLat(location.longitude, location.latitude),
                    15f
                )
            )
            captureFrame(true)
        }

    private fun initPlaceMapList() {
        val placeMapListAdapter =
            PlaceMapListAdapter(mapCaptureRequestChannel, mainViewModel.locationReadyUpdates) {
                val controller =
                    activity as? PlaceMapItemActionController ?: return@PlaceMapListAdapter
                controller.onPlaceMapItemClick(marker = it)
            }
        binding.placeMapRecyclerView.adapter = placeMapListAdapter
        mainViewModel.markerUpdates.onEach(placeMapListAdapter::update).launchIn(lifecycleScope)
    }

    private suspend fun MapController.loadScene(scene: MapScene) {
        viewModel.intent(MapSceneIntent.LoadingScene(scene))
        loadSceneFile(
            scene.url,
            listOf(SceneUpdate("global.sdk_api_key", BuildConfig.NEXTZEN_API_KEY))
        )
    }

    private fun Deferred<MapController>.launch(block: suspend MapController.() -> Unit) {
        lifecycleScope.launch(Dispatchers.Main.immediate) { this@launch.await().block() }
    }

    override fun onRegionWillChange(animated: Boolean) = Unit
    override fun onRegionIsChanging() = Unit
    override fun onRegionDidChange(animated: Boolean) = Unit
}
