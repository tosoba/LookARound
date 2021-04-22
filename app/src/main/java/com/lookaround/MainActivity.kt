package com.lookaround

import android.animation.LayoutTransition
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.lookaround.core.android.ar.listener.AREventsListener
import com.lookaround.core.android.ext.assistedViewModel
import com.lookaround.core.android.ext.toggleVisibility
import com.lookaround.core.android.view.theme.LookARoundTheme
import com.lookaround.databinding.ActivityMainBinding
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.model.MainIntent
import com.lookaround.ui.main.model.MainSignal
import com.lookaround.ui.main.model.bottomSheetStateUpdates
import com.lookaround.ui.main.model.locationUpdateFailureUpdates
import com.lookaround.ui.place.types.PlaceTypesView
import com.lookaround.ui.search.composable.Search
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.accompanist.insets.ProvideWindowInsets
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.filterIsInstance
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding.initSearch()
        binding.initPlaceTypes()

        viewModel
            .locationUpdateFailureUpdates
            .onEach { Timber.tag("LOCATION").e("Failed to update location.") }
            .launchIn(lifecycleScope)

        viewModel
            .signals
            .filterIsInstance<MainSignal.UnableToLoadPlacesWithoutLocation>()
            .onEach { Timber.tag("PLACES").e("Failed to load places without location.") }
            .launchIn(lifecycleScope)
    }

    private fun ActivityMainBinding.initSearch() {
        searchBarView.setContent { ProvideWindowInsets { LookARoundTheme { Search() } } }
        searchBarView.layoutTransition =
            LayoutTransition().apply { setAnimateParentHierarchy(false) }
    }

    private fun ActivityMainBinding.initPlaceTypes() {
        placeTypesView.setContent {
            LookARoundTheme {
                PlaceTypesView { placeType ->
                    lifecycleScope.launch { viewModel.intent(MainIntent.LoadPlaces(placeType)) }
                }
            }
        }

        with(BottomSheetBehavior.from(placeTypesView)) {
            addBottomSheetCallback(
                object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) =
                        onBottomSheetStateChanged(newState, true)

                    override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
                }
            )

            viewModel
                .bottomSheetStateUpdates
                .onEach { (sheetState, changedByUser) ->
                    state = sheetState
                    if (changedByUser) {
                        showPlaceTypesBtn.apply {
                            if (sheetState == BottomSheetBehavior.STATE_HIDDEN) show() else hide()
                        }
                    }
                }
                .launchIn(lifecycleScope)

            showPlaceTypesBtn.setOnClickListener { state = BottomSheetBehavior.STATE_HALF_EXPANDED }
        }
    }

    override fun onAREnabled() {
        binding.searchBarView.visibility = View.VISIBLE
        onBottomSheetStateChanged(BottomSheetBehavior.STATE_COLLAPSED, false)
    }

    override fun onARLoading() {
        binding.searchBarView.visibility = View.GONE
        onBottomSheetStateChanged(BottomSheetBehavior.STATE_HIDDEN, false)
    }

    override fun onARDisabled(anyPermissionDenied: Boolean, locationDisabled: Boolean) {
        binding.searchBarView.visibility = View.GONE
        onBottomSheetStateChanged(BottomSheetBehavior.STATE_HIDDEN, false)
    }

    override fun onCameraTouch() {
        binding.searchBarView.toggleVisibility()
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
