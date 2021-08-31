package com.lookaround.ui.place.list

import android.content.res.Configuration
import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Switch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.lookaround.core.android.ext.assistedActivityViewModel
import com.lookaround.core.android.ext.assistedViewModel
import com.lookaround.core.android.ext.captureFrame
import com.lookaround.core.android.ext.init
import com.lookaround.core.android.map.MapCaptureCache
import com.lookaround.core.android.map.scene.MapSceneViewModel
import com.lookaround.core.android.map.scene.model.MapScene
import com.lookaround.core.android.map.scene.model.MapSceneIntent
import com.lookaround.core.android.map.scene.model.MapSceneSignal
import com.lookaround.core.android.model.WithValue
import com.lookaround.core.android.view.composable.BottomSheetHeaderText
import com.lookaround.core.android.view.composable.PlaceItem
import com.lookaround.core.android.view.theme.LookARoundTheme
import com.lookaround.core.delegate.lazyAsync
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.locationReadyUpdates
import com.lookaround.ui.place.list.databinding.FragmentPlacesBinding
import com.mapzen.tangram.*
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
import uk.co.senab.bitmapcache.CacheableBitmapDrawable

@ExperimentalCoroutinesApi
@FlowPreview
@AndroidEntryPoint
@WithFragmentBindings
class PlacesFragment :
    Fragment(R.layout.fragment_places), MapController.SceneLoadListener, MapChangeListener {
    private val binding: FragmentPlacesBinding by viewBinding(FragmentPlacesBinding::bind)

    @Inject internal lateinit var mainViewModelFactory: MainViewModel.Factory
    private val mainViewModel: MainViewModel by assistedActivityViewModel {
        mainViewModelFactory.create(it)
    }

    @Inject internal lateinit var viewModelFactory: MapSceneViewModel.Factory
    private val viewModel: MapSceneViewModel by assistedViewModel { viewModelFactory.create(it) }

    @Inject internal lateinit var mapTilesHttpHandler: HttpHandler
    @Inject internal lateinit var glViewHolderFactory: GLViewHolderFactory
    private val mapController: Deferred<MapController> by
        lifecycleScope.lazyAsync { binding.map.init(mapTilesHttpHandler, glViewHolderFactory) }

    @Inject internal lateinit var mapCaptureCache: MapCaptureCache
    private var processingPlaces: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mapController.launch {
            setSceneLoadListener(this@PlacesFragment)
            setMapChangeListener(this@PlacesFragment)
            loadScene(MapScene.BUBBLE_WRAP)
        }

        viewModel
            .signals
            .filterIsInstance<MapSceneSignal.RetryLoadScene>()
            .onEach { mapController.await().loadScene(it.scene) }
            .launchIn(lifecycleScope)

        binding.placeMapRecyclerView.setContent {
            LookARoundTheme {
                val markers = mainViewModel.states.collectAsState().value.markers
                val flag = remember { mutableStateOf(false) }
                if (markers is WithValue) {
                    Column(Modifier.padding(horizontal = 10.dp)) {
                        BottomSheetHeaderText("Places")
                        Switch(flag.value, flag::value::set)
                        val configuration = LocalConfiguration.current
                        LazyColumn(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                markers.value.chunked(
                                    size =
                                        if (configuration.orientation ==
                                                Configuration.ORIENTATION_LANDSCAPE
                                        ) {
                                            3
                                        } else {
                                            2
                                        }
                                )
                            ) { chunk ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.wrapContentHeight()
                                ) {
                                    chunk.forEach { point ->
                                        if (flag.value) {
                                            PlaceItem(
                                                point,
                                                mainViewModel.locationReadyUpdates,
                                                Modifier.weight(1f)
                                            )
                                        } else {
                                            PlaceMapListItem(
                                                point,
                                                mainViewModel.locationReadyUpdates,
                                                this@PlacesFragment::getBitmapFor,
                                                Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
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
        processingPlaces = true
    }

    private suspend fun getBitmapFor(location: Location): Bitmap {
        val cached = getCachedBitmap(location)
        if (cached != null) return cached.bitmap
        val bitmap = captureBitmap(location)
        lifecycleScope.launch { cacheBitmap(location, bitmap) }
        return bitmap
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

    private suspend fun getCachedBitmap(location: Location): CacheableBitmapDrawable? =
        if (mapCaptureCache.isEnabled) withContext(Dispatchers.IO) { mapCaptureCache[location] }
        else null

    private suspend fun cacheBitmap(location: Location, bitmap: Bitmap) {
        if (!mapCaptureCache.isEnabled) return
        withContext(Dispatchers.IO) { mapCaptureCache[location] = bitmap }
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
