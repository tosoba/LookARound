package com.lookaround

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.lookaround.core.android.ar.listener.AREventsListener
import com.lookaround.core.android.ext.*
import com.lookaround.core.android.model.Marker
import com.lookaround.core.android.view.theme.LookARoundTheme
import com.lookaround.databinding.ActivityMainBinding
import com.lookaround.ui.camera.CameraFragment
import com.lookaround.ui.main.*
import com.lookaround.ui.main.model.MainIntent
import com.lookaround.ui.main.model.MainSignal
import com.lookaround.ui.map.MapFragment
import com.lookaround.ui.place.list.PlaceListFragment
import com.lookaround.ui.place.list.PlaceMapItemActionController
import com.lookaround.ui.place.types.PlaceTypesFragment
import com.lookaround.ui.search.SearchFragment
import com.lookaround.ui.search.composable.SearchBar
import com.lookaround.ui.search.composable.rememberSearchBarState
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.accompanist.insets.ProvideWindowInsets
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber

@ExperimentalFoundationApi
@ExperimentalStdlibApi
@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), AREventsListener, PlaceMapItemActionController {
    private val binding: ActivityMainBinding by viewBinding(ActivityMainBinding::bind)

    @Inject internal lateinit var viewModelFactory: MainViewModel.Factory
    private val viewModel: MainViewModel by assistedViewModel { viewModelFactory.create(it) }

    private val bottomSheetBehavior by
        lazy(LazyThreadSafetyMode.NONE) {
            BottomSheetBehavior.from(binding.bottomSheetFragmentContainerView)
        }
    private var latestARState: ARState? = null
    private var selectedBottomNavigationViewItemId: Int = R.id.action_unchecked

    private val bottomSheetFragments: Map<Class<out Fragment>, Fragment> by
        lazy(LazyThreadSafetyMode.NONE) {
            val bottomSheetFragmentClasses =
                listOf(PlaceTypesFragment::class.java, PlaceListFragment::class.java)
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

    private val onBottomNavItemSelectedListener by
        lazy(LazyThreadSafetyMode.NONE) {
            BottomNavigationView.OnNavigationItemSelectedListener { menuItem ->
                selectedBottomNavigationViewItemId = menuItem.itemId
                if (menuItem.itemId == R.id.action_unchecked) {
                    return@OnNavigationItemSelectedListener true
                }

                fragmentTransaction {
                    when (menuItem.itemId) {
                        R.id.action_place_types -> showBottomSheetFragment<PlaceTypesFragment>()
                        R.id.action_place_list -> showBottomSheetFragment<PlaceListFragment>()
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
            when (F::class.java) {
                PlaceTypesFragment::class.java ->
                    setCustomAnimations(R.anim.slide_in_from_left, R.anim.slide_out_to_left)
                PlaceListFragment::class.java ->
                    setCustomAnimations(R.anim.slide_in_from_right, R.anim.slide_out_to_right)
            }
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

        supportFragmentManager.addOnBackStackChangedListener { signalTopFragmentChanged(false) }

        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        window.statusBarColor = Color.TRANSPARENT

        initSearch()
        initBottomSheet()
        initBottomNavigationView(savedInstanceState)

        viewModel
            .locationUpdateFailureUpdates
            .onEach { Timber.tag("LOCATION").e("Failed to update location.") }
            .launchIn(lifecycleScope)

        viewModel
            .unableToLoadPlacesWithoutLocationSignals
            .onEach { Timber.tag("PLACES").e("Failed to load places without location.") }
            .launchIn(lifecycleScope)
    }

    override fun onResume() {
        super.onResume()
        signalTopFragmentChanged(true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(
            SavedStateKeys.BOTTOM_NAV_SELECTED_ITEM_ID.name,
            selectedBottomNavigationViewItemId
        )
    }

    override fun onBackPressed() {
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            if (viewModel.state.searchFocused) super.onBackPressed()
        } else {
            super.onBackPressed()
        }
    }

    override fun onAREnabled() {
        latestARState = ARState.ENABLED
        binding.searchBarView.visibility = View.VISIBLE
        binding.bottomNavigationView.visibility = View.VISIBLE
        bottomSheetBehavior.state = viewModel.state.lastLiveBottomSheetState
    }

    override fun onARLoading() {
        latestARState = ARState.LOADING
        binding.searchBarView.visibility = View.GONE
        binding.bottomNavigationView.visibility = View.GONE
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    override fun onARDisabled(anyPermissionDenied: Boolean, locationDisabled: Boolean) {
        if (latestARState == ARState.ENABLED) {
            lifecycleScope.launch {
                viewModel.intent(MainIntent.LiveBottomSheetStateChanged(bottomSheetBehavior.state))
            }
        }
        latestARState = ARState.DISABLED

        binding.searchBarView.visibility = View.GONE
        binding.bottomNavigationView.visibility = View.GONE
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    override fun onCameraTouch(targetVisibility: Int) {
        changeSearchbarVisibility(targetVisibility)
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
        viewModel
            .searchFragmentVisibilityUpdates
            .filter { lifecycle.isResumed }
            .onEach {
                if (it && currentTopFragment !is SearchFragment) {
                    showSearchFragment()
                } else if (!it && currentTopFragment is SearchFragment) {
                    supportFragmentManager.popBackStack()
                }
            }
            .launchIn(lifecycleScope)

        binding.searchBarView.setContent {
            ProvideWindowInsets {
                LookARoundTheme {
                    val (_, _, _, searchQuery, searchFocused) = viewModel.state
                    SearchBar(
                        state = rememberSearchBarState(searchQuery, searchFocused),
                        onSearchFocusChange = { focused ->
                            lifecycleScope.launchWhenResumed {
                                viewModel.intent(MainIntent.SearchFocusChanged(focused))
                            }
                        }
                    ) { textValue ->
                        lifecycleScope.launchWhenResumed {
                            viewModel.intent(MainIntent.SearchQueryChanged(textValue.text))
                        }
                    }
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

                    override fun onSlide(bottomSheet: View, slideOffset: Float) =
                        onBottomSheetSlideChanged(slideOffset)
                }
            )

            viewModel
                .bottomSheetStateUpdates
                .onEach { sheetState ->
                    when (sheetState) {
                        BottomSheetBehavior.STATE_EXPANDED ->
                            changeSearchbarVisibility(View.VISIBLE)
                        BottomSheetBehavior.STATE_HIDDEN ->
                            binding.bottomNavigationView.selectedItemId = R.id.action_unchecked
                    }
                }
                .launchIn(lifecycleScope)
        }
    }

    private fun initBottomNavigationView(savedInstanceState: Bundle?) {
        with(binding.bottomNavigationView) {
            savedInstanceState
                ?.getInt(SavedStateKeys.BOTTOM_NAV_SELECTED_ITEM_ID.name)
                ?.let(::selectedBottomNavigationViewItemId::set)
            selectedItemId = selectedBottomNavigationViewItemId
            setOnNavigationItemSelectedListener(onBottomNavItemSelectedListener)

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
        }
    }

    private fun showSearchFragment() {
        if (currentTopFragment is SearchFragment || !lifecycle.isResumed) return
        fragmentTransaction {
            setSlideInFromBottom()
            add(R.id.main_fragment_container, SearchFragment())
            addToBackStack(null)
        }
    }

    private fun onBottomSheetStateChanged(@BottomSheetBehavior.State sheetState: Int) {
        if (latestARState == ARState.ENABLED) {
            lifecycleScope.launch {
                viewModel.intent(MainIntent.LiveBottomSheetStateChanged(sheetState))
            }
        }
        when (sheetState) {
            BottomSheetBehavior.STATE_EXPANDED -> 1f
            BottomSheetBehavior.STATE_COLLAPSED -> 0f
            BottomSheetBehavior.STATE_HIDDEN -> -1f
            else -> null
        }?.let { slideOffset ->
            lifecycleScope.launch {
                viewModel.signal(MainSignal.BottomSheetSlideChanged(slideOffset))
            }
        }
        lifecycleScope.launch { viewModel.signal(MainSignal.BottomSheetStateChanged(sheetState)) }
    }

    private fun onBottomSheetSlideChanged(slideOffset: Float) {
        lifecycleScope.launch { viewModel.signal(MainSignal.BottomSheetSlideChanged(slideOffset)) }
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

    private enum class SavedStateKeys {
        BOTTOM_NAV_SELECTED_ITEM_ID
    }

    private enum class ARState {
        LOADING,
        ENABLED,
        DISABLED
    }
}
