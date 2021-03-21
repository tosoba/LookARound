package com.lookaround

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.lookaround.core.android.ext.assistedViewModel
import com.lookaround.databinding.ActivityMainBinding
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.model.MainSignal
import com.lookaround.ui.main.model.locationUpdateFailureUpdates
import com.lookaround.ui.place.types.PlaceTypesFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

        val bottomSheetFragment = PlaceTypesFragment()
        bottomSheetFragment.show(supportFragmentManager, PlaceTypesFragment::class.simpleName)

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
