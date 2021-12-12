package com.lookaround

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.collectAsState
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.lookaround.core.android.ext.*
import com.lookaround.core.android.model.*
import com.lookaround.core.android.view.theme.LookARoundTheme
import com.lookaround.core.android.view.viewpager.DiffUtilFragmentStateAdapter
import com.lookaround.databinding.ActivityMainBinding
import com.lookaround.ui.camera.CameraFragment
import com.lookaround.ui.main.*
import com.lookaround.ui.main.model.MainIntent
import com.lookaround.ui.main.model.MainSearchMode
import com.lookaround.ui.main.model.MainSignal
import com.lookaround.ui.main.model.MainState
import com.lookaround.ui.map.MapFragment
import com.lookaround.ui.place.list.PlaceMapItemActionController
import com.lookaround.ui.search.composable.SearchBar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
@ExperimentalStdlibApi
@FlowPreview
class MainActivity : AppCompatActivity(), PlaceMapItemActionController {
    private val binding: ActivityMainBinding by viewBinding(ActivityMainBinding::bind)

    @Inject internal lateinit var viewModelFactory: MainViewModel.Factory
    private val viewModel: MainViewModel by assistedViewModel { viewModelFactory.create(it) }

    private var latestARState: ARState = ARState.INITIAL

    private val bottomSheetBehavior by
        lazy(LazyThreadSafetyMode.NONE) { BottomSheetBehavior.from(binding.bottomSheetViewPager) }

    private val bottomSheetViewPagerAdapter by
        lazy(LazyThreadSafetyMode.NONE) { DiffUtilFragmentStateAdapter(this@MainActivity) }

    private var placesStatusLoadingSnackbar: Snackbar? = null

    private val onBottomNavItemSelectedListener by
        lazy(LazyThreadSafetyMode.NONE) {
            NavigationBarView.OnItemSelectedListener { menuItem ->
                lifecycleScope.launch {
                    viewModel.intent(MainIntent.BottomNavigationViewItemSelected(menuItem.itemId))
                }
                if (menuItem.itemId == R.id.action_unchecked || !menuItem.isVisible) {
                    return@OnItemSelectedListener true
                }

                binding.bottomSheetViewPager.setCurrentItem(
                    bottomSheetViewPagerAdapter.fragmentFactories.indexOf(
                        when (menuItem.itemId) {
                            R.id.action_place_types -> MainFragmentFactory.PLACE_TYPES
                            R.id.action_place_list -> MainFragmentFactory.PLACE_LIST
                            R.id.action_recent_searches -> MainFragmentFactory.RECENT_SEARCHES
                            else -> throw IllegalArgumentException()
                        }
                    ),
                    bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED
                )

                if (latestARState == ARState.ENABLED) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                }

                lifecycleScope.launch {
                    viewModel.intent(
                        MainIntent.SearchModeChanged(
                            when (menuItem.itemId) {
                                R.id.action_place_types -> MainSearchMode.PLACE_TYPES
                                R.id.action_place_list -> MainSearchMode.PLACE_LIST
                                R.id.action_recent_searches -> MainSearchMode.RECENT
                                else -> throw IllegalArgumentException()
                            }
                        )
                    )
                }

                true
            }
        }

    private val currentTopFragment: Fragment?
        get() = supportFragmentManager.findFragmentById(R.id.main_fragment_container)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        makeFullScreenWithTransparentBars()

        supportFragmentManager.addOnBackStackChangedListener { signalTopFragmentChanged(false) }

        initSearch()
        initBottomSheet()
        initBottomNavigationView()

        viewModel
            .signals
            .filterIsInstance<MainSignal.ARLoading>()
            .onEach { onARLoading() }
            .launchIn(lifecycleScope)
        viewModel
            .signals
            .filterIsInstance<MainSignal.AREnabled>()
            .onEach { onAREnabled() }
            .launchIn(lifecycleScope)
        viewModel
            .signals
            .filterIsInstance<MainSignal.ARDisabled>()
            .onEach { onARDisabled() }
            .launchIn(lifecycleScope)
        viewModel
            .signals
            .filterIsInstance<MainSignal.ToggleSearchBarVisibility>()
            .onEach { (targetVisibility) -> changeSearchbarVisibility(targetVisibility) }
            .launchIn(lifecycleScope)

        viewModel
            .locationUpdateFailureUpdates
            .onEach { Timber.tag("LOCATION").e("Failed to update location.") }
            .launchIn(lifecycleScope)

        launchPlacesLoadingSnackbarUpdates()
    }

    override fun onResume() {
        super.onResume()
        signalTopFragmentChanged(true)
    }

    override fun onBackPressed() {
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            if (viewModel.state.searchFocused) super.onBackPressed()
        } else {
            super.onBackPressed()
        }
    }

    override fun onPlaceMapItemClick(marker: Marker) {
        if (!lifecycle.isResumed) return
        when (val topFragment = currentTopFragment) {
            is MapFragment -> topFragment.updateMarker(marker)
            else -> {
                fragmentTransaction {
                    setSlideInFromBottom()
                    add(R.id.main_fragment_container, MapFragment.new(marker))
                    addToBackStack(null)
                }
            }
        }
    }

    private fun onARLoading() {
        latestARState = ARState.LOADING
        binding.searchBarView.visibility = View.GONE
        binding.bottomNavigationView.visibility = View.GONE
        binding.bottomSheetViewPager.visibility = View.GONE
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    private fun onAREnabled() {
        latestARState = ARState.ENABLED
        binding.searchBarView.visibility = View.VISIBLE
        binding.bottomNavigationView.visibility = View.VISIBLE
        binding.bottomSheetViewPager.visibility = View.VISIBLE
        bottomSheetBehavior.state = viewModel.state.lastLiveBottomSheetState
    }

    private fun onARDisabled() {
        if (latestARState == ARState.ENABLED &&
                bottomSheetBehavior.state != BottomSheetBehavior.STATE_SETTLING
        ) {
            lifecycleScope.launch {
                viewModel.intent(MainIntent.LiveBottomSheetStateChanged(bottomSheetBehavior.state))
            }
        }
        latestARState = ARState.DISABLED

        binding.searchBarView.visibility = View.GONE
        binding.bottomNavigationView.visibility = View.GONE
        binding.bottomSheetViewPager.visibility = View.GONE
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    private fun signalTopFragmentChanged(onResume: Boolean) {
        lifecycleScope.launch {
            viewModel.signal(
                MainSignal.TopFragmentChanged(
                    cameraObscured = currentTopFragment !is CameraFragment,
                    onResume
                )
            )
        }
    }

    private fun initSearch() {
        val searchModes = viewModel.states.map(MainState::searchMode::get).distinctUntilChanged()
        val searchFocusedFlow =
            viewModel.states.map(MainState::searchFocused::get).distinctUntilChanged()
        binding.searchBarView.setContent {
            ProvideWindowInsets {
                LookARoundTheme {
                    val searchMode =
                        searchModes.collectAsState(initial = MainSearchMode.AUTOCOMPLETE)
                    val searchFocused = searchFocusedFlow.collectAsState(initial = false)
                    val state = viewModel.state
                    SearchBar(
                        query =
                            when (searchMode.value) {
                                MainSearchMode.AUTOCOMPLETE -> state.autocompleteSearchQuery
                                MainSearchMode.PLACE_TYPES -> state.placeTypesSearchQuery
                                MainSearchMode.PLACE_LIST -> state.placeListSearchQuery
                                MainSearchMode.RECENT -> state.recentSearchQuery
                            },
                        searchFocused = searchFocused.value,
                        onSearchFocusChange = { focused ->
                            lifecycleScope.launch {
                                viewModel.intent(MainIntent.SearchFocusChanged(focused))
                            }
                        },
                        onTextValueChange = { textValue ->
                            lifecycleScope.launch {
                                viewModel.intent(MainIntent.SearchQueryChanged(textValue.text))
                            }
                        }
                    )
                }
            }
        }
    }

    private fun initBottomSheet() {
        bottomSheetBehavior.addBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) =
                    onBottomSheetStateChanged(newState)
                override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
            }
        )

        with(binding.bottomSheetViewPager) {
            offscreenPageLimit = MainFragmentFactory.values().size - 1
            adapter = bottomSheetViewPagerAdapter
            registerOnPageChangeCallback(
                object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) return
                        val factory = bottomSheetViewPagerAdapter.fragmentFactories[position]
                        binding.bottomNavigationView.selectedItemId =
                            when (factory) {
                                MainFragmentFactory.PLACE_TYPES -> R.id.action_place_types
                                MainFragmentFactory.PLACE_LIST -> R.id.action_place_list
                                MainFragmentFactory.RECENT_SEARCHES -> R.id.action_recent_searches
                                else -> throw IllegalArgumentException()
                            }
                    }
                }
            )
            disableNestedScrolling()
        }

        combine(
                viewModel.placesBottomNavItemVisibilityUpdates,
                viewModel.recentSearchesBottomNavItemVisibilityUpdates
            ) { placesVisible, recentSearchesVisible ->
                val factories = mutableListOf(MainFragmentFactory.PLACE_TYPES)
                if (placesVisible) factories.add(MainFragmentFactory.PLACE_LIST)
                if (recentSearchesVisible) factories.add(MainFragmentFactory.RECENT_SEARCHES)
                factories
            }
            .distinctUntilChanged()
            .onEach { factories ->
                val currentFragmentFactory =
                    if (bottomSheetViewPagerAdapter.fragmentFactories.isNotEmpty()) {
                        bottomSheetViewPagerAdapter.fragmentFactories[
                            binding.bottomSheetViewPager.currentItem]
                    } else {
                        null
                    }
                bottomSheetViewPagerAdapter.fragmentFactories = factories
                currentFragmentFactory?.let { fragmentFactory ->
                    factories
                        .indexOf(fragmentFactory)
                        .takeIf { itemIndex -> itemIndex != -1 }
                        ?.let { itemIndex ->
                            binding.bottomSheetViewPager.setCurrentItem(itemIndex, false)
                        }
                }
            }
            .launchIn(lifecycleScope)

        viewModel
            .bottomSheetStateUpdates
            .onEach { sheetState ->
                when (sheetState) {
                    BottomSheetBehavior.STATE_EXPANDED -> changeSearchbarVisibility(View.VISIBLE)
                    BottomSheetBehavior.STATE_HIDDEN ->
                        binding.bottomNavigationView.selectedItemId = R.id.action_unchecked
                }
            }
            .launchIn(lifecycleScope)
    }

    private fun initBottomNavigationView() {
        with(binding.bottomNavigationView) {
            selectedItemId = viewModel.state.selectedBottomNavigationViewItemId
            setOnItemSelectedListener(onBottomNavItemSelectedListener)

            viewModel
                .placesBottomNavItemVisibilityUpdates
                .onEach { isVisible -> menu.findItem(R.id.action_place_list).isVisible = isVisible }
                .launchIn(lifecycleScope)

            viewModel
                .recentSearchesBottomNavItemVisibilityUpdates
                .onEach { isVisible ->
                    menu.findItem(R.id.action_recent_searches).isVisible = isVisible
                }
                .launchIn(lifecycleScope)
        }
    }

    private fun onBottomSheetStateChanged(@BottomSheetBehavior.State sheetState: Int) {
        if (latestARState == ARState.ENABLED && sheetState != BottomSheetBehavior.STATE_SETTLING) {
            lifecycleScope.launch {
                viewModel.intent(MainIntent.LiveBottomSheetStateChanged(sheetState))
            }
        }
        lifecycleScope.launch { viewModel.signal(MainSignal.BottomSheetStateChanged(sheetState)) }
    }

    private fun changeSearchbarVisibility(targetVisibility: Int) {
        binding.searchBarView.apply {
            val delta = -250f
            if (targetVisibility == View.GONE) {
                slideChangeVisibility(targetVisibility, toYDelta = delta)
            } else {
                slideChangeVisibility(targetVisibility, fromYDelta = delta)
            }
        }
    }

    private fun launchPlacesLoadingSnackbarUpdates() {
        viewModel
            .signals
            .filterIsInstance<MainSignal.PlacesLoadingFailed>()
            .onEach {
                placesStatusLoadingSnackbar?.dismiss()
                placesStatusLoadingSnackbar =
                    showPlacesLoadingStatusSnackbar(
                        getString(R.string.loading_places_failed),
                        Snackbar.LENGTH_SHORT
                    )
            }
            .launchIn(lifecycleScope)

        viewModel
            .signals
            .filterIsInstance<MainSignal.UnableToLoadPlacesWithoutLocation>()
            .onEach {
                placesStatusLoadingSnackbar?.dismiss()
                showPlacesLoadingStatusSnackbar(
                    getString(R.string.location_unavailable),
                    Snackbar.LENGTH_SHORT
                )
            }
            .launchIn(lifecycleScope)

        viewModel
            .signals
            .filterIsInstance<MainSignal.UnableToLoadPlacesWithoutConnection>()
            .onEach {
                placesStatusLoadingSnackbar?.dismiss()
                showPlacesLoadingStatusSnackbar(
                    getString(R.string.no_internet_connection),
                    Snackbar.LENGTH_SHORT
                )
            }
            .launchIn(lifecycleScope)

        viewModel
            .markerUpdates
            .onEach { markers ->
                placesStatusLoadingSnackbar?.dismiss()
                if (markers is Loading) {
                    placesStatusLoadingSnackbar =
                        showPlacesLoadingStatusSnackbar(
                            getString(R.string.loading_places_in_progress),
                            Snackbar.LENGTH_INDEFINITE
                        )
                }
            }
            .launchIn(lifecycleScope)
    }

    private fun showPlacesLoadingStatusSnackbar(
        message: String,
        @BaseTransientBottomBar.Duration length: Int
    ): Snackbar =
        Snackbar.make(binding.mainLayout, message, length)
            .setAnchorView(binding.bottomNavigationView)
            .apply {
                fun signalSnackbarStatusChanged(isShowing: Boolean) {
                    lifecycleScope.launch {
                        viewModel.signal(MainSignal.SnackbarStatusChanged(isShowing))
                    }
                }

                addCallback(
                    object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                        override fun onShown(transientBottomBar: Snackbar?) {
                            transientBottomBar?.let {
                                signalSnackbarStatusChanged(isShowing = true)
                            }
                        }
                        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                            signalSnackbarStatusChanged(isShowing = false)
                        }
                    }
                )
                show()
            }

    private enum class ARState {
        INITIAL,
        LOADING,
        ENABLED,
        DISABLED
    }
}
