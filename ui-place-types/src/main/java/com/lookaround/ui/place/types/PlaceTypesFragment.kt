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
import com.lookaround.core.model.Amenity
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.model.MainIntent
import com.lookaround.ui.place.types.model.PlaceType
import com.lookaround.ui.place.types.model.PlaceTypeGroup
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import dev.chrisbanes.accompanist.insets.ProvideWindowInsets
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
@WithFragmentBindings
class PlaceTypesFragment : Fragment() {
    private val placeTypeGroups =
        listOf(
            PlaceTypeGroup(
                name = "General",
                placeTypes =
                    listOf(
                        PlaceType(
                            wrapped = Amenity.PARKING,
                            imageUrl = "https://source.unsplash.com/UsSdMZ78Q3E"
                        ),
                        PlaceType(
                            wrapped = Amenity.RESTAURANT,
                            imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                        ),
                        PlaceType(
                            wrapped = Amenity.FUEL,
                            imageUrl = "https://source.unsplash.com/_jk8KIyN_uA"
                        ),
                        PlaceType(
                            wrapped = Amenity.BANK,
                            imageUrl = "https://source.unsplash.com/UsSdMZ78Q3E"
                        )
                    )
            ),
        )

    @Inject internal lateinit var viewModelFactory: MainViewModel.Factory
    private val viewModel: MainViewModel by assistedActivityViewModel {
        viewModelFactory.create(it)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        ComposeView(requireContext()).apply {
            setContent {
                ProvideWindowInsets {
                    LookARoundTheme {
                        PlaceTypes(placeTypeGroups) {
                            lifecycleScope.launch {
                                viewModel.intent(MainIntent.LoadPlaces(it.wrapped))
                            }
                        }
                    }
                }
            }
        }
}
