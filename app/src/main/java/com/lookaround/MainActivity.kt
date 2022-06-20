package com.lookaround

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.iterator
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.imxie.exvpbs.ViewPagerBottomSheetBehavior
import com.lookaround.core.android.ext.*
import com.lookaround.core.android.model.WithValue
import com.lookaround.core.android.model.hasValue
import com.lookaround.core.android.view.composable.SearchBar
import com.lookaround.core.android.view.theme.LookARoundTheme
import com.lookaround.core.android.view.viewpager.DiffUtilFragmentStateAdapter
import com.lookaround.core.model.SearchType
import com.lookaround.databinding.ActivityMainBinding
import com.lookaround.ui.about.AboutFragment
import com.lookaround.ui.camera.CameraFragment
import com.lookaround.ui.camera.model.CameraARState
import com.lookaround.ui.main.*
import com.lookaround.ui.main.model.MainIntent
import com.lookaround.ui.main.model.MainSignal
import com.lookaround.ui.main.model.MainState
import com.lookaround.ui.map.MapFragment
import com.lookaround.ui.place.PlaceFragment
import com.lookaround.ui.place.list.PlaceListFragment
import com.lookaround.ui.recent.searches.composable.RecentSearchesChipList
import com.lookaround.ui.settings.SettingsFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent.setEventListener
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener
import timber.log.Timber

@AndroidEntryPoint
@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
@ExperimentalStdlibApi
@FlowPreview
@SuppressLint("RtlHardcoded")
class MainActivity : AppCompatActivity(R.layout.activity_main) {
    private val binding: ActivityMainBinding by viewBinding(ActivityMainBinding::bind)

    private val viewModel: MainViewModel by viewModels()

    private var latestARState: CameraARState = CameraARState.INITIAL

    private val bottomSheetBehavior by
        lazy(LazyThreadSafetyMode.NONE) {
            ViewPagerBottomSheetBehavior.from(binding.bottomSheetViewPager)
        }
    private val bottomSheetViewPagerAdapter by
        lazy(LazyThreadSafetyMode.NONE) { DiffUtilFragmentStateAdapter(this@MainActivity) }

    private val placeListBottomSheetBehavior by
        lazy(LazyThreadSafetyMode.NONE) {
            BottomSheetBehavior.from(binding.placeListFragmentContainerView)
        }

    private val shouldUpdateLastLiveBottomSheetState: Boolean
        get() {
            val bottomSheetState = bottomSheetBehavior.state
            return (bottomSheetState == ViewPagerBottomSheetBehavior.STATE_EXPANDED ||
                bottomSheetState == ViewPagerBottomSheetBehavior.STATE_HIDDEN) &&
                viewsInteractionEnabled
        }

    private val viewsInteractionEnabled: Boolean
        get() =
            (currentTopFragment is CameraFragment && latestARState == CameraARState.ENABLED) ||
                currentTopFragment is MapFragment

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

                hideKeyboard()

                binding.bottomSheetViewPager.setCurrentItem(
                    bottomSheetViewPagerAdapter.fragmentFactories.indexOf(
                        when (menuItem.itemId) {
                            R.id.action_place_categories -> MainFragmentFactory.PLACE_CATEGORIES
                            R.id.action_place_map_list -> MainFragmentFactory.PLACE_MAP_LIST
                            R.id.action_recent_searches -> MainFragmentFactory.RECENT_SEARCHES
                            else -> throw IllegalArgumentException()
                        }
                    ),
                    bottomSheetBehavior.state == ViewPagerBottomSheetBehavior.STATE_EXPANDED
                )

                if (viewsInteractionEnabled) {
                    binding.placeListFragmentContainerView.visibility = View.GONE
                    placeListBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                    binding.bottomSheetViewPager.visibility = View.VISIBLE
                    bottomSheetBehavior.state = ViewPagerBottomSheetBehavior.STATE_EXPANDED
                    if (currentTopFragment is MapFragment) {
                        lifecycleScope.launch {
                            viewModel.signal(
                                MainSignal.BottomSheetStateChanged(
                                    ViewPagerBottomSheetBehavior.STATE_EXPANDED
                                )
                            )
                        }
                    }
                }

                true
            }
        }

    private val currentTopFragment: Fragment?
        get() = supportFragmentManager.findFragmentById(R.id.main_fragment_container_view)

    private val placeListFragment: PlaceListFragment?
        get() =
            supportFragmentManager.findFragmentById(R.id.place_list_fragment_container_view)
                as? PlaceListFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFullScreenWithTransparentBars()

        supportFragmentManager.addOnBackStackChangedListener {
            lifecycleScope.launchWhenResumed {
                signalTopFragmentChanged()
                viewModel.signal(MainSignal.BottomSheetStateChanged(bottomSheetBehavior.state))
                when (currentTopFragment) {
                    is MapFragment -> {
                        setSearchbarVisibility(View.VISIBLE)
                        binding.bottomNavigationView.fadeSetVisibility(View.VISIBLE)
                    }
                    is PlaceFragment -> {
                        setSearchbarVisibility(View.GONE)
                        binding.bottomNavigationView.fadeSetVisibility(View.GONE)
                    }
                }
            }
        }

        initNavigationDrawer()
        initSearch()
        initBottomSheet()
        initPlaceListBottomSheet(savedInstanceState)
        initBottomNavigationView()
        initNearMeFab()

        viewModel.onEachSignal<MainSignal.ARLoading> { onARLoading() }.launchIn(lifecycleScope)
        viewModel.onEachSignal<MainSignal.AREnabled> { onAREnabled() }.launchIn(lifecycleScope)
        viewModel.onEachSignal<MainSignal.ARDisabled> { onARDisabled() }.launchIn(lifecycleScope)

        viewModel
            .filterSignals(MainSignal.ToggleSearchBarVisibility::targetVisibility)
            .filter { viewsInteractionEnabled }
            .onEach {
                if (it == View.GONE) hideKeyboard()
                setSearchbarVisibility(it)
            }
            .launchIn(lifecycleScope)

        viewModel.locationUpdateFailureUpdates
            .onEach {
                Timber.tag("LOCATION").e(it?.message ?: "Unknown location update failure occurred.")
            }
            .launchIn(lifecycleScope)

        lifecycleScope.launchWhenResumed {
            viewModel.filterSignals<MainSignal.ShowPlaceFragment>().collect { (marker) ->
                mainFragmentContainerViewTransaction(FragmentTransactionType.ADD) {
                    PlaceFragment.new(marker)
                }
            }
        }

        lifecycleScope.launchWhenResumed {
            viewModel.filterSignals<MainSignal.ShowMapFragment>().collect { (marker) ->
                if (marker != null) {
                    when (val topFragment = currentTopFragment) {
                        is MapFragment -> topFragment.updateCurrentMarker(marker)
                        else -> mainFragmentContainerViewTransaction { MapFragment.new(marker) }
                    }
                } else {
                    mainFragmentContainerViewTransaction<MapFragment>()
                }
            }
        }

        lifecycleScope.launchWhenResumed {
            viewModel
                .filterSignals<MainSignal.MapFragmentResumed>()
                .filter { currentTopFragment is MapFragment }
                .collect {
                    binding.bottomNavigationView.visibility = View.VISIBLE
                    if (!viewModel.state.markers.hasValue) {
                        binding.nearMeFab.visibility = View.VISIBLE
                    }
                    binding.searchBarView.visibility = View.VISIBLE
                }
        }

        launchPlacesLoadingSnackbarUpdates()
    }

    override fun onPause() {
        if (bottomSheetBehavior.state == ViewPagerBottomSheetBehavior.STATE_HIDDEN) {
            binding.bottomNavigationView.visibility = View.GONE
        }
        binding.nearMeFab.visibility = View.GONE
        binding.searchBarView.visibility = View.GONE
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launchWhenResumed { signalTopFragmentChanged() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(
            SavedStateKeys.PLACE_LIST_BOTTOM_SHEET_STATE.name,
            placeListBottomSheetBehavior.state
        )
    }

    override fun onBackPressed() {
        when {
            binding.mainDrawerLayout.isDrawerOpen(Gravity.LEFT) -> {
                binding.mainDrawerLayout.closeDrawer(Gravity.LEFT)
            }
            placeListBottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED -> {
                placeListBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
            bottomSheetBehavior.state == ViewPagerBottomSheetBehavior.STATE_EXPANDED -> {
                bottomSheetBehavior.state = ViewPagerBottomSheetBehavior.STATE_HIDDEN
            }
            else -> super.onBackPressed()
        }
    }

    private fun onARLoading() {
        if (currentTopFragment !is CameraFragment) return
        latestARState = CameraARState.LOADING
        hideViewsOnARNotEnabled()
    }

    private fun onAREnabled() {
        latestARState = CameraARState.ENABLED
        showViewsOnAREnabled()
    }

    private fun onARDisabled() {
        if (shouldUpdateLastLiveBottomSheetState) {
            lifecycleScope.launch {
                viewModel.intent(MainIntent.LiveBottomSheetStateChanged(bottomSheetBehavior.state))
            }
        }
        latestARState = CameraARState.DISABLED

        hideViewsOnARNotEnabled()
    }

    private fun showViewsOnAREnabled() {
        binding.bottomNavigationView.visibility = View.VISIBLE
        val lastLiveBottomSheetState = viewModel.state.lastLiveBottomSheetState
        if (lastLiveBottomSheetState != ViewPagerBottomSheetBehavior.STATE_EXPANDED) {
            binding.searchBarView.visibility = View.VISIBLE
            if (viewModel.state.markers !is WithValue) {
                binding.nearMeFab.visibility = View.VISIBLE
            }
        }
        binding.bottomSheetViewPager.visibility = View.VISIBLE
        bottomSheetBehavior.state = lastLiveBottomSheetState
    }

    private fun hideViewsOnARNotEnabled() {
        binding.searchBarView.visibility = View.GONE
        binding.bottomNavigationView.visibility = View.GONE
        binding.nearMeFab.visibility = View.GONE
        binding.bottomSheetViewPager.visibility = View.GONE
        bottomSheetBehavior.state = ViewPagerBottomSheetBehavior.STATE_HIDDEN
    }

    private suspend fun signalTopFragmentChanged() {
        currentTopFragment?.let {
            viewModel.signal(MainSignal.TopFragmentChanged(fragmentClass = it::class.java))
        }
    }

    private fun initNavigationDrawer() {
        binding.drawerNavigationView.setNavigationItemSelectedListener {
            if (lifecycle.isResumed)
                when (it.itemId) {
                    R.id.drawer_nav_map -> {
                        mainFragmentContainerViewTransaction<MapFragment>()
                    }
                    R.id.drawer_nav_about -> {
                        hideViewsOnARNotEnabled()
                        mainFragmentContainerViewTransaction<AboutFragment>()
                    }
                    R.id.drawer_nav_settings -> {
                        hideViewsOnARNotEnabled()
                        mainFragmentContainerViewTransaction<SettingsFragment>()
                    }
                }
            binding.mainDrawerLayout.closeDrawer(Gravity.LEFT)
            true
        }

        fun onDrawerToggled(open: Boolean) {
            lifecycleScope.launch { viewModel.intent(MainIntent.DrawerToggled(open)) }
        }

        val initiallyOpen = binding.mainDrawerLayout.isDrawerOpen(binding.drawerNavigationView)
        onDrawerToggled(initiallyOpen)

        viewModel
            .filterSignals(MainSignal.TopFragmentChanged::fragmentClass)
            .distinctUntilChanged()
            .onEach { fragmentClass ->
                binding.mainDrawerLayout.setDrawerLockMode(
                    if (!fragmentClass.isAssignableFrom(CameraFragment::class.java)) {
                        DrawerLayout.LOCK_MODE_LOCKED_CLOSED
                    } else {
                        DrawerLayout.LOCK_MODE_UNLOCKED
                    }
                )
            }
            .launchIn(lifecycleScope)

        binding.mainDrawerLayout.addDrawerListener(
            object : DrawerLayout.DrawerListener {
                private var lastState: Int = DrawerLayout.STATE_IDLE
                private var open: Boolean = initiallyOpen

                init {
                    viewModel.signals
                        .filterIsInstance<MainSignal.BlurBackgroundUpdated>()
                        .filter {
                            lifecycle.isResumed && !open && lastState == DrawerLayout.STATE_IDLE
                        }
                        .onEach { (drawable) ->
                            binding.drawerNavigationView.background =
                                LayerDrawable(
                                    arrayOf(
                                        drawable,
                                        ContextCompat.getDrawable(
                                            this@MainActivity,
                                            R.drawable.top_dark_gradient
                                        )
                                    )
                                )
                        }
                        .launchIn(lifecycleScope)
                }

                override fun onDrawerOpened(drawerView: View) {
                    open = true
                    onDrawerToggled(true)
                }

                override fun onDrawerClosed(drawerView: View) {
                    open = false
                    onDrawerToggled(false)
                }

                override fun onDrawerStateChanged(newState: Int) {
                    lastState = newState
                }

                override fun onDrawerSlide(drawerView: View, slideOffset: Float) = Unit
            }
        )
    }

    private inline fun <reified F : Fragment> mainFragmentContainerViewTransaction(
        transactionType: FragmentTransactionType = FragmentTransactionType.REPLACE,
        crossinline factory: () -> F = F::class.java::newInstance,
    ) {
        if (currentTopFragment is F) return
        fragmentTransaction {
            setFadeAnimations()
            when (transactionType) {
                FragmentTransactionType.ADD -> add(R.id.main_fragment_container_view, factory())
                FragmentTransactionType.REPLACE ->
                    replace(R.id.main_fragment_container_view, factory())
            }
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
                .filterSignals(MainSignal.TopFragmentChanged::fragmentClass)
                .map { it.isAssignableFrom(CameraFragment::class.java) }
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
        RecentSearchesChipList(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
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
        bottomSheetBehavior.setBottomSheetCallback(
            object : ViewPagerBottomSheetBehavior.BottomSheetCallback() {
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
                    ViewPagerBottomSheetBehavior.STATE_EXPANDED -> {
                        setSearchbarVisibility(View.GONE)
                        binding.bottomSheetViewPager.visibility = View.VISIBLE
                    }
                    ViewPagerBottomSheetBehavior.STATE_HIDDEN -> {
                        if (viewsInteractionEnabled) setSearchbarVisibility(View.VISIBLE)
                        binding.bottomNavigationView.selectedItemId = R.id.action_unchecked
                    }
                }
            }
            .launchIn(lifecycleScope)

        viewModel
            .onEachSignal<MainSignal.HideBottomSheet> {
                bottomSheetBehavior.state = ViewPagerBottomSheetBehavior.STATE_HIDDEN
                binding.bottomNavigationView.selectedItemId = R.id.action_unchecked
            }
            .launchIn(lifecycleScope)

        with(binding.bottomSheetViewPager) {
            offscreenPageLimit = MainFragmentFactory.values().size - 1
            adapter = bottomSheetViewPagerAdapter
            registerOnPageChangeCallback(
                object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        if (bottomSheetBehavior.state ==
                                ViewPagerBottomSheetBehavior.STATE_HIDDEN ||
                                binding.bottomNavigationView.selectedItemId == R.id.action_unchecked
                        ) {
                            return
                        }

                        hideKeyboard()

                        val selectedItemId =
                            when (bottomSheetViewPagerAdapter.fragmentFactories[position]) {
                                MainFragmentFactory.PLACE_CATEGORIES -> R.id.action_place_categories
                                MainFragmentFactory.PLACE_MAP_LIST -> R.id.action_place_map_list
                                MainFragmentFactory.RECENT_SEARCHES -> R.id.action_recent_searches
                                else -> throw IllegalArgumentException()
                            }
                        if (binding.bottomNavigationView.selectedItemId != selectedItemId) {
                            binding.bottomNavigationView.selectedItemId = selectedItemId
                        }
                    }
                }
            )
        }

        combine(
                viewModel.placesBottomNavItemVisibilityUpdates,
                viewModel.recentSearchesBottomNavItemVisibilityUpdates
            ) { placesVisible, recentSearchesVisible ->
                val factories = mutableListOf(MainFragmentFactory.PLACE_CATEGORIES)
                if (placesVisible) factories.add(MainFragmentFactory.PLACE_MAP_LIST)
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
    }

    private fun initPlaceListBottomSheet(savedInstanceState: Bundle?) {
        savedInstanceState?.getInt(SavedStateKeys.PLACE_LIST_BOTTOM_SHEET_STATE.name)?.let {
            placeListBottomSheetBehavior.state = it
            if (it != BottomSheetBehavior.STATE_EXPANDED) return@let
            binding.placeListFragmentContainerView.visibility = View.VISIBLE
        }

        viewModel
            .onEachSignal(MainSignal.TopFragmentChanged::fragmentClass) { fragmentClass ->
                if (!fragmentClass.isAssignableFrom(MapFragment::class.java)) {
                    binding.placeListFragmentContainerView.visibility = View.GONE
                    placeListBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                }
            }
            .launchIn(lifecycleScope)

        viewModel
            .filterSignals(MainSignal.ShowPlaceInBottomSheet::id)
            .onEach { id ->
                binding.bottomSheetViewPager.visibility = View.GONE
                bottomSheetBehavior.state = ViewPagerBottomSheetBehavior.STATE_HIDDEN
                binding.placeListFragmentContainerView.visibility = View.VISIBLE
                placeListBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                placeListFragment?.scrollToPlace(id)
            }
            .launchIn(lifecycleScope)

        viewModel
            .filterSignals<MainSignal.HidePlacesListBottomSheet>()
            .onEach { placeListBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN }
            .launchIn(lifecycleScope)

        placeListBottomSheetBehavior.addBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    lifecycleScope.launch {
                        viewModel.signal(MainSignal.PlaceListBottomSheetStateChanged(newState))
                    }
                }
                override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
            }
        )
    }

    private fun initBottomNavigationView() {
        with(binding.bottomNavigationView) {
            selectedItemId = viewModel.state.selectedBottomNavigationViewItemId
            setOnItemSelectedListener(onBottomNavItemSelectedListener)

            viewModel.placesBottomNavItemVisibilityUpdates
                .onEach { isVisible ->
                    menu.findItem(R.id.action_place_map_list).isVisible = isVisible
                    updateBottomAppBarFabAlignment()
                }
                .launchIn(lifecycleScope)

            viewModel.recentSearchesBottomNavItemVisibilityUpdates
                .onEach { isVisible ->
                    menu.findItem(R.id.action_recent_searches).isVisible = isVisible
                    updateBottomAppBarFabAlignment()
                }
                .launchIn(lifecycleScope)

            viewModel
                .filterSignals(MainSignal.TopFragmentChanged::fragmentClass)
                .distinctUntilChanged()
                .onEach { fragmentClass ->
                    if (fragmentClass.isAssignableFrom(CameraFragment::class.java)) {
                        setBackgroundColor(Color.TRANSPARENT)
                    } else {
                        background =
                            ContextCompat.getDrawable(
                                this@MainActivity,
                                R.drawable.rounded_view_background
                            )
                    }
                }
                .launchIn(lifecycleScope)

            setEventListener(
                this@MainActivity,
                KeyboardVisibilityEventListener { open ->
                    if (currentTopFragment !is CameraFragment) {
                        return@KeyboardVisibilityEventListener
                    }

                    if (open) {
                        alpha = 0f
                        background =
                            ContextCompat.getDrawable(
                                this@MainActivity,
                                R.drawable.rounded_view_background
                            )
                        animate().alpha(1f).duration = 250L
                    } else {
                        alpha = 0f
                        setBackgroundColor(Color.TRANSPARENT)
                        animate().alpha(1f).duration = 250L
                    }
                }
            )

            if (currentTopFragment is MapFragment) visibility = View.VISIBLE
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

        viewModel.nearMeFabVisibilityUpdates
            .filter { viewsInteractionEnabled }
            .onEach { visible ->
                binding.nearMeFab.fadeSetVisibility(if (visible) View.VISIBLE else View.GONE)
            }
            .launchIn(lifecycleScope)
    }

    private fun onBottomSheetStateChanged(@ViewPagerBottomSheetBehavior.State sheetState: Int) {
        if (shouldUpdateLastLiveBottomSheetState) {
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
        viewModel.snackbarUpdates
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

    private enum class SavedStateKeys {
        PLACE_LIST_BOTTOM_SHEET_STATE
    }

    private enum class FragmentTransactionType {
        ADD,
        REPLACE
    }
}
