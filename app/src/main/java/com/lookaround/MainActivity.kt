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
import com.lookaround.core.android.ext.slideChangeVisibility
import com.lookaround.core.android.view.theme.LookARoundTheme
import com.lookaround.databinding.ActivityMainBinding
import com.lookaround.ui.main.*
import com.lookaround.ui.main.model.MainIntent
import com.lookaround.ui.place.types.PlaceTypesView
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
class MainActivity : AppCompatActivity(), AREventsListener {
    private val binding: ActivityMainBinding by viewBinding(ActivityMainBinding::bind)

    @Inject internal lateinit var viewModelFactory: MainViewModel.Factory
    private val viewModel: MainViewModel by assistedViewModel { viewModelFactory.create(it) }

    private val bottomSheetBehavior by lazy(LazyThreadSafetyMode.NONE) {
        BottomSheetBehavior.from(binding.placeTypesView)
    }

    private val onBottomNavItemSelectedListener by lazy(LazyThreadSafetyMode.NONE) {
        BottomNavigationView.OnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_place_types -> {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
                R.id.action_place_list -> {}
            }
            true
        }
    }

    private val currentTopFragment: Fragment?
        get() = supportFragmentManager.findFragmentById(R.id.main_fragment_container)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding.initSearch()
        binding.initPlaceTypes()
        binding.bottomNavigationView.setOnNavigationItemSelectedListener(
            onBottomNavItemSelectedListener
        )

        viewModel
            .locationUpdateFailureUpdates
            .onEach { Timber.tag("LOCATION").e("Failed to update location.") }
            .launchIn(lifecycleScope)

        viewModel
            .unableToLoadPlacesWithoutLocationSignals
            .onEach { Timber.tag("PLACES").e("Failed to load places without location.") }
            .launchIn(lifecycleScope)
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

    private fun showSearchFragment() {
        if (currentTopFragment is SearchFragment || !lifecycle.isResumed) return
        with(supportFragmentManager.beginTransaction()) {
            setCustomAnimations(
                R.anim.slide_in_bottom,
                R.anim.slide_out_top,
                R.anim.slide_in_top,
                R.anim.slide_out_bottom
            )
            add(R.id.main_fragment_container, SearchFragment())
            addToBackStack(null)
            commit()
        }
    }

    private fun hideSearchFragment() {
        if (currentTopFragment !is SearchFragment || !lifecycle.isResumed) return
        supportFragmentManager.popBackStack()
    }

    private fun ActivityMainBinding.initPlaceTypes() {
        placeTypesView.setContent {
            LookARoundTheme {
                PlaceTypesView { placeType ->
                    lifecycleScope.launch { viewModel.intent(MainIntent.LoadPlaces(placeType)) }
                }
            }
        }

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

    override fun onAREnabled() {
        binding.searchBarView.visibility = View.VISIBLE
        binding.bottomNavigationView.visibility = View.VISIBLE
        onBottomSheetStateChanged(BottomSheetBehavior.STATE_COLLAPSED, false)
    }

    override fun onARLoading() {
        binding.searchBarView.visibility = View.GONE
        binding.bottomNavigationView.visibility = View.GONE
        onBottomSheetStateChanged(BottomSheetBehavior.STATE_HIDDEN, false)
    }

    override fun onARDisabled(anyPermissionDenied: Boolean, locationDisabled: Boolean) {
        binding.searchBarView.visibility = View.GONE
        binding.bottomNavigationView.visibility = View.GONE
        onBottomSheetStateChanged(BottomSheetBehavior.STATE_HIDDEN, false)
    }

    override fun onCameraTouch(targetVisibility: Int) {
        changeSearchbarVisibility(targetVisibility)
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

    private fun onBottomSheetStateChanged(
        @BottomSheetBehavior.State newState: Int,
        changedByUser: Boolean
    ) {
        lifecycleScope.launch {
            viewModel.intent(MainIntent.BottomSheetStateChanged(newState, changedByUser))
        }
    }
}
