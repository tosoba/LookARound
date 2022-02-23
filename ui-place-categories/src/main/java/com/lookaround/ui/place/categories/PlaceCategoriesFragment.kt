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
                            item { Spacer(Modifier.height(112.dp)) }
                        } else {
                            stickyHeader {
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
                                        ChipList(
                                            itemsFlow = placeCategoriesFlow,
                                            label = PlaceCategory::name::get,
                                            chipModifier =
                                                Modifier.clip(RoundedCornerShape(20.dp))
                                                    .background(
                                                        brush = backgroundGradientBrush,
                                                        shape = RoundedCornerShape(20.dp),
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
                                item {
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
                                items(group.placeTypes.chunked(rowItemsCount)) { chunk ->
                                    PlaceTypesRow(
                                        placeTypes = chunk,
                                        itemWidth = itemWidth,
                                        backgroundGradientBrush = backgroundGradientBrush,
                                        itemBackgroundAlpha = itemBackgroundAlpha
                                    )
                                }
                                item { Spacer(Modifier.height(4.dp)) }
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
                            PlaceType(wrapped = Amenity.PARKING, drawableId = R.drawable.parking),
                            PlaceType(wrapped = Amenity.FUEL, drawableId = R.drawable.fuel),
                            PlaceType(wrapped = Amenity.CAR_WASH, drawableId = R.drawable.car_wash),
                            PlaceType(wrapped = Amenity.ATM, drawableId = R.drawable.atm),
                            PlaceType(
                                wrapped = Amenity.POST_OFFICE,
                                drawableId = R.drawable.post_office
                            ),
                            PlaceType(wrapped = Amenity.TOILETS, drawableId = R.drawable.toilet)
                        )
                ),
                PlaceCategory(
                    name = "Food & drinks",
                    placeTypes =
                        listOf(
                            PlaceType(
                                wrapped = Amenity.RESTAURANT,
                                drawableId = R.drawable.restaurant
                            ),
                            PlaceType(wrapped = Amenity.CAFE, drawableId = R.drawable.cafe),
                            PlaceType(
                                wrapped = Amenity.FAST_FOOD,
                                drawableId = R.drawable.fast_food
                            ),
                            PlaceType(wrapped = Amenity.BAR, drawableId = R.drawable.bar),
                            PlaceType(wrapped = Amenity.PUB, drawableId = R.drawable.pub),
                            PlaceType(
                                wrapped = Amenity.ICE_CREAM,
                                drawableId = R.drawable.ice_cream
                            )
                        )
                ),
                PlaceCategory(
                    name = "Transport",
                    placeTypes =
                        listOf(
                            PlaceType(
                                wrapped = Amenity.BUS_STATION,
                                drawableId = R.drawable.bus_station
                            ),
                            PlaceType(wrapped = Amenity.TAXI, drawableId = R.drawable.taxi),
                            PlaceType(
                                wrapped = Amenity.CAR_RENTAL,
                                drawableId = R.drawable.car_rental
                            ),
                        )
                ),
                PlaceCategory(
                    name = "Shop",
                    placeTypes =
                        listOf(
                            PlaceType(
                                wrapped = Shop.CONVENIENCE,
                                drawableId = R.drawable.convenience
                            ),
                            PlaceType(
                                wrapped = Shop.SUPERMARKET,
                                drawableId = R.drawable.supermarket
                            ),
                            PlaceType(wrapped = Shop.MALL, drawableId = R.drawable.mall),
                            PlaceType(wrapped = Shop.CLOTHES, drawableId = R.drawable.clothes),
                            PlaceType(wrapped = Shop.SHOES, drawableId = R.drawable.shoes),
                            PlaceType(wrapped = Shop.ALCOHOL, drawableId = R.drawable.alcohol),
                            PlaceType(
                                wrapped = Shop.HAIRDRESSER,
                                drawableId = R.drawable.hairdresser
                            ),
                            PlaceType(wrapped = Shop.CAR, drawableId = R.drawable.car),
                            PlaceType(wrapped = Shop.HARDWARE, drawableId = R.drawable.hardware),
                            PlaceType(
                                wrapped = Shop.ELECTRONICS,
                                drawableId = R.drawable.electronics
                            ),
                            PlaceType(wrapped = Shop.BOOKS, drawableId = R.drawable.books),
                        )
                ),
                PlaceCategory(
                    name = "Tourism",
                    placeTypes =
                        listOf(
                            PlaceType(wrapped = Tourism.HOTEL, drawableId = R.drawable.hotel),
                            PlaceType(
                                wrapped = Tourism.VIEWPOINT,
                                drawableId = R.drawable.viewpoint
                            ),
                            PlaceType(wrapped = Tourism.MUSEUM, drawableId = R.drawable.museum),
                            PlaceType(wrapped = Tourism.GALLERY, drawableId = R.drawable.gallery),
                            PlaceType(
                                wrapped = Tourism.CAMP_SITE,
                                drawableId = R.drawable.camp_site
                            ),
                            PlaceType(
                                wrapped = Tourism.THEME_PARK,
                                drawableId = R.drawable.theme_park
                            ),
                            PlaceType(
                                wrapped = Leisure.NATURE_RESERVE,
                                drawableId = R.drawable.nature_reserve
                            ),
                            PlaceType(wrapped = Tourism.ZOO, drawableId = R.drawable.zoo),
                        )
                ),
                PlaceCategory(
                    name = "Entertainment",
                    placeTypes =
                        listOf(
                            PlaceType(wrapped = Amenity.CINEMA, drawableId = R.drawable.cinema),
                            PlaceType(wrapped = Amenity.THEATRE, drawableId = R.drawable.theatre),
                            PlaceType(
                                wrapped = Amenity.NIGHTCLUB,
                                drawableId = R.drawable.nightclub
                            ),
                            PlaceType(
                                wrapped = Amenity.EVENTS_VENUE,
                                drawableId = R.drawable.events_venue
                            ),
                            PlaceType(wrapped = Amenity.CASINO, drawableId = R.drawable.casino),
                            PlaceType(wrapped = Amenity.LIBRARY, drawableId = R.drawable.library)
                        )
                ),
                PlaceCategory(
                    name = "Leisure",
                    placeTypes =
                        listOf(
                            PlaceType(wrapped = Leisure.PARK, drawableId = R.drawable.park),
                            PlaceType(wrapped = Leisure.GARDEN, drawableId = R.drawable.garden),
                            PlaceType(
                                wrapped = Leisure.PLAYGROUND,
                                drawableId = R.drawable.playground
                            ),
                            PlaceType(wrapped = Leisure.PITCH, drawableId = R.drawable.pitch),
                            PlaceType(
                                wrapped = Leisure.SPORTS_CENTRE,
                                drawableId = R.drawable.sports_centre
                            ),
                            PlaceType(
                                wrapped = Leisure.SWIMMING_POOL,
                                drawableId = R.drawable.swimming_pool
                            ),
                            PlaceType(
                                wrapped = Leisure.GOLF_COURSE,
                                drawableId = R.drawable.golf_course
                            )
                        )
                ),
                PlaceCategory(
                    name = "Health",
                    placeTypes =
                        listOf(
                            PlaceType(wrapped = Amenity.PHARMACY, drawableId = R.drawable.pharmacy),
                            PlaceType(wrapped = Amenity.HOSPITAL, drawableId = R.drawable.hospital),
                            PlaceType(wrapped = Amenity.DOCTORS, drawableId = R.drawable.doctors),
                            PlaceType(
                                wrapped = Amenity.VETERINARY,
                                drawableId = R.drawable.veterinary
                            ),
                        )
                )
            )
    }
}
