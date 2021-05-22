package com.lookaround

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
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
import com.lookaround.ui.place.map.list.PlaceMapItemActionController
import com.lookaround.ui.search.SearchFragment
import com.lookaround.ui.search.composable.SearchBar
import com.lookaround.ui.search.composable.rememberSearchBarState
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.accompanist.insets.ProvideWindowInsets
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), AREventsListener, PlaceMapItemActionController {
    private val binding: ActivityMainBinding by viewBinding(ActivityMainBinding::bind)

    @Inject internal lateinit var viewModelFactory: MainViewModel.Factory
    private val viewModel: MainViewModel by assistedViewModel { viewModelFactory.create(it) }

    private val bottomSheetBehavior by lazy(LazyThreadSafetyMode.NONE) {
        BottomSheetBehavior.from(binding.bottomSheetFragmentContainer)
    }
    private var lastLiveBottomSheetState: Int? = null

    private var selectedBottomNavigationViewItemId: Int = bottomSheetFragmentIds.first()
    private val onBottomNavItemSelectedListener by lazy(LazyThreadSafetyMode.NONE) {
        fun changeBottomSheetFragmentsVisibility(visibleFragmentId: Int) {
            bottomSheetFragmentIds.forEach { id ->
                findViewById<View>(id).visibility =
                    if (id == visibleFragmentId) View.VISIBLE else View.GONE
            }
        }

        fun bottomSheetVisibleFragmentId(navigationItemId: Int): Int =
            when (navigationItemId) {
                R.id.action_place_types -> R.id.place_types_fragment_view
                R.id.action_place_list -> R.id.place_list_fragment_view
                R.id.action_place_map_list -> R.id.place_map_list_fragment_view
                else -> throw IllegalArgumentException()
            }

        BottomNavigationView.OnNavigationItemSelectedListener { menuItem ->
            selectedBottomNavigationViewItemId = menuItem.itemId
            changeBottomSheetFragmentsVisibility(
                visibleFragmentId = bottomSheetVisibleFragmentId(menuItem.itemId)
            )
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            true
        }
    }

    private val currentTopFragment: Fragment?
        get() = supportFragmentManager.findFragmentById(R.id.main_fragment_container)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding.initSearch()
        initBottomSheet(savedInstanceState)
        binding.initBottomNavigationView(savedInstanceState)

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
        binding.searchBarView.visibility = View.VISIBLE
        binding.bottomNavigationView.visibility = View.VISIBLE
        onBottomSheetStateChanged(
            lastLiveBottomSheetState ?: BottomSheetBehavior.STATE_COLLAPSED,
            false
        )
    }

    override fun onARLoading() {
        binding.searchBarView.visibility = View.GONE
        binding.bottomNavigationView.visibility = View.GONE
        onBottomSheetStateChanged(BottomSheetBehavior.STATE_HIDDEN, false)
    }

    override fun onARDisabled(anyPermissionDenied: Boolean, locationDisabled: Boolean) {
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

    private fun ActivityMainBinding.initSearch() {
        viewModel
            .searchFocusUpdates
            .onEach { focused -> if (focused) showSearchFragment() else hideSearchFragment() }
            .launchIn(lifecycleScope)

        searchBarView.setContent {
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
        with(bottomSheetBehavior) {
            addBottomSheetCallback(
                object : BottomSheetBehavior.BottomSheetCallback() {
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

    private fun ActivityMainBinding.initBottomNavigationView(savedInstanceState: Bundle?) {
        bottomNavigationView.setOnNavigationItemSelectedListener(onBottomNavItemSelectedListener)
        savedInstanceState
            ?.getInt(SavedStateKeys.BOTTOM_NAV_SELECTED_ITEM_ID.name)
            ?.let(::selectedBottomNavigationViewItemId::set)
        bottomNavigationView.selectedItemId = selectedBottomNavigationViewItemId
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

    private fun hideSearchFragment() {
        if (currentTopFragment !is SearchFragment || !lifecycle.isResumed) return
        supportFragmentManager.popBackStack()
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

    companion object {
        private val bottomSheetFragmentIds: Array<Int> =
            arrayOf(
                R.id.place_types_fragment_view,
                R.id.place_list_fragment_view,
                R.id.place_map_list_fragment_view
            )
    }
}
