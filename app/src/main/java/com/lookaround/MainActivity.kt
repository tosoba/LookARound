package com.lookaround

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import biz.laenger.android.vpbs.BottomSheetUtils
import biz.laenger.android.vpbs.ViewPagerBottomSheetBehavior
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.lookaround.core.android.ar.listener.AREventsListener
import com.lookaround.core.android.ext.assistedViewModel
import com.lookaround.core.android.ext.isResumed
import com.lookaround.core.android.ext.setSlideInFromBottom
import com.lookaround.core.android.ext.slideChangeVisibility
import com.lookaround.core.android.model.Marker
import com.lookaround.core.android.view.theme.LookARoundTheme
import com.lookaround.databinding.ActivityMainBinding
import com.lookaround.ui.camera.CameraFragment
import com.lookaround.ui.main.*
import com.lookaround.ui.main.model.MainIntent
import com.lookaround.ui.map.MapFragment
import com.lookaround.ui.place.list.PlaceListFragment
import com.lookaround.ui.place.map.list.PlaceMapItemActionController
import com.lookaround.ui.place.map.list.PlaceMapListFragment
import com.lookaround.ui.place.types.PlaceTypesFragment
import com.lookaround.ui.search.SearchFragment
import com.lookaround.ui.search.composable.SearchBar
import com.lookaround.ui.search.composable.rememberSearchBarState
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.accompanist.insets.ProvideWindowInsets
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), AREventsListener, PlaceMapItemActionController {
    private val binding: ActivityMainBinding by viewBinding(ActivityMainBinding::bind)

    @Inject internal lateinit var viewModelFactory: MainViewModel.Factory
    private val viewModel: MainViewModel by assistedViewModel { viewModelFactory.create(it) }

    private val bottomSheetBehavior by
        lazy(LazyThreadSafetyMode.NONE) {
            ViewPagerBottomSheetBehavior.from(binding.bottomSheetViewPager)
        }
    private var lastLiveBottomSheetState: Int? = null

    private var latestARState: ARState? = null
    private var selectedBottomNavigationViewItemId: Int = R.id.action_place_types
    private val onBottomNavItemSelectedListener by
        lazy(LazyThreadSafetyMode.NONE) {
            BottomNavigationView.OnNavigationItemSelectedListener { menuItem ->
                selectedBottomNavigationViewItemId = menuItem.itemId
                binding.bottomSheetViewPager.currentItem =
                    when (menuItem.itemId) {
                        R.id.action_place_types -> 0
                        R.id.action_place_list -> 1
                        R.id.action_place_map_list -> 2
                        else -> throw IllegalArgumentException()
                    }
                if (latestARState == ARState.ENABLED) {
                    bottomSheetBehavior.state = ViewPagerBottomSheetBehavior.STATE_EXPANDED
                }
                true
            }
        }

    private val currentTopFragment: Fragment?
        get() = supportFragmentManager.findFragmentById(R.id.main_fragment_container)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        window.statusBarColor = Color.TRANSPARENT

        initSearch()
        initBottomSheet(savedInstanceState)
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        lastLiveBottomSheetState?.let {
            outState.putInt(SavedStateKeys.BOTTOM_SHEET_STATE.name, it)
        }
        outState.putInt(
            SavedStateKeys.BOTTOM_NAV_SELECTED_ITEM_ID.name,
            selectedBottomNavigationViewItemId
        )
    }

    override fun onAREnabled() {
        latestARState = ARState.ENABLED
        binding.searchBarView.visibility = View.VISIBLE
        binding.bottomNavigationView.visibility = View.VISIBLE
        onBottomSheetStateChanged(
            lastLiveBottomSheetState ?: BottomSheetBehavior.STATE_COLLAPSED,
            false
        )
    }

    override fun onARLoading() {
        latestARState = ARState.LOADING
        binding.searchBarView.visibility = View.GONE
        binding.bottomNavigationView.visibility = View.GONE
        onBottomSheetStateChanged(BottomSheetBehavior.STATE_HIDDEN, false)
    }

    override fun onARDisabled(anyPermissionDenied: Boolean, locationDisabled: Boolean) {
        latestARState = ARState.DISABLED
        binding.searchBarView.visibility = View.GONE
        binding.bottomNavigationView.visibility = View.GONE
        lastLiveBottomSheetState = viewModel.state.bottomSheetState.state
        onBottomSheetStateChanged(BottomSheetBehavior.STATE_HIDDEN, false)
    }

    override fun onCameraTouch(targetVisibility: Int) {
        changeSearchbarVisibility(targetVisibility)
    }

    override fun onPlaceMapItemClick(marker: Marker) {
        if (!lifecycle.isResumed) return
        when (val topFragment = currentTopFragment) {
            is MapFragment -> topFragment.updateMarker(marker)
            else -> {
                with(supportFragmentManager.beginTransaction()) {
                    setSlideInFromBottom()
                    add(R.id.main_fragment_container, MapFragment.new(marker))
                    addToBackStack(null)
                    commit()
                }
            }
        }
    }

    private fun initSearch() {
        viewModel
            .searchFocusUpdates
            .filter { it }
            .onEach { showSearchFragment() }
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
                        },
                        onTextValueChange = { textValue ->
                            lifecycleScope.launchWhenResumed {
                                viewModel.intent(MainIntent.SearchQueryChanged(textValue.text))
                            }
                        },
                        onBackPressed = {
                            if (currentTopFragment !is CameraFragment) {
                                supportFragmentManager.popBackStack()
                            }
                        }
                    )
                }
            }
        }
    }

    private fun initBottomSheet(savedInstanceState: Bundle?) {
        lastLiveBottomSheetState =
            savedInstanceState?.getInt(SavedStateKeys.BOTTOM_SHEET_STATE.name)

        with(binding.bottomSheetViewPager) {
            val fragments =
                arrayOf(PlaceTypesFragment(), PlaceListFragment(), PlaceMapListFragment())
            offscreenPageLimit = fragments.size - 1
            adapter =
                object :
                    FragmentPagerAdapter(
                        supportFragmentManager,
                        BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
                    ) {
                    override fun getCount(): Int = fragments.size
                    override fun getItem(position: Int): Fragment = fragments[position]
                }
            BottomSheetUtils.setupViewPager(this)
            addOnPageChangeListener(
                object : ViewPager.OnPageChangeListener {
                    override fun onPageScrolled(
                        position: Int,
                        positionOffset: Float,
                        positionOffsetPixels: Int
                    ) = Unit

                    override fun onPageSelected(position: Int) {
                        binding.bottomNavigationView.selectedItemId =
                            when (position) {
                                0 -> R.id.action_place_types
                                1 -> R.id.action_place_list
                                2 -> R.id.action_place_map_list
                                else -> throw IllegalArgumentException()
                            }
                        requestLayout()
                    }

                    override fun onPageScrollStateChanged(state: Int) = Unit
                }
            )
        }

        with(bottomSheetBehavior) {
            setBottomSheetCallback(
                object : ViewPagerBottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) =
                        onBottomSheetStateChanged(newState, true)

                    override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
                }
            )

            viewModel
                .bottomSheetStateUpdates
                .onEach { (sheetState, _) -> state = sheetState }
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

            viewModel
                .placesBottomNavItemVisibilityUpdates
                .onEach { isVisible -> menu.findItem(R.id.action_place_list).isVisible = isVisible }
                .launchIn(lifecycleScope)
        }
    }

    private fun showSearchFragment() {
        if (currentTopFragment is SearchFragment || !lifecycle.isResumed) return
        with(supportFragmentManager.beginTransaction()) {
            setSlideInFromBottom()
            add(R.id.main_fragment_container, SearchFragment())
            addToBackStack(null)
            commit()
        }
    }

    private fun onBottomSheetStateChanged(
        @BottomSheetBehavior.State newState: Int,
        changedByUser: Boolean
    ) {
        if (changedByUser) lastLiveBottomSheetState = newState
        lifecycleScope.launch {
            viewModel.intent(MainIntent.BottomSheetStateChanged(newState, changedByUser))
        }
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
        BOTTOM_SHEET_STATE,
        BOTTOM_NAV_SELECTED_ITEM_ID
    }

    private enum class ARState {
        LOADING,
        ENABLED,
        DISABLED
    }
}
