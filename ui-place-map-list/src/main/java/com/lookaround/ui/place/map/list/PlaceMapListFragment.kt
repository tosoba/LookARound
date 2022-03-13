package com.lookaround.ui.place.map.list

import android.content.res.Configuration
import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.accompanist.insets.ProvideWindowInsets
import com.lookaround.core.android.ext.*
import com.lookaround.core.android.map.LocationBitmapCaptureCache
import com.lookaround.core.android.map.scene.MapSceneViewModel
import com.lookaround.core.android.map.scene.model.MapScene
import com.lookaround.core.android.map.scene.model.MapSceneIntent
import com.lookaround.core.android.map.scene.model.MapSceneSignal
import com.lookaround.core.android.model.Marker
import com.lookaround.core.android.model.ParcelableSortedSet
import com.lookaround.core.android.model.WithValue
import com.lookaround.core.android.view.composable.SearchBar
import com.lookaround.core.android.view.recyclerview.LocationRecyclerViewAdapterCallbacks
import com.lookaround.core.android.view.recyclerview.contrastingColorCallbacks
import com.lookaround.core.android.view.theme.LookARoundTheme
import com.lookaround.core.delegate.lazyAsync
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.locationReadyUpdates
import com.lookaround.ui.main.model.MainIntent
import com.lookaround.ui.main.model.MainSignal
import com.lookaround.ui.main.model.MainState
import com.lookaround.ui.place.list.BuildConfig
import com.lookaround.ui.place.list.R
import com.lookaround.ui.place.list.databinding.FragmentPlaceMapListBinding
import com.mapzen.tangram.*
import com.mapzen.tangram.networking.HttpHandler
import com.mapzen.tangram.viewholder.GLViewHolderFactory
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import uk.co.senab.bitmapcache.CacheableBitmapDrawable

@AndroidEntryPoint
@WithFragmentBindings
@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
@FlowPreview
class PlaceMapListFragment :
    Fragment(R.layout.fragment_place_map_list), MapController.SceneLoadListener, MapChangeListener {
    private val binding: FragmentPlaceMapListBinding by
        viewBinding(FragmentPlaceMapListBinding::bind)

    private val mainViewModel: MainViewModel by activityViewModels()
    private val viewModel: MapSceneViewModel by viewModels()

    @Inject internal lateinit var mapTilesHttpHandler: HttpHandler
    @Inject internal lateinit var glViewHolderFactory: GLViewHolderFactory
    private val mapController: Deferred<MapController> by
        lifecycleScope.lazyAsync { binding.map.init(mapTilesHttpHandler, glViewHolderFactory) }
    private val mapReady = CompletableDeferred<Unit>()

    @Inject internal lateinit var locationBitmapCaptureCache: LocationBitmapCaptureCache
    private var currentGetLocationBitmapIncrement = 0L
    private val getLocationBitmapFlow = MutableSharedFlow<GetLocationBitmapRequest>()
    private val reloadBitmapTrigger = MutableSharedFlow<Unit>()

    private var searchQuery: String = ""
    private var searchFocused: Boolean = false

    private val placeMapsRecyclerViewAdapter by
        lazy(LazyThreadSafetyMode.NONE) {
            PlaceMapsRecyclerViewAdapter(
                viewLifecycleOwner.lifecycleScope.contrastingColorCallbacks(
                    mainViewModel.filterSignals(MainSignal.ContrastingColorUpdated::color)
                ),
                object : PlaceMapsRecyclerViewAdapter.BitmapCallbacks {
                    private val loadBitmapJobs = mutableMapOf<UUID, Job>()
                    private val reloadBitmapJobs = mutableMapOf<UUID, Job>()

                    override fun onBindViewHolder(
                        uuid: UUID,
                        location: Location,
                        onBitmapLoadingStarted: () -> Unit,
                        onBitmapLoaded: (bitmap: Bitmap) -> Unit
                    ) {
                        loadBitmap(uuid, location, onBitmapLoaded)
                        if (!reloadBitmapJobs.containsKey(uuid)) {
                            reloadBitmapJobs[uuid] =
                                reloadBitmapTrigger
                                    .onEach {
                                        loadBitmapJobs.remove(uuid)?.cancel()
                                        onBitmapLoadingStarted()
                                        loadBitmap(uuid, location, onBitmapLoaded)
                                    }
                                    .launchIn(viewLifecycleOwner.lifecycleScope)
                        }
                    }

                    private fun loadBitmap(
                        uuid: UUID,
                        location: Location,
                        action: (bitmap: Bitmap) -> Unit
                    ) {
                        if (!loadBitmapJobs.containsKey(uuid)) {
                            loadBitmapJobs[uuid] =
                                viewLifecycleOwner.lifecycleScope
                                    .launch { action(getBitmapFor(location)) }
                                    .apply { invokeOnCompletion { loadBitmapJobs.remove(uuid) } }
                        }
                    }

                    override fun onViewDetachedFromWindow(uuid: UUID) {
                        loadBitmapJobs.remove(uuid)?.cancel()
                        reloadBitmapJobs.remove(uuid)?.cancel()
                    }

                    override fun onDetachedFromRecyclerView() {
                        loadBitmapJobs.values.forEach(Job::cancel)
                        reloadBitmapJobs.values.forEach(Job::cancel)
                    }
                },
                object : LocationRecyclerViewAdapterCallbacks {
                    private val jobs = mutableMapOf<UUID, Job>()

                    override fun onBindViewHolder(
                        uuid: UUID,
                        action: (userLocation: Location) -> Unit
                    ) {
                        if (jobs.containsKey(uuid)) return
                        jobs[uuid] =
                            mainViewModel
                                .locationReadyUpdates
                                .onEach(action)
                                .launchIn(viewLifecycleOwner.lifecycleScope)
                    }

                    override fun onViewDetachedFromWindow(uuid: UUID) {
                        jobs.remove(uuid)?.cancel()
                    }

                    override fun onDetachedFromRecyclerView() {
                        jobs.values.forEach(Job::cancel)
                    }
                }
            ) { namedLocation ->
                (activity as? PlaceMapListFragmentHost)?.onPlaceMapItemClick(namedLocation)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) initFromSavedState(savedInstanceState)
    }

    private fun initFromSavedState(savedInstanceState: Bundle) {
        with(savedInstanceState) {
            getString(SavedStateKey.SEARCH_QUERY.name)?.let(::searchQuery::set)
            searchFocused = getBoolean(SavedStateKey.SEARCH_FOCUSED.name)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mapController.launch {
            setSceneLoadListener(this@PlaceMapListFragment)
            setMapChangeListener(this@PlaceMapListFragment)
            loadScene(MapScene.WALKABOUT)
        }

        viewModel
            .onEachSignal<MapSceneSignal.RetryLoadScene> { (scene) ->
                mapController.await().loadScene(scene)
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        val mapLayoutParams = getMapLayoutParams()
        binding.map.layoutParams = mapLayoutParams

        binding.reloadMapsFab.setOnClickListener {
            ++currentGetLocationBitmapIncrement
            viewLifecycleOwner.lifecycleScope.launch {
                withContext(Dispatchers.IO) { locationBitmapCaptureCache.clear() }
                reloadBitmapTrigger.emit(Unit)
            }
        }

        binding.showMapFab.setOnClickListener {
            (activity as? PlaceMapListFragmentHost)?.let {
                it.onShowMapClick()
                viewLifecycleOwner.lifecycleScope.launchWhenResumed {
                    mainViewModel.signal(MainSignal.HideBottomSheet)
                }
            }
        }

        binding.clearAllFab.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launchWhenResumed {
                mainViewModel.intent(MainIntent.ClearMarkers)
                mainViewModel.signal(MainSignal.HideBottomSheet)
            }
        }

        val searchQueryFlow = MutableStateFlow(searchQuery)

        binding.placeMapsSearchBar.setContent {
            ProvideWindowInsets {
                LookARoundTheme {
                    val searchQueryState = searchQueryFlow.collectAsState(initial = searchQuery)
                    var searchFocusedState by remember { mutableStateOf(searchFocused) }
                    SearchBar(
                        query = searchQueryState.value,
                        focused = searchFocusedState,
                        onBackPressedDispatcher = requireActivity().onBackPressedDispatcher,
                        onSearchFocusChange = {
                            searchFocusedState = it
                            searchFocused = it
                        },
                        onTextFieldValueChange = {
                            searchQueryFlow.value = it.text
                            searchQuery = it.text
                        },
                        modifier = Modifier.onSizeChanged { addTopSpacer(it.height) }
                    )
                }
            }
        }

        mainViewModel
            .mapStates(MainState::markers)
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
            .onEach { markers ->
                placeMapsRecyclerViewAdapter.updateItems(
                    markers.map(PlaceMapsRecyclerViewAdapter.Item::Map)
                )
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
        binding.placeMapsRecyclerView.adapter = placeMapsRecyclerViewAdapter
        val orientation = resources.configuration.orientation
        val spanCount = if (orientation == Configuration.ORIENTATION_LANDSCAPE) 4 else 2
        binding.placeMapsRecyclerView.layoutManager =
            GridLayoutManager(requireContext(), spanCount, GridLayoutManager.VERTICAL, false)
                .apply {
                    spanSizeLookup =
                        object : GridLayoutManager.SpanSizeLookup() {
                            override fun getSpanSize(position: Int): Int =
                                when (placeMapsRecyclerViewAdapter.items[position]) {
                                    is PlaceMapsRecyclerViewAdapter.Item.Spacer -> spanCount
                                    else -> 1
                                }
                        }
                }
        binding.placeMapsRecyclerView.addCollapseTopViewOnScrollListener(binding.placeMapsSearchBar)
    }

    private fun addTopSpacer(height: Int) {
        if (placeMapsRecyclerViewAdapter.items[0] is PlaceMapsRecyclerViewAdapter.Item.Spacer) {
            binding.placeMapsRecyclerView.visibility = View.VISIBLE
            return
        }
        val layoutManager = binding.placeMapsRecyclerView.layoutManager as LinearLayoutManager
        val wasNotScrolled = layoutManager.findFirstCompletelyVisibleItemPosition() == 0
        placeMapsRecyclerViewAdapter.addTopSpacer(PlaceMapsRecyclerViewAdapter.Item.Spacer(height))
        binding.placeMapsRecyclerView.apply {
            if (wasNotScrolled) scrollToTopAndShow() else visibility = View.VISIBLE
        }
    }

    private fun RecyclerView.scrollToTopAndShow() {
        post {
            scrollToPosition(0)
            visibility = View.VISIBLE
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(SavedStateKey.SEARCH_QUERY.name, searchQuery)
        outState.putBoolean(SavedStateKey.SEARCH_FOCUSED.name, searchFocused)
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
        if (view == null) return

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

        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            getLocationBitmapFlow
                .filter { it.increment == currentGetLocationBitmapIncrement }
                .collect { (location, deferred, _) ->
                    val bitmap = captureBitmapAndCacheBitmap(location)
                    deferred.complete(bitmap)
                }
        }
        mapReady.complete(Unit)
    }

    private fun getMapLayoutParams(): ViewGroup.LayoutParams {
        val mapDimensionPx = getListItemDimensionPx()
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
        getLocationBitmapFlow.emit(
            GetLocationBitmapRequest(location, deferredBitmap, currentGetLocationBitmapIncrement)
        )
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
            removeAllMarkers()
            addMarkerFor(location)
            captureFrame(true)
        }

    private suspend fun getCachedBitmap(location: Location): CacheableBitmapDrawable? =
        if (locationBitmapCaptureCache.isEnabled) {
            withContext(Dispatchers.IO) { locationBitmapCaptureCache[location] }
        } else {
            null
        }

    private suspend fun cacheBitmap(location: Location, bitmap: Bitmap) {
        if (!locationBitmapCaptureCache.isEnabled) return
        withContext(Dispatchers.IO) { locationBitmapCaptureCache[location] = bitmap }
    }

    private suspend fun MapController.loadScene(scene: MapScene) {
        viewModel.intent(MapSceneIntent.LoadingScene(scene))
        loadSceneFile(
            scene.url,
            listOf(SceneUpdate("global.sdk_api_key", BuildConfig.NEXTZEN_API_KEY))
        )
    }

    private fun Deferred<MapController>.launch(block: suspend MapController.() -> Unit) {
        if (view == null) return
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main.immediate) {
            this@launch.await().block()
        }
    }

    override fun onRegionWillChange(animated: Boolean) = Unit
    override fun onRegionIsChanging() = Unit
    override fun onRegionDidChange(animated: Boolean) = Unit

    private enum class SavedStateKey {
        SEARCH_QUERY,
        SEARCH_FOCUSED
    }

    private data class GetLocationBitmapRequest(
        val location: Location,
        val deferredBitmap: CompletableDeferred<Bitmap>,
        val increment: Long = 0L
    )
}
