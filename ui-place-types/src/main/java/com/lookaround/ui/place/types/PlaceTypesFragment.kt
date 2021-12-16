package com.lookaround.ui.place.types

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.lookaround.core.android.ext.assistedActivityViewModel
import com.lookaround.core.android.model.Amenity
import com.lookaround.core.android.view.theme.LookARoundTheme
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.bottomSheetStateUpdates
import com.lookaround.ui.main.model.MainIntent
import com.lookaround.ui.place.types.composable.PlaceTypeGroupItem
import com.lookaround.ui.place.types.model.PlaceType
import com.lookaround.ui.place.types.model.PlaceTypeGroup
import com.lookaround.ui.search.composable.SearchBar
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

@FlowPreview
@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
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
            val bottomSheetSignalsFlow =
                mainViewModel
                    .bottomSheetStateUpdates
                    .onStart { emit(mainViewModel.state.lastLiveBottomSheetState) }
                    .distinctUntilChanged()
            setContent {
                ProvideWindowInsets {
                    LookARoundTheme {
                        val bottomSheetState =
                            bottomSheetSignalsFlow.collectAsState(
                                    initial = BottomSheetBehavior.STATE_HIDDEN
                                )
                                .value
                        LazyColumn {
                            if (bottomSheetState != BottomSheetBehavior.STATE_EXPANDED) {
                                item { Spacer(Modifier.height(112.dp)) }
                            } else {
                                stickyHeader {
                                    SearchBar(
                                        query = "",
                                        searchFocused = false,
                                        onBackPressedDispatcher =
                                            requireActivity().onBackPressedDispatcher,
                                        onSearchFocusChange = { focused -> },
                                        onTextValueChange = { textValue -> }
                                    )
                                }
                            }
                            itemsIndexed(
                                listOf(
                                    PlaceTypeGroup(
                                        name = "General",
                                        placeTypes =
                                            listOf(
                                                PlaceType(
                                                    wrapped = Amenity.PARKING,
                                                    imageUrl =
                                                        "https://source.unsplash.com/UsSdMZ78Q3E"
                                                ),
                                                PlaceType(
                                                    wrapped = Amenity.RESTAURANT,
                                                    imageUrl =
                                                        "https://source.unsplash.com/SfP1PtM9Qa8"
                                                ),
                                                PlaceType(
                                                    wrapped = Amenity.FUEL,
                                                    imageUrl =
                                                        "https://source.unsplash.com/_jk8KIyN_uA"
                                                ),
                                                PlaceType(
                                                    wrapped = Amenity.BANK,
                                                    imageUrl =
                                                        "https://source.unsplash.com/UsSdMZ78Q3E"
                                                )
                                            )
                                    ),
                                )
                            ) { index, group ->
                                PlaceTypeGroupItem(group, index) { placeType ->
                                    lifecycleScope.launch {
                                        mainViewModel.intent(MainIntent.GetPlacesOfType(placeType))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
}
