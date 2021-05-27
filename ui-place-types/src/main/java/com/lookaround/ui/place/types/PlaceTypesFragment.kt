package com.lookaround.ui.place.types

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.lookaround.core.android.ext.assistedActivityViewModel
import com.lookaround.core.android.view.theme.LookARoundTheme
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.model.MainIntent
import com.lookaround.ui.place.types.composable.PlaceTypesView
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch

@FlowPreview
@ExperimentalCoroutinesApi
class PlaceTypesFragment : Fragment() {
    @Inject internal lateinit var mainViewModelFactory: MainViewModel.Factory
    private val mainViewModel: MainViewModel by assistedActivityViewModel {
        mainViewModelFactory.create(it)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        ComposeView(requireContext()).apply {
            setContent {
                LookARoundTheme {
                    PlaceTypesView { placeType ->
                        lifecycleScope.launch {
                            mainViewModel.intent(MainIntent.LoadPlaces(placeType))
                        }
                    }
                }
            }
        }
}
