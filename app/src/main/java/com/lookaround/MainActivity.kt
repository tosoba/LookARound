package com.lookaround

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.collectAsState
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.lookaround.core.android.ext.*
import com.lookaround.core.android.model.*
import com.lookaround.core.android.view.theme.LookARoundTheme
import com.lookaround.databinding.ActivityMainBinding
import com.lookaround.ui.camera.CameraFragment
import com.lookaround.ui.main.*
import com.lookaround.ui.main.model.MainIntent
import com.lookaround.ui.main.model.MainSignal
import com.lookaround.ui.main.model.MainState
import com.lookaround.ui.map.MapFragment
import com.lookaround.ui.place.list.PlaceListFragment
import com.lookaround.ui.place.list.PlaceMapItemActionController
import com.lookaround.ui.place.types.PlaceTypesFragment
import com.lookaround.ui.recent.searches.RecentSearchesFragment
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

    private val bottomSheetBehavior by
        lazy(LazyThreadSafetyMode.NONE) {
            BottomSheetBehavior.from(binding.bottomSheetFragmentContainerView)
        }
    private var latestARState: ARState = ARState.INITIAL

    private val bottomSheetFragments: Map<Class<out Fragment>, Fragment> by
        lazy(LazyThreadSafetyMode.NONE) {
            val bottomSheetFragmentClasses =
                listOf(
                    PlaceTypesFragment::class.java,
                    PlaceListFragment::class.java,
                    RecentSearchesFragment::class.java
                )
            buildMap {
                putAll(
                    supportFragmentManager.fragments
                        .filter { bottomSheetFragmentClasses.contains(it::class.java) }
                        .map { it::class.java to it }
                )
                bottomSheetFragmentClasses.forEach {
                    if (!containsKey(it)) put(it, it.newInstance())
                }
            }
        }

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

                fragmentTransaction {
                    when (menuItem.itemId) {
                        R.id.action_place_types -> showBottomSheetFragment<PlaceTypesFragment>()
                        R.id.action_place_list -> showBottomSheetFragment<PlaceListFragment>()
                        R.id.action_recent_searches ->
                            showBottomSheetFragment<RecentSearchesFragment>()
                        else -> throw IllegalArgumentException()
                    }
                }
                if (latestARState == ARState.ENABLED) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                }

                true
            }
        }

    private inline fun <reified F : Fragment> FragmentTransaction.showBottomSheetFragment() {
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        bottomSheetFragments
            .filter { (clazz, fragment) -> clazz != F::class.java && fragment.isAdded }
            .map(Map.Entry<Class<out Fragment>, Fragment>::value)
            .forEach(this::hide)
        show(requireNotNull(bottomSheetFragments[F::class.java]))
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
            .filter { latestARState == ARState.ENABLED }
            .onEach { (targetVisibility) -> setSearchbarVisibility(targetVisibility) }
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
        binding.bottomSheetFragmentContainerView.visibility = View.GONE
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    private fun onAREnabled() {
        latestARState = ARState.ENABLED
        binding.searchBarView.visibility = View.VISIBLE
        binding.bottomNavigationView.visibility = View.VISIBLE
        binding.bottomSheetFragmentContainerView.visibility = View.VISIBLE
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
        binding.bottomSheetFragmentContainerView.visibility = View.GONE
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
        val searchFocusedFlow =
            viewModel.states.map(MainState::searchFocused::get).distinctUntilChanged()
        binding.searchBarView.setContent {
            ProvideWindowInsets {
                LookARoundTheme {
                    val searchFocused = searchFocusedFlow.collectAsState(initial = false)
                    val state = viewModel.state
                    SearchBar(
                        query = state.autocompleteSearchQuery,
                        focused = searchFocused.value,
                        onBackPressedDispatcher = onBackPressedDispatcher,
                        onSearchFocusChange = { focused ->
                            lifecycleScope.launch {
                                viewModel.intent(MainIntent.SearchFocusChanged(focused))
                            }
                        },
                        onTextFieldValueChange = { textValue ->
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
        with(bottomSheetBehavior) {
            addBottomSheetCallback(
                object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) =
                        onBottomSheetStateChanged(newState)
                    override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
                }
            )

            viewModel
                .bottomSheetStateUpdates
                .onEach { sheetState ->
                    when (sheetState) {
                        BottomSheetBehavior.STATE_EXPANDED -> setSearchbarVisibility(View.GONE)
                        BottomSheetBehavior.STATE_HIDDEN -> {
                            if (latestARState == ARState.ENABLED) {
                                setSearchbarVisibility(View.VISIBLE)
                            }
                            binding.bottomNavigationView.selectedItemId = R.id.action_unchecked
                        }
                    }
                }
                .launchIn(lifecycleScope)
        }
    }

    private fun initBottomNavigationView() {
        with(binding.bottomNavigationView) {
            selectedItemId = viewModel.state.selectedBottomNavigationViewItemId
            setOnItemSelectedListener(onBottomNavItemSelectedListener)

            fragmentTransaction {
                val notAddedFragments = bottomSheetFragments.values.filterNot(Fragment::isAdded)
                if (notAddedFragments.isNotEmpty()) {
                    notAddedFragments.forEach { fragment ->
                        add(R.id.bottom_sheet_fragment_container_view, fragment)
                        bottomSheetFragments.values.forEach(this::hide)
                    }
                }
                binding.bottomSheetFragmentContainerView.visibility = View.VISIBLE
            }

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

    private fun setSearchbarVisibility(visibility: Int) {
        binding.searchBarView.fadeSetVisibility(visibility)
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
