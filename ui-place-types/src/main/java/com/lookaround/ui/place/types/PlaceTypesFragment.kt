package com.lookaround.ui.place.types

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@FlowPreview
@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
class PlaceTypesFragment : Fragment() {
    @Inject internal lateinit var mainViewModelFactory: MainViewModel.Factory
    private val mainViewModel: MainViewModel by assistedActivityViewModel {
        mainViewModelFactory.create(it)
    }

    private var searchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getString(SavedStateKey.SEARCH_QUERY.name)?.let(::searchQuery::set)
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

            val searchQueryFlow = MutableStateFlow(searchQuery)
            val placeTypeGroupsFlow =
                searchQueryFlow
                    .map { it.trim().lowercase() }
                    .distinctUntilChanged()
                    .map(::placeTypeGroupsMatching)
                    .distinctUntilChanged()

            setContent {
                ProvideWindowInsets {
                    LookARoundTheme {
                        val orientation = LocalConfiguration.current.orientation

                        val bottomSheetState =
                            bottomSheetSignalsFlow.collectAsState(
                                    initial = BottomSheetBehavior.STATE_HIDDEN
                                )
                                .value

                        val searchQuery = searchQueryFlow.collectAsState(initial = "")
                        val searchFocused = rememberSaveable { mutableStateOf(false) }
                        val placeTypeGroups =
                            placeTypeGroupsFlow.collectAsState(initial = allPlaceTypeGroups)

                        LazyColumn {
                            if (bottomSheetState != BottomSheetBehavior.STATE_EXPANDED) {
                                item { Spacer(Modifier.height(112.dp)) }
                            } else {
                                stickyHeader {
                                    SearchBar(
                                        query = searchQuery.value,
                                        focused = searchFocused.value,
                                        onBackPressedDispatcher =
                                            requireActivity().onBackPressedDispatcher,
                                        onSearchFocusChange = searchFocused::value::set,
                                        onTextFieldValueChange = {
                                            searchQueryFlow.value = it.text
                                            this@PlaceTypesFragment.searchQuery = it.text
                                        }
                                    )
                                }
                            }

                            if (placeTypeGroups.value.isNotEmpty()) {
                                val columns =
                                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) 4 else 2
                                itemsIndexed(placeTypeGroups.value) { index, group ->
                                    PlaceTypeGroupItem(
                                        group = group,
                                        index = index,
                                        columns = columns
                                    ) { placeType ->
                                        lifecycleScope.launch {
                                            mainViewModel.intent(
                                                MainIntent.GetPlacesOfType(placeType)
                                            )
                                        }
                                    }
                                }
                            } else {
                                item {
                                    Text(
                                        text = "No place types found.",
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(SavedStateKey.SEARCH_QUERY.name, searchQuery)
    }

    private fun placeTypeGroupsMatching(query: String): List<PlaceTypeGroup> =
        allPlaceTypeGroups
            .map {
                PlaceTypeGroup(
                    name = it.name,
                    placeTypes =
                        it.placeTypes.filter { placeType ->
                            placeType.wrapped.label.lowercase().contains(query) ||
                                placeType.wrapped.description.lowercase().contains(query)
                        }
                )
            }
            .filter { it.placeTypes.isNotEmpty() }

    private enum class SavedStateKey {
        SEARCH_QUERY
    }

    companion object {
        private val allPlaceTypeGroups =
            listOf(
                PlaceTypeGroup(
                    name = "Food & drinks",
                    placeTypes =
                        listOf(
                            PlaceType(
                                wrapped = Amenity.RESTAURANT,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Amenity.CAFE,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Amenity.FAST_FOOD,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Amenity.BAR,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Amenity.PUB,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Amenity.ICE_CREAM,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            )
                        )
                ),
                PlaceTypeGroup(
                    name = "Entertainment",
                    placeTypes =
                        listOf(
                            PlaceType(
                                wrapped = Amenity.CINEMA,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Amenity.THEATRE,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Amenity.NIGHTCLUB,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Amenity.ARTS_CENTRE,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Amenity.COMMUNITY_CENTRE,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Amenity.EVENTS_VENUE,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            )
                        )
                )
            )
    }
}
