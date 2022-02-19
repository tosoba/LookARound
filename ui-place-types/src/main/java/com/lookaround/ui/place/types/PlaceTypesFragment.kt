package com.lookaround.ui.place.types

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.lookaround.core.android.model.Amenity
import com.lookaround.core.android.model.Leisure
import com.lookaround.core.android.model.Shop
import com.lookaround.core.android.model.Tourism
import com.lookaround.core.android.view.theme.LookARoundTheme
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.model.MainIntent
import com.lookaround.ui.main.model.MainSignal
import com.lookaround.ui.place.types.composable.PlaceTypeGroupItem
import com.lookaround.ui.place.types.databinding.FragmentPlaceTypesBinding
import com.lookaround.ui.place.types.model.PlaceType
import com.lookaround.ui.place.types.model.PlaceTypeGroup
import com.lookaround.ui.search.composable.SearchBar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

@FlowPreview
@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
class PlaceTypesFragment : Fragment(R.layout.fragment_place_types) {
    private val binding: FragmentPlaceTypesBinding by viewBinding(FragmentPlaceTypesBinding::bind)

    private val mainViewModel: MainViewModel by activityViewModels()

    private var searchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getString(SavedStateKey.SEARCH_QUERY.name)?.let(::searchQuery::set)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val bottomSheetSignalsFlow =
            mainViewModel
                .filterSignals(MainSignal.BottomSheetStateChanged::state)
                .onStart { emit(mainViewModel.state.lastLiveBottomSheetState) }
                .distinctUntilChanged()

        val searchQueryFlow = MutableStateFlow(searchQuery)
        val placeTypeGroupsFlow =
            searchQueryFlow
                .map { it.trim().lowercase() }
                .distinctUntilChanged()
                .map(::placeTypeGroupsMatching)
                .distinctUntilChanged()

        binding.placeTypesList.setContent {
            ProvideWindowInsets {
                LookARoundTheme {
                    val orientation = LocalConfiguration.current.orientation

                    val bottomSheetState =
                        bottomSheetSignalsFlow.collectAsState(
                            initial = BottomSheetBehavior.STATE_HIDDEN
                        )

                    val searchQuery = searchQueryFlow.collectAsState(initial = "")
                    val searchFocused = rememberSaveable { mutableStateOf(false) }
                    val placeTypeGroups =
                        placeTypeGroupsFlow.collectAsState(initial = placeTypeGroups)

                    val lazyListState = rememberLazyListState()
                    binding
                        .disallowInterceptTouchContainer
                        .shouldRequestDisallowInterceptTouchEvent =
                        (lazyListState.firstVisibleItemIndex != 0 ||
                            lazyListState.firstVisibleItemScrollOffset != 0) &&
                            bottomSheetState.value == BottomSheetBehavior.STATE_EXPANDED
                    LazyColumn(state = lazyListState, modifier = Modifier.fillMaxHeight()) {
                        if (bottomSheetState.value != BottomSheetBehavior.STATE_EXPANDED) {
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
                                        mainViewModel.intent(MainIntent.GetPlacesOfType(placeType))
                                        mainViewModel.signal(MainSignal.HideBottomSheet)
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
        placeTypeGroups
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
        private val placeTypeGroups =
            listOf(
                PlaceTypeGroup(
                    name = "General",
                    placeTypes =
                        listOf(
                            PlaceType(
                                wrapped = Amenity.PARKING,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Amenity.FUEL,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Amenity.CAR_WASH,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Amenity.ATM,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Amenity.POST_OFFICE,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Amenity.TOILETS,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            )
                        )
                ),
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
                    name = "Transport",
                    placeTypes =
                        listOf(
                            PlaceType(
                                wrapped = Amenity.BUS_STATION,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Amenity.TAXI,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Amenity.CAR_RENTAL,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Amenity.CAR_SHARING,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Amenity.BICYCLE_RENTAL,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            )
                        )
                ),
                PlaceTypeGroup(
                    name = "Shop",
                    placeTypes =
                        listOf(
                            PlaceType(
                                wrapped = Shop.CONVENIENCE,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Shop.SUPERMARKET,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Shop.MALL,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Shop.CLOTHES,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Shop.SHOES,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Shop.KIOSK,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Shop.ALCOHOL,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Shop.HAIRDRESSER,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Shop.CAR,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Shop.HARDWARE,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Shop.ELECTRONICS,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Shop.BOOKS,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Shop.SPORTS,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                        )
                ),
                PlaceTypeGroup(
                    name = "Tourism",
                    placeTypes =
                        listOf(
                            PlaceType(
                                wrapped = Tourism.HOTEL,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Tourism.ATTRACTION,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Tourism.INFORMATION,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Tourism.VIEWPOINT,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Tourism.MUSEUM,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Tourism.GALLERY,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Tourism.HOSTEL,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Tourism.MOTEL,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Tourism.CAMP_SITE,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Tourism.THEME_PARK,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Leisure.NATURE_RESERVE,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Tourism.ZOO,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
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
                            ),
                            PlaceType(
                                wrapped = Amenity.CASINO,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Amenity.LIBRARY,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            )
                        )
                ),
                PlaceTypeGroup(
                    name = "Leisure",
                    placeTypes =
                        listOf(
                            PlaceType(
                                wrapped = Leisure.PARK,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Leisure.GARDEN,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Leisure.PLAYGROUND,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Leisure.PITCH,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Leisure.SPORTS_CENTRE,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Leisure.SWIMMING_POOL,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Leisure.GOLF_COURSE,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            )
                        )
                ),
                PlaceTypeGroup(
                    name = "Health",
                    placeTypes =
                        listOf(
                            PlaceType(
                                wrapped = Amenity.PHARMACY,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Amenity.HOSPITAL,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Amenity.DOCTORS,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                            PlaceType(
                                wrapped = Amenity.VETERINARY,
                                imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                            ),
                        )
                )
            )
    }
}
