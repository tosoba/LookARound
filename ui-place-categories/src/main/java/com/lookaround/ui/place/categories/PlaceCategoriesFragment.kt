package com.lookaround.ui.place.categories

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.lookaround.core.android.architecture.ListFragmentHost
import com.lookaround.core.android.ext.dpToPx
import com.lookaround.core.android.ext.getListItemDimensionPx
import com.lookaround.core.android.ext.listItemBackground
import com.lookaround.core.android.ext.pxToDp
import com.lookaround.core.android.model.Amenity
import com.lookaround.core.android.model.Leisure
import com.lookaround.core.android.model.Shop
import com.lookaround.core.android.model.Tourism
import com.lookaround.core.android.view.composable.ChipList
import com.lookaround.core.android.view.composable.SearchBar
import com.lookaround.core.android.view.theme.LookARoundTheme
import com.lookaround.core.android.view.theme.Ocean0
import com.lookaround.core.android.view.theme.Ocean2
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.listFragmentItemBackgroundUpdates
import com.lookaround.ui.main.model.MainIntent
import com.lookaround.ui.main.model.MainSignal
import com.lookaround.ui.place.categories.composable.PlaceCategoryHeader
import com.lookaround.ui.place.categories.composable.PlaceType
import com.lookaround.ui.place.categories.composable.placeTypeShape
import com.lookaround.ui.place.categories.databinding.FragmentPlaceCategoriesBinding
import com.lookaround.ui.place.categories.model.PlaceCategory
import com.lookaround.ui.place.categories.model.PlaceType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
@FlowPreview
class PlaceCategoriesFragment : Fragment(R.layout.fragment_place_categories) {
    private val binding: FragmentPlaceCategoriesBinding by
        viewBinding(FragmentPlaceCategoriesBinding::bind)

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
        val placeCategoriesFlow =
            searchQueryFlow
                .map { it.trim().lowercase() }
                .distinctUntilChanged()
                .map(::placeCategoriesMatching)
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
                    val placeCategories =
                        placeCategoriesFlow.collectAsState(initial = placeCategories)

                    val opaqueBackgroundFlow = remember {
                        mainViewModel
                            .listFragmentItemBackgroundUpdates
                            .map { it == ListFragmentHost.ItemBackground.OPAQUE }
                            .distinctUntilChanged()
                    }
                    val opaqueBackground =
                        opaqueBackgroundFlow.collectAsState(
                            initial = listItemBackground == ListFragmentHost.ItemBackground.OPAQUE
                        )

                    val lazyListState = rememberLazyListState()
                    binding
                        .disallowInterceptTouchContainer
                        .shouldRequestDisallowInterceptTouchEvent =
                        (lazyListState.firstVisibleItemIndex != 0 ||
                            lazyListState.firstVisibleItemScrollOffset != 0) &&
                            bottomSheetState.value == BottomSheetBehavior.STATE_EXPANDED

                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.padding(horizontal = 5.dp).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val rowItemsCount =
                            if (orientation == Configuration.ORIENTATION_LANDSCAPE) 4 else 2

                        val itemBackgroundAlpha = if (opaqueBackground.value) .95f else .55f
                        val backgroundGradientBrush =
                            Brush.horizontalGradient(colors = listOf(Ocean2, Ocean0))

                        if (bottomSheetState.value != BottomSheetBehavior.STATE_EXPANDED) {
                            item(key = "categories-top-spacer") { Spacer(Modifier.height(112.dp)) }
                        } else {
                            stickyHeader(key = "categories-sticky-header") {
                                val headerPaddingBottomPx = requireContext().dpToPx(10f).toInt()
                                var headerHeightPx by remember { mutableStateOf(0) }
                                Column(
                                    modifier =
                                        Modifier.onSizeChanged {
                                            headerHeightPx = it.height + headerPaddingBottomPx
                                        }
                                ) {
                                    SearchBar(
                                        query = searchQuery.value,
                                        focused = searchFocused.value,
                                        onBackPressedDispatcher =
                                            requireActivity().onBackPressedDispatcher,
                                        onSearchFocusChange = searchFocused::value::set,
                                        onTextFieldValueChange = {
                                            searchQueryFlow.value = it.text
                                            this@PlaceCategoriesFragment.searchQuery = it.text
                                        }
                                    )
                                    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                                        val scope = rememberCoroutineScope()
                                        val shape = RoundedCornerShape(20.dp)
                                        ChipList(
                                            itemsFlow = placeCategoriesFlow,
                                            label = PlaceCategory::name::get,
                                            chipModifier =
                                                Modifier.clip(shape)
                                                    .background(
                                                        brush = backgroundGradientBrush,
                                                        shape = shape,
                                                        alpha = itemBackgroundAlpha,
                                                    )
                                        ) { category ->
                                            scope.launch {
                                                val placeCategoryIndex =
                                                    placeCategories.value.indexOfFirst {
                                                        it.name == category.name
                                                    }
                                                val placeItemsCount =
                                                    if (placeCategoryIndex == 0) {
                                                        0
                                                    } else {
                                                        placeCategories.value.take(
                                                                placeCategoryIndex
                                                            )
                                                            .sumOf {
                                                                val useExtraRowIncrement =
                                                                    if (it.placeTypes.size %
                                                                            rowItemsCount == 0
                                                                    ) {
                                                                        0
                                                                    } else {
                                                                        1
                                                                    }
                                                                it.placeTypes.size / rowItemsCount +
                                                                    useExtraRowIncrement
                                                            }
                                                    }
                                                lazyListState.scrollToItem(
                                                    index =
                                                        placeCategoryIndex * 2 +
                                                            placeItemsCount +
                                                            1,
                                                    scrollOffset = -headerHeightPx
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (placeCategories.value.isNotEmpty()) {
                            val itemWidth =
                                requireContext().pxToDp(getListItemDimensionPx()).toInt()

                            placeCategories.value.forEach { group ->
                                item(key = group.name) {
                                    PlaceCategoryHeader(
                                        group,
                                        modifier =
                                            Modifier.background(
                                                brush = backgroundGradientBrush,
                                                shape = placeTypeShape,
                                                alpha = itemBackgroundAlpha,
                                            )
                                    )
                                }
                                items(
                                    group.placeTypes.chunked(rowItemsCount),
                                    key = { placeTypes ->
                                        placeTypes.joinToString("-") { it.wrapped.typeValue }
                                    }
                                ) { chunk ->
                                    PlaceTypesRow(
                                        placeTypes = chunk,
                                        itemWidth = itemWidth,
                                        backgroundGradientBrush = backgroundGradientBrush,
                                        itemBackgroundAlpha = itemBackgroundAlpha
                                    )
                                }
                                item(key = "${group.name}-bottom-spacer") {
                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                        } else {
                            item(key = "no-place-types-found-text") {
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

    @Composable
    private fun PlaceTypesRow(
        placeTypes: List<PlaceType>,
        itemWidth: Int,
        backgroundGradientBrush: Brush,
        itemBackgroundAlpha: Float
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier =
                Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    .fillMaxWidth()
                    .wrapContentHeight()
        ) {
            val scope = rememberCoroutineScope()
            placeTypes.forEach { placeType ->
                PlaceType(
                    placeType = placeType,
                    modifier =
                        Modifier.padding(horizontal = 1.dp)
                            .width(itemWidth.dp)
                            .clip(placeTypeShape)
                            .background(
                                brush = backgroundGradientBrush,
                                shape = placeTypeShape,
                                alpha = itemBackgroundAlpha,
                            )
                            .weight(1f, fill = false)
                            .clickable {
                                scope.launch {
                                    mainViewModel.intent(
                                        MainIntent.GetPlacesOfType(placeType.wrapped)
                                    )
                                    mainViewModel.signal(MainSignal.HideBottomSheet)
                                }
                            },
                )
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(SavedStateKey.SEARCH_QUERY.name, searchQuery)
    }

    private fun placeCategoriesMatching(query: String): List<PlaceCategory> =
        placeCategories
            .map { category ->
                PlaceCategory(
                    name = category.name,
                    placeTypes =
                        category.placeTypes.filter { (wrapped) ->
                            wrapped.label.lowercase().contains(query) ||
                                wrapped.description.lowercase().contains(query)
                        }
                )
            }
            .filter { it.placeTypes.isNotEmpty() }

    private enum class SavedStateKey {
        SEARCH_QUERY
    }

    companion object {
        private val placeCategories =
            listOf(
                PlaceCategory(
                    name = "General",
                    placeTypes =
                        listOf(
                            PlaceType(Amenity.PARKING, R.drawable.parking),
                            PlaceType(Amenity.FUEL, R.drawable.fuel),
                            PlaceType(Amenity.CAR_WASH, R.drawable.car_wash),
                            PlaceType(Amenity.ATM, R.drawable.atm),
                            PlaceType(Amenity.POST_OFFICE, R.drawable.post_office),
                            PlaceType(Amenity.TOILETS, R.drawable.toilet)
                        )
                ),
                PlaceCategory(
                    name = "Food & drinks",
                    placeTypes =
                        listOf(
                            PlaceType(Amenity.RESTAURANT, R.drawable.restaurant),
                            PlaceType(Amenity.CAFE, R.drawable.cafe),
                            PlaceType(Amenity.FAST_FOOD, R.drawable.fast_food),
                            PlaceType(Amenity.BAR, R.drawable.bar),
                            PlaceType(Amenity.PUB, R.drawable.pub),
                            PlaceType(Amenity.ICE_CREAM, R.drawable.ice_cream)
                        )
                ),
                PlaceCategory(
                    name = "Transport",
                    placeTypes =
                        listOf(
                            PlaceType(Amenity.BUS_STATION, R.drawable.bus_station),
                            PlaceType(Amenity.TAXI, R.drawable.taxi),
                            PlaceType(Amenity.CAR_RENTAL, R.drawable.car_rental),
                        )
                ),
                PlaceCategory(
                    name = "Shop",
                    placeTypes =
                        listOf(
                            PlaceType(Shop.CONVENIENCE, R.drawable.convenience),
                            PlaceType(Shop.SUPERMARKET, R.drawable.supermarket),
                            PlaceType(Shop.MALL, R.drawable.mall),
                            PlaceType(Shop.CLOTHES, R.drawable.clothes),
                            PlaceType(Shop.SHOES, R.drawable.shoes),
                            PlaceType(Shop.ALCOHOL, R.drawable.alcohol),
                            PlaceType(Shop.HAIRDRESSER, R.drawable.hairdresser),
                            PlaceType(Shop.CAR, R.drawable.car),
                            PlaceType(Shop.HARDWARE, R.drawable.hardware),
                            PlaceType(Shop.ELECTRONICS, R.drawable.electronics),
                            PlaceType(Shop.BOOKS, R.drawable.books),
                        )
                ),
                PlaceCategory(
                    name = "Tourism",
                    placeTypes =
                        listOf(
                            PlaceType(Tourism.HOTEL, R.drawable.hotel),
                            PlaceType(Tourism.VIEWPOINT, R.drawable.viewpoint),
                            PlaceType(Tourism.MUSEUM, R.drawable.museum),
                            PlaceType(Tourism.GALLERY, R.drawable.gallery),
                            PlaceType(Tourism.CAMP_SITE, R.drawable.camp_site),
                            PlaceType(Tourism.THEME_PARK, R.drawable.theme_park),
                            PlaceType(Leisure.NATURE_RESERVE, R.drawable.nature_reserve),
                            PlaceType(Tourism.ZOO, R.drawable.zoo),
                        )
                ),
                PlaceCategory(
                    name = "Entertainment",
                    placeTypes =
                        listOf(
                            PlaceType(Amenity.CINEMA, R.drawable.cinema),
                            PlaceType(Amenity.THEATRE, R.drawable.theatre),
                            PlaceType(Amenity.NIGHTCLUB, R.drawable.nightclub),
                            PlaceType(Amenity.EVENTS_VENUE, R.drawable.events_venue),
                            PlaceType(Amenity.CASINO, R.drawable.casino),
                            PlaceType(Amenity.LIBRARY, R.drawable.library)
                        )
                ),
                PlaceCategory(
                    name = "Leisure",
                    placeTypes =
                        listOf(
                            PlaceType(Leisure.PARK, R.drawable.park),
                            PlaceType(Leisure.GARDEN, R.drawable.garden),
                            PlaceType(Leisure.PLAYGROUND, R.drawable.playground),
                            PlaceType(Leisure.PITCH, R.drawable.pitch),
                            PlaceType(Leisure.SPORTS_CENTRE, R.drawable.sports_centre),
                            PlaceType(Leisure.SWIMMING_POOL, R.drawable.swimming_pool),
                            PlaceType(Leisure.GOLF_COURSE, R.drawable.golf_course)
                        )
                ),
                PlaceCategory(
                    name = "Health",
                    placeTypes =
                        listOf(
                            PlaceType(Amenity.PHARMACY, R.drawable.pharmacy),
                            PlaceType(Amenity.HOSPITAL, R.drawable.hospital),
                            PlaceType(Amenity.DOCTORS, R.drawable.doctors),
                            PlaceType(Amenity.VETERINARY, R.drawable.veterinary),
                        )
                )
            )
    }
}
