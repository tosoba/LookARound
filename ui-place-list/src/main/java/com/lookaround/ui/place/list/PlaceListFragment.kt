package com.lookaround.ui.place.list

import android.content.res.Configuration
import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.lookaround.core.android.ext.*
import com.lookaround.core.android.map.MapCaptureCache
import com.lookaround.core.android.map.scene.MapSceneViewModel
import com.lookaround.core.android.map.scene.model.MapScene
import com.lookaround.core.android.map.scene.model.MapSceneIntent
import com.lookaround.core.android.map.scene.model.MapSceneSignal
import com.lookaround.core.android.model.Marker
import com.lookaround.core.android.model.ParcelableSortedSet
import com.lookaround.core.android.model.WithValue
import com.lookaround.core.android.view.theme.LookARoundTheme
import com.lookaround.core.delegate.lazyAsync
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.bottomSheetStateUpdates
import com.lookaround.ui.main.locationReadyUpdates
import com.lookaround.ui.main.model.MainState
import com.lookaround.ui.place.list.databinding.FragmentPlaceListBinding
import com.lookaround.ui.search.composable.SearchBar
import com.mapzen.tangram.*
import com.mapzen.tangram.networking.HttpHandler
import com.mapzen.tangram.viewholder.GLViewHolderFactory
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import timber.log.Timber
import uk.co.senab.bitmapcache.CacheableBitmapDrawable

@AndroidEntryPoint
@WithFragmentBindings
@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
@FlowPreview
class PlaceListFragment :
    Fragment(R.layout.fragment_place_list), MapController.SceneLoadListener, MapChangeListener {
    private val binding: FragmentPlaceListBinding by viewBinding(FragmentPlaceListBinding::bind)

    private val mainViewModel: MainViewModel by activityViewModels()
    private val viewModel: MapSceneViewModel by viewModels()

    @Inject internal lateinit var mapTilesHttpHandler: HttpHandler
    @Inject internal lateinit var glViewHolderFactory: GLViewHolderFactory
    private val mapController: Deferred<MapController> by
        lifecycleScope.lazyAsync { binding.map.init(mapTilesHttpHandler, glViewHolderFactory) }

    @Inject internal lateinit var mapCaptureCache: MapCaptureCache
    private val getLocationBitmapFlow =
        MutableSharedFlow<Pair<Location, CompletableDeferred<Bitmap>>>()
    private val mapReady = CompletableDeferred<Unit>()

    private var searchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getString(SavedStateKey.SEARCH_QUERY.name)?.let(::searchQuery::set)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mapController.launch {
            setSceneLoadListener(this@PlaceListFragment)
            setMapChangeListener(this@PlaceListFragment)
            loadScene(MapScene.BUBBLE_WRAP)
        }

        viewModel
            .signals
            .filterIsInstance<MapSceneSignal.RetryLoadScene>()
            .onEach { (scene) -> mapController.await().loadScene(scene) }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        val mapLayoutParams = getMapLayoutParams()
        binding.map.layoutParams = mapLayoutParams

        val reloadBitmapTrigger = Channel<Unit>()
        binding.reloadMapsFab.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                withContext(Dispatchers.IO) { mapCaptureCache.clear() }
                reloadBitmapTrigger.send(Unit)
            }
        }

        val bottomSheetSignalsFlow =
            mainViewModel
                .bottomSheetStateUpdates
                .onStart { emit(mainViewModel.state.lastLiveBottomSheetState) }
                .distinctUntilChanged()

        val searchQueryFlow = MutableStateFlow(searchQuery)

        val markersFlow =
            mainViewModel
                .states
                .map(MainState::markers::get)
                .filterIsInstance<WithValue<ParcelableSortedSet<Marker>>>()
                .distinctUntilChanged()
                .combine(searchQueryFlow.map { it.trim().lowercase() }.distinctUntilChanged()) {
                    markers,
                    query ->
                    markers to query
                }
                .map { (markers, query) ->
                    markers.value.items.run {
                        if (query.isBlank()) toList()
                        else filter { marker -> marker.name.lowercase().contains(query) }
                    }
                }
                .distinctUntilChanged()

        binding.placeMapList.setContent {
            ProvideWindowInsets {
                LookARoundTheme {
                    val bottomSheetState =
                        bottomSheetSignalsFlow.collectAsState(
                            initial = BottomSheetBehavior.STATE_HIDDEN
                        )

                    binding.reloadMapsFab.visibility =
                        if (bottomSheetState.value == BottomSheetBehavior.STATE_EXPANDED) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                    val markers = markersFlow.collectAsState(initial = emptyList())
                    val searchQuery = searchQueryFlow.collectAsState(initial = "")
                    val searchFocused = rememberSaveable { mutableStateOf(false) }

                    val lazyListState = rememberLazyListState()
                    binding
                        .disallowInterceptTouchContainer
                        .shouldRequestDisallowInterceptTouchEvent =
                        (lazyListState.firstVisibleItemIndex != 0 ||
                            lazyListState.firstVisibleItemScrollOffset != 0) &&
                            bottomSheetState.value == BottomSheetBehavior.STATE_EXPANDED
                    val orientation = LocalConfiguration.current.orientation
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (bottomSheetState.value != BottomSheetBehavior.STATE_EXPANDED) {
                            item { Spacer(Modifier.height(112.dp)) }
                        } else {
                            stickyHeader {
                                SearchBar(
                                    query = searchQuery.value,
                                    focused = searchFocused.value,
                                    onBackPressedDispatcher =
                                        requireActivity().onBackPressedDispatcher,
                                    onSearchFocusChange = searchFocused::value::set,
                                    onTextFieldValueChange = {
                                        searchQueryFlow.value = it.text
                                        this@PlaceListFragment.searchQuery = it.text
                                    }
                                )
                            }
                        }

                        items(
                            markers.value.chunked(
                                if (orientation == Configuration.ORIENTATION_LANDSCAPE) 4 else 2
                            )
                        ) { chunk ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.wrapContentHeight()
                            ) {
                                chunk.forEach { point ->
                                    PlaceMapListItem(
                                        point = point,
                                        userLocationFlow = mainViewModel.locationReadyUpdates,
                                        getPlaceBitmap = this@PlaceListFragment::getBitmapFor,
                                        reloadBitmapTrigger = reloadBitmapTrigger.receiveAsFlow(),
                                        bitmapDimension =
                                            requireContext()
                                                .pxToDp(mapLayoutParams.width.toFloat())
                                                .toInt(),
                                        modifier =
                                            Modifier.weight(1f, fill = false)
                                                .border(
                                                    width = 2.dp,
                                                    brush =
                                                        Brush.horizontalGradient(
                                                            colors =
                                                                listOf(
                                                                    Color.Transparent,
                                                                    Color.LightGray
                                                                )
                                                        ),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    (activity as? PlaceMapItemActionsHandler)
                                                        ?.onPlaceMapItemClick(point)
                                                },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(SavedStateKey.SEARCH_QUERY.name, searchQuery)
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
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.intent(MapSceneIntent.SceneLoaded)
            }
        } else {
            Timber.e("Failed to load scene: $sceneId. Scene error: $sceneError")
        }
    }

    override fun onViewComplete() {
        if (mapReady.isCompleted) return
        if (!viewModel.state.sceneLoaded) {
            Timber.d("Scene is not loaded")
            return
        }

        getLocationBitmapFlow
            .onEach { (location, deferred) ->
                val bitmap = captureBitmapAndCacheBitmap(location)
                deferred.complete(bitmap)
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
        mapReady.complete(Unit)
    }

    private fun getMapLayoutParams(): ViewGroup.LayoutParams {
        val spacingPx = requireContext().dpToPx(10f)
        val displayMetrics = resources.displayMetrics
        val orientation = resources.configuration.orientation
        val mapDimensionPx =
            (displayMetrics.widthPixels -
                spacingPx * (if (orientation == Configuration.ORIENTATION_LANDSCAPE) 5 else 3)) /
                (if (orientation == Configuration.ORIENTATION_LANDSCAPE) 4 else 2)
        val mapLayoutParams = binding.map.layoutParams
        mapLayoutParams.width = mapDimensionPx.toInt()
        mapLayoutParams.height = mapDimensionPx.toInt()
        return mapLayoutParams
    }

    private suspend fun getBitmapFor(location: Location): Bitmap {
        val cached = getCachedBitmap(location)
        if (cached != null) return cached.bitmap

        mapReady.await()
        val deferredBitmap = CompletableDeferred<Bitmap>()
        getLocationBitmapFlow.emit(location to deferredBitmap)
        return deferredBitmap.await()
    }

    private suspend fun captureBitmapAndCacheBitmap(location: Location): Bitmap {
        val bitmap = captureBitmap(location)
        viewLifecycleOwner.lifecycleScope.launch { cacheBitmap(location, bitmap) }
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
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main.immediate) {
            this@launch.await().block()
        }
    }

    override fun onRegionWillChange(animated: Boolean) = Unit
    override fun onRegionIsChanging() = Unit
    override fun onRegionDidChange(animated: Boolean) = Unit

    private enum class SavedStateKey {
        SEARCH_QUERY
    }
}
