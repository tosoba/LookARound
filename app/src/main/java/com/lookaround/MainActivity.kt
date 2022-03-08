package com.lookaround

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.iterator
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.lookaround.core.android.architecture.ListFragmentHost
import com.lookaround.core.android.ext.*
import com.lookaround.core.android.model.*
import com.lookaround.core.android.view.composable.SearchBar
import com.lookaround.core.android.view.theme.LookARoundTheme
import com.lookaround.core.android.view.theme.Ocean0
import com.lookaround.core.android.view.theme.Ocean2
import com.lookaround.core.model.SearchType
import com.lookaround.databinding.ActivityMainBinding
import com.lookaround.ui.camera.CameraFragment
import com.lookaround.ui.camera.model.CameraARState
import com.lookaround.ui.main.*
import com.lookaround.ui.main.model.MainIntent
import com.lookaround.ui.main.model.MainSignal
import com.lookaround.ui.main.model.MainState
import com.lookaround.ui.map.MapFragment
import com.lookaround.ui.place.categories.PlaceCategoriesFragment
import com.lookaround.ui.place.map.list.PlaceMapListFragment
import com.lookaround.ui.place.map.list.PlaceMapListFragmentHost
import com.lookaround.ui.recent.searches.RecentSearchesFragment
import com.lookaround.ui.recent.searches.composable.RecentSearchesChipList
import dagger.hilt.android.AndroidEntryPoint
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
@SuppressLint("RtlHardcoded")
class MainActivity : AppCompatActivity(R.layout.activity_main), PlaceMapListFragmentHost {
    private val binding: ActivityMainBinding by viewBinding(ActivityMainBinding::bind)

    private val viewModel: MainViewModel by viewModels()

    private val bottomSheetBehavior by
        lazy(LazyThreadSafetyMode.NONE) {
            BottomSheetBehavior.from(binding.bottomSheetFragmentContainerView)
        }
    private var latestARState: CameraARState = CameraARState.INITIAL

    private val bottomSheetFragments: Map<Class<out Fragment>, Fragment> by
        lazy(LazyThreadSafetyMode.NONE) {
            val bottomSheetFragmentClasses =
                listOf(
                    PlaceCategoriesFragment::class.java,
                    PlaceMapListFragment::class.java,
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

    private val shouldUpdateLastLiveBottomSheetState: Boolean
        get() =
            viewsInteractionEnabled &&
                bottomSheetBehavior.state != BottomSheetBehavior.STATE_SETTLING

    private val viewsInteractionEnabled: Boolean
        get() = currentTopFragment !is CameraFragment || latestARState == CameraARState.ENABLED

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
                        R.id.action_place_categories ->
                            showBottomSheetFragment<PlaceCategoriesFragment>()
                        R.id.action_place_map_list ->
                            showBottomSheetFragment<PlaceMapListFragment>()
                        R.id.action_recent_searches ->
                            showBottomSheetFragment<RecentSearchesFragment>()
                        else -> throw IllegalArgumentException()
                    }
                }
                if (viewsInteractionEnabled) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                }

                true
            }
        }

    private val currentBottomSheetFragment: Fragment?
        get() = supportFragmentManager.findFragmentById(R.id.bottom_sheet_fragment_container_view)

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
        get() = supportFragmentManager.findFragmentById(R.id.main_fragment_container_view)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFullScreenWithTransparentBars()

        supportFragmentManager.addOnBackStackChangedListener {
            lifecycleScope.launchWhenResumed {
                signalTopFragmentChanged(false)
                viewModel.signal(MainSignal.BottomSheetStateChanged(bottomSheetBehavior.state))
            }
            setSearchbarVisibility(View.VISIBLE)
        }

        initNavigationDrawer()
        initSearch()
        initBottomSheet()
        initBottomNavigationView()
        initNearMeFab()

        viewModel.onEachSignal<MainSignal.ARLoading> { onARLoading() }.launchIn(lifecycleScope)
        viewModel.onEachSignal<MainSignal.AREnabled> { onAREnabled() }.launchIn(lifecycleScope)
        viewModel.onEachSignal<MainSignal.ARDisabled> { onARDisabled() }.launchIn(lifecycleScope)
        viewModel
            .filterSignals<MainSignal.ToggleSearchBarVisibility>()
            .filter { viewsInteractionEnabled }
            .onEach { (targetVisibility) -> setSearchbarVisibility(targetVisibility) }
            .launchIn(lifecycleScope)
        viewModel
            .locationUpdateFailureUpdates
            .onEach {
                Timber.tag("LOCATION").e(it?.message ?: "Unknown location update failure occurred.")
            }
            .launchIn(lifecycleScope)

        launchPlacesLoadingSnackbarUpdates()
    }

    override fun onPause() {
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
            binding.bottomNavigationView.visibility = View.GONE
        }
        binding.nearMeFab.visibility = View.GONE
        binding.searchBarView.visibility = View.GONE
        super.onPause()
    }

    override fun onResume() {
        super.onResume()

        if (viewsInteractionEnabled) {
            binding.bottomNavigationView.visibility = View.VISIBLE
            if (!viewModel.state.markers.hasValue) {
                binding.nearMeFab.visibility = View.VISIBLE
            }
            binding.searchBarView.visibility = View.VISIBLE
        }

        lifecycleScope.launchWhenResumed {
            signalTopFragmentChanged(true)
            viewModel.signal(MainSignal.BottomSheetStateChanged(bottomSheetBehavior.state))
        }
    }

    override fun onBackPressed() {
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            if (viewModel.state.searchFocused) super.onBackPressed()
        } else if (binding.mainDrawerLayout.isDrawerOpen(Gravity.LEFT)) {
            binding.mainDrawerLayout.closeDrawer(Gravity.LEFT)
        } else {
            super.onBackPressed()
        }
    }

    override val listItemBackground: ListFragmentHost.ItemBackground
        get() =
            if (currentTopFragment is CameraFragment) ListFragmentHost.ItemBackground.TRANSPARENT
            else ListFragmentHost.ItemBackground.OPAQUE

    override fun onPlaceMapItemClick(marker: Marker) {
        if (!lifecycle.isResumed) return
        when (val topFragment = currentTopFragment) {
            is MapFragment -> topFragment.updateCurrentMarker(marker)
            else -> {
                fragmentTransaction {
                    setSlideInFromBottom()
                    replace(R.id.main_fragment_container_view, MapFragment.new(marker))
                    addToBackStack(null)
                }
            }
        }
    }

    override fun onShowMapClick() {
        showMapFragment()
    }

    private fun onARLoading() {
        if (currentTopFragment !is CameraFragment) return
        latestARState = CameraARState.LOADING
        binding.searchBarView.visibility = View.GONE
        binding.bottomNavigationView.visibility = View.GONE
        binding.nearMeFab.visibility = View.GONE
        binding.bottomSheetFragmentContainerView.visibility = View.GONE
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    private fun onAREnabled() {
        latestARState = CameraARState.ENABLED
        binding.searchBarView.visibility = View.VISIBLE
        binding.bottomNavigationView.visibility = View.VISIBLE
        if (viewModel.state.markers !is WithValue) {
            binding.nearMeFab.visibility = View.VISIBLE
        }
        binding.bottomSheetFragmentContainerView.visibility = View.VISIBLE
        bottomSheetBehavior.state = viewModel.state.lastLiveBottomSheetState
    }

    private fun onARDisabled() {
        if (shouldUpdateLastLiveBottomSheetState) {
            lifecycleScope.launch {
                viewModel.intent(MainIntent.LiveBottomSheetStateChanged(bottomSheetBehavior.state))
            }
        }
        latestARState = CameraARState.DISABLED

        binding.searchBarView.visibility = View.GONE
        binding.bottomNavigationView.visibility = View.GONE
        binding.nearMeFab.visibility = View.GONE
        binding.bottomSheetFragmentContainerView.visibility = View.GONE
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    private suspend fun signalTopFragmentChanged(onResume: Boolean) {
        viewModel.signal(
            MainSignal.TopFragmentChanged(
                cameraObscured = currentTopFragment !is CameraFragment,
                onResume
            )
        )
    }

    private fun initNavigationDrawer() {
        binding.drawerNavigationView.setNavigationItemSelectedListener {
            if (lifecycle.isResumed)
                when (it.itemId) {
                    R.id.drawer_nav_map -> showMapFragment()
                    R.id.drawer_nav_about -> {}
                    R.id.drawer_nav_settings -> {}
                }
            binding.mainDrawerLayout.closeDrawer(Gravity.LEFT)
            true
        }
    }

    private fun showMapFragment() {
        if (currentTopFragment is MapFragment) return
        fragmentTransaction {
            setSlideInFromBottom()
            replace(R.id.main_fragment_container_view, MapFragment())
            addToBackStack(null)
        }
    }

    private fun initSearch() {
        binding.searchBarView.setContent {
            ProvideWindowInsets {
                LookARoundTheme {
                    Column {
                        PlacesAutocompleteSearchBar()
                        val orientation = LocalConfiguration.current.orientation
                        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                            RecentSearchesList()
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun PlacesAutocompleteSearchBar() {
        val searchFocusedFlow = remember {
            viewModel.mapStates(MainState::searchFocused).distinctUntilChanged()
        }
        val cameraFragmentVisibleFlow = remember {
            viewModel
                .filterSignals<MainSignal.TopFragmentChanged>()
                .map { (cameraObscured) -> !cameraObscured }
                .distinctUntilChanged()
        }
        val searchFocused = searchFocusedFlow.collectAsState(initial = false)
        val cameraFragmentVisible =
            cameraFragmentVisibleFlow.collectAsState(initial = currentTopFragment is CameraFragment)
        val scope = rememberCoroutineScope()
        SearchBar(
            query = viewModel.state.autocompleteSearchQuery,
            focused = searchFocused.value,
            onBackPressedDispatcher = onBackPressedDispatcher,
            leadingUnfocused = {
                if (cameraFragmentVisible.value) {
                    IconButton(
                        onClick = {
                            with(binding.mainDrawerLayout) {
                                if (isDrawerOpen(Gravity.LEFT)) closeDrawer(Gravity.LEFT)
                                else openDrawer(Gravity.LEFT)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Menu,
                            tint = LookARoundTheme.colors.iconPrimary,
                            contentDescription = stringResource(R.string.navigation)
                        )
                    }
                } else {
                    IconButton(onClick = ::onBackPressed) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            tint = LookARoundTheme.colors.iconPrimary,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            },
            onSearchFocusChange = { focused ->
                scope.launch { viewModel.intent(MainIntent.SearchFocusChanged(focused)) }
            },
            onTextFieldValueChange = { textValue ->
                scope.launch { viewModel.intent(MainIntent.SearchQueryChanged(textValue.text)) }
            }
        )
    }

    @Composable
    private fun RecentSearchesList() {
        val scope = rememberCoroutineScope()
        val opaqueBackgroundFlow = remember {
            viewModel
                .listFragmentItemBackgroundUpdates
                .map { it == ListFragmentHost.ItemBackground.OPAQUE }
                .distinctUntilChanged()
        }
        val opaqueBackground =
            opaqueBackgroundFlow.collectAsState(
                initial = listItemBackground == ListFragmentHost.ItemBackground.OPAQUE
            )
        val itemBackgroundAlpha = if (opaqueBackground.value) .95f else .55f
        val backgroundGradientBrush = Brush.horizontalGradient(colors = listOf(Ocean2, Ocean0))
        RecentSearchesChipList(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            chipModifier =
                Modifier.clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = backgroundGradientBrush,
                        shape = RoundedCornerShape(20.dp),
                        alpha = itemBackgroundAlpha,
                    ),
            onMoreClicked = {
                binding.bottomNavigationView.selectedItemId = R.id.action_recent_searches
            }
        ) { (id, _, type) ->
            scope.launch {
                viewModel.intent(
                    when (type) {
                        SearchType.AROUND -> MainIntent.LoadSearchAroundResults(id)
                        SearchType.AUTOCOMPLETE -> MainIntent.LoadSearchAutocompleteResults(id)
                    }
                )
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
                .filterSignals(MainSignal.BottomSheetStateChanged::state)
                .debounce(500L)
                .onEach { sheetState ->
                    when (sheetState) {
                        BottomSheetBehavior.STATE_EXPANDED -> setSearchbarVisibility(View.GONE)
                        BottomSheetBehavior.STATE_HIDDEN -> {
                            if (viewsInteractionEnabled) setSearchbarVisibility(View.VISIBLE)
                            binding.bottomNavigationView.selectedItemId = R.id.action_unchecked
                        }
                    }
                }
                .launchIn(lifecycleScope)

            viewModel
                .onEachSignal<MainSignal.HideBottomSheet> {
                    state = BottomSheetBehavior.STATE_HIDDEN
                    binding.bottomNavigationView.selectedItemId = R.id.action_unchecked
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
                .onEach { isVisible ->
                    menu.findItem(R.id.action_place_map_list).isVisible = isVisible
                    updateBottomAppBarFabAlignment()
                }
                .launchIn(lifecycleScope)

            viewModel
                .recentSearchesBottomNavItemVisibilityUpdates
                .onEach { isVisible ->
                    menu.findItem(R.id.action_recent_searches).isVisible = isVisible
                    updateBottomAppBarFabAlignment()
                }
                .launchIn(lifecycleScope)

            if (currentTopFragment !is CameraFragment) visibility = View.VISIBLE
        }
    }

    private fun BottomNavigationView.updateBottomAppBarFabAlignment() {
        binding.nearMeFab.hide()
        val params = binding.nearMeFab.layoutParams as CoordinatorLayout.LayoutParams
        params.gravity =
            if (menu.iterator().asSequence().filter(MenuItem::isVisible).count() == 1) {
                Gravity.BOTTOM or Gravity.RIGHT
            } else {
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }
        binding.nearMeFab.layoutParams = params
        if (viewsInteractionEnabled && !viewModel.state.markers.hasValue) {
            binding.nearMeFab.show()
        }
    }

    private fun initNearMeFab() {
        binding.nearMeFab.setOnClickListener {
            lifecycleScope.launch { viewModel.intent(MainIntent.GetAttractions) }
        }

        viewModel
            .nearMeFabVisibilityUpdates
            .filter { viewsInteractionEnabled }
            .onEach { visible ->
                binding.nearMeFab.fadeSetVisibility(if (visible) View.VISIBLE else View.GONE)
            }
            .launchIn(lifecycleScope)
    }

    private fun onBottomSheetStateChanged(@BottomSheetBehavior.State sheetState: Int) {
        if (sheetState != BottomSheetBehavior.STATE_SETTLING && viewsInteractionEnabled) {
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
            .snackbarUpdates
            .onEach {
                when (it) {
                    is SnackbarUpdate.Show -> {
                        placesStatusLoadingSnackbar =
                            showPlacesLoadingStatusSnackbar(it.msgRes, it.length)
                    }
                    is SnackbarUpdate.Dismiss -> placesStatusLoadingSnackbar?.dismiss()
                }
            }
            .launchIn(lifecycleScope)
    }

    private fun showPlacesLoadingStatusSnackbar(
        @StringRes msgRes: Int,
        @BaseTransientBottomBar.Duration length: Int
    ): Snackbar =
        Snackbar.make(binding.mainCoordinatorLayout, getString(msgRes), length)
            .setAnchorView(binding.bottomNavigationView)
            .apply {
                addCallback(
                    object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                        override fun onShown(transientBottomBar: Snackbar?) {
                            signalSnackbarStatusChanged(isShowing = true)
                        }
                        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                            signalSnackbarStatusChanged(isShowing = false)
                        }
                    }
                )
                show()
            }

    private fun signalSnackbarStatusChanged(isShowing: Boolean) {
        lifecycleScope.launch { viewModel.signal(MainSignal.SnackbarStatusChanged(isShowing)) }
    }
}
