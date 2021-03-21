package com.lookaround

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.lookaround.core.android.ext.assistedViewModel
import com.lookaround.databinding.ActivityMainBinding
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.model.MainIntent
import com.lookaround.ui.main.model.MainSignal
import com.lookaround.ui.main.model.locationUpdateFailureUpdates
import com.lookaround.ui.place.types.PlaceTypesView
import dagger.hilt.android.AndroidEntryPoint
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
class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by viewBinding(ActivityMainBinding::bind)

    @Inject internal lateinit var viewModelFactory: MainViewModel.Factory
    private val viewModel: MainViewModel by assistedViewModel { viewModelFactory.create(it) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding.placeTypesView.setContent {
            PlaceTypesView {
                lifecycleScope.launch { viewModel.intent(MainIntent.LoadPlaces(it)) }
            }
        }

        val bottomSheetBehavior = BottomSheetBehavior.from(binding.placeTypesView)
        bottomSheetBehavior.addBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    when (newState) {
                        BottomSheetBehavior.STATE_COLLAPSED -> {}
                        BottomSheetBehavior.STATE_DRAGGING -> {}
                        BottomSheetBehavior.STATE_EXPANDED -> {}
                        BottomSheetBehavior.STATE_HALF_EXPANDED -> {}
                        BottomSheetBehavior.STATE_HIDDEN -> {}
                        BottomSheetBehavior.STATE_SETTLING -> {}
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {}
            }
        )
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

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
}
