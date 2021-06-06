package com.lookaround.ui.place.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.lookaround.core.android.ext.assistedActivityViewModel
import com.lookaround.core.android.model.WithValue
import com.lookaround.core.android.view.theme.LookARoundTheme
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.locationReadyUpdates
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@FlowPreview
@ExperimentalCoroutinesApi
class PlaceListFragment : Fragment() {
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
                    val markers = mainViewModel.states.collectAsState().value.markers
                    if (markers is WithValue) {
                        PlacesList(
                            markers.value.items,
                            locationFlow = mainViewModel.locationReadyUpdates
                        )
                    }
                }
            }
        }
}
