package com.lookaround.ui.place.categories

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.accompanist.insets.ProvideWindowInsets
import com.lookaround.core.android.architecture.ListFragmentHost
import com.lookaround.core.android.ext.addCollapseTopViewOnScrollListener
import com.lookaround.core.android.ext.dpToPx
import com.lookaround.core.android.ext.listItemBackground
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
import com.lookaround.ui.place.categories.databinding.FragmentPlaceCategoriesBinding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
@FlowPreview
class PlaceCategoriesFragment : Fragment(R.layout.fragment_place_categories) {
    private val binding by viewBinding(FragmentPlaceCategoriesBinding::bind)

    private val mainViewModel: MainViewModel by activityViewModels()

    private val placeTypesAdapter by
        lazy(LazyThreadSafetyMode.NONE) {
            PlaceTypesRecyclerViewAdapter(placeTypeListItems) { placeType ->
                lifecycleScope.launch {
                    mainViewModel.intent(MainIntent.GetPlacesOfType(placeType))
                    mainViewModel.signal(MainSignal.HideBottomSheet)
                }
            }
        }

    private var searchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getString(SavedStateKey.SEARCH_QUERY.name)?.let(::searchQuery::set)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.placeTypesRecyclerView.init()
        initSearchBar()
    }

    private fun initSearchBar() {
        val searchQueryFlow = MutableStateFlow(searchQuery)
        binding.placeTypesSearchBar.setContent {
            val searchQuery = searchQueryFlow.collectAsState(initial = "")
            val searchFocused = rememberSaveable { mutableStateOf(false) }

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

            ProvideWindowInsets {
                LookARoundTheme {
                    Column(
                        modifier = Modifier.onSizeChanged { addPlaceTypesListTopSpacer(it.height) }
                    ) {
                        SearchBar(
                            query = searchQuery.value,
                            focused = searchFocused.value,
                            onBackPressedDispatcher = requireActivity().onBackPressedDispatcher,
                            onSearchFocusChange = searchFocused::value::set,
                            onTextFieldValueChange = {
                                searchQueryFlow.value = it.text
                                this@PlaceCategoriesFragment.searchQuery = it.text
                            }
                        )

                        val orientation = LocalConfiguration.current.orientation
                        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                            PlaceCategoriesChipList(searchQueryFlow, opaqueBackground.value)
                        }
                    }
                }
            }
        }
    }

    private fun addPlaceTypesListTopSpacer(height: Int) {
        if (placeTypeListItems.first() is PlaceTypeListItem.Spacer) {
            placeTypeListItems.removeAt(0)
            placeTypesAdapter.notifyItemRemoved(0)
        }
        val layoutManager = binding.placeTypesRecyclerView.layoutManager as LinearLayoutManager
        val wasNotScrolled = layoutManager.findFirstCompletelyVisibleItemPosition() == 0
        val headerHeightPx = height + requireContext().dpToPx(8f).toInt()
        placeTypeListItems.add(0, PlaceTypeListItem.Spacer(headerHeightPx))
        placeTypesAdapter.notifyItemInserted(0)
        binding.placeTypesRecyclerView.apply {
            if (wasNotScrolled) scrollToTopAndShow() else visibility = View.VISIBLE
        }
    }

    @Composable
    private fun PlaceCategoriesChipList(searchQueryFlow: Flow<String>, opaqueBackground: Boolean) {
        val placeCategoriesFlow = remember {
            searchQueryFlow
                .map { it.trim().lowercase() }
                .distinctUntilChanged()
                .map(::placeCategoriesMatching)
                .distinctUntilChanged()
        }
        val scope = rememberCoroutineScope()
        val shape = RoundedCornerShape(20.dp)
        ChipList(
            itemsFlow = placeCategoriesFlow,
            label = PlaceTypeListItem.PlaceCategory::name::get,
            chipModifier =
                Modifier.clip(shape)
                    .background(
                        brush = Brush.horizontalGradient(colors = listOf(Ocean2, Ocean0)),
                        shape = shape,
                        alpha = if (opaqueBackground) .95f else .55f,
                    )
        ) { category -> scope.launch {} }
    }

    private fun placeCategoriesMatching(query: String): List<PlaceTypeListItem.PlaceCategory> =
        placeTypeListItems.filterIsInstance<PlaceTypeListItem.PlaceCategory>().filter {
            var index = placeTypeListItems.indexOf(it) + 1
            while (index < placeTypeListItems.size &&
                placeTypeListItems[index] is PlaceTypeListItem.PlaceType) {
                val placeType = (placeTypeListItems[index] as PlaceTypeListItem.PlaceType).wrapped
                if (placeType.label.lowercase().contains(query) ||
                        placeType.description.lowercase().contains(query)
                ) {
                    return@filter true
                }
                ++index
            }
            false
        }

    private fun RecyclerView.init() {
        adapter = placeTypesAdapter
        val orientation = resources.configuration.orientation
        val spanCount = if (orientation == Configuration.ORIENTATION_LANDSCAPE) 4 else 2
        layoutManager =
            GridLayoutManager(requireContext(), spanCount, GridLayoutManager.VERTICAL, false)
                .apply {
                    spanSizeLookup =
                        object : GridLayoutManager.SpanSizeLookup() {
                            override fun getSpanSize(position: Int): Int =
                                when (placeTypeListItems[position]) {
                                    is PlaceTypeListItem.PlaceCategory -> spanCount
                                    else -> 1
                                }
                        }
                }
        addCollapseTopViewOnScrollListener(binding.placeTypesSearchBar)
    }

    private fun RecyclerView.scrollToTopAndShow() {
        post {
            scrollToPosition(0)
            visibility = View.VISIBLE
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(SavedStateKey.SEARCH_QUERY.name, searchQuery)
    }

    private enum class SavedStateKey {
        SEARCH_QUERY
    }

    private val placeTypeListItems: MutableList<PlaceTypeListItem> =
        mutableListOf(
            PlaceTypeListItem.PlaceCategory("General"),
            PlaceTypeListItem.PlaceType(Amenity.PARKING, R.drawable.parking),
            PlaceTypeListItem.PlaceType(Amenity.FUEL, R.drawable.fuel),
            PlaceTypeListItem.PlaceType(Amenity.CAR_WASH, R.drawable.car_wash),
            PlaceTypeListItem.PlaceType(Amenity.ATM, R.drawable.atm),
            PlaceTypeListItem.PlaceType(Amenity.POST_OFFICE, R.drawable.post_office),
            PlaceTypeListItem.PlaceType(Amenity.TOILETS, R.drawable.toilet),
            PlaceTypeListItem.PlaceCategory("Food & drinks"),
            PlaceTypeListItem.PlaceType(Amenity.RESTAURANT, R.drawable.restaurant),
            PlaceTypeListItem.PlaceType(Amenity.CAFE, R.drawable.cafe),
            PlaceTypeListItem.PlaceType(Amenity.FAST_FOOD, R.drawable.fast_food),
            PlaceTypeListItem.PlaceType(Amenity.BAR, R.drawable.bar),
            PlaceTypeListItem.PlaceType(Amenity.PUB, R.drawable.pub),
            PlaceTypeListItem.PlaceType(Amenity.ICE_CREAM, R.drawable.ice_cream),
            PlaceTypeListItem.PlaceCategory("Transport"),
            PlaceTypeListItem.PlaceType(Amenity.BUS_STATION, R.drawable.bus_station),
            PlaceTypeListItem.PlaceType(Amenity.TAXI, R.drawable.taxi),
            PlaceTypeListItem.PlaceType(Amenity.CAR_RENTAL, R.drawable.car_rental),
            PlaceTypeListItem.PlaceCategory("Shop"),
            PlaceTypeListItem.PlaceType(Shop.CONVENIENCE, R.drawable.convenience),
            PlaceTypeListItem.PlaceType(Shop.SUPERMARKET, R.drawable.supermarket),
            PlaceTypeListItem.PlaceType(Shop.MALL, R.drawable.mall),
            PlaceTypeListItem.PlaceType(Shop.CLOTHES, R.drawable.clothes),
            PlaceTypeListItem.PlaceType(Shop.SHOES, R.drawable.shoes),
            PlaceTypeListItem.PlaceType(Shop.ALCOHOL, R.drawable.alcohol),
            PlaceTypeListItem.PlaceType(Shop.HAIRDRESSER, R.drawable.hairdresser),
            PlaceTypeListItem.PlaceType(Shop.CAR, R.drawable.car),
            PlaceTypeListItem.PlaceType(Shop.HARDWARE, R.drawable.hardware),
            PlaceTypeListItem.PlaceType(Shop.ELECTRONICS, R.drawable.electronics),
            PlaceTypeListItem.PlaceType(Shop.BOOKS, R.drawable.books),
            PlaceTypeListItem.PlaceCategory("Tourism"),
            PlaceTypeListItem.PlaceType(Tourism.HOTEL, R.drawable.hotel),
            PlaceTypeListItem.PlaceType(Tourism.VIEWPOINT, R.drawable.viewpoint),
            PlaceTypeListItem.PlaceType(Tourism.MUSEUM, R.drawable.museum),
            PlaceTypeListItem.PlaceType(Tourism.GALLERY, R.drawable.gallery),
            PlaceTypeListItem.PlaceType(Tourism.CAMP_SITE, R.drawable.camp_site),
            PlaceTypeListItem.PlaceType(Tourism.THEME_PARK, R.drawable.theme_park),
            PlaceTypeListItem.PlaceType(Leisure.NATURE_RESERVE, R.drawable.nature_reserve),
            PlaceTypeListItem.PlaceType(Tourism.ZOO, R.drawable.zoo),
            PlaceTypeListItem.PlaceCategory("Entertainment"),
            PlaceTypeListItem.PlaceType(Amenity.CINEMA, R.drawable.cinema),
            PlaceTypeListItem.PlaceType(Amenity.THEATRE, R.drawable.theatre),
            PlaceTypeListItem.PlaceType(Amenity.NIGHTCLUB, R.drawable.nightclub),
            PlaceTypeListItem.PlaceType(Amenity.EVENTS_VENUE, R.drawable.events_venue),
            PlaceTypeListItem.PlaceType(Amenity.CASINO, R.drawable.casino),
            PlaceTypeListItem.PlaceType(Amenity.LIBRARY, R.drawable.library),
            PlaceTypeListItem.PlaceCategory("Leisure"),
            PlaceTypeListItem.PlaceType(Leisure.PARK, R.drawable.park),
            PlaceTypeListItem.PlaceType(Leisure.GARDEN, R.drawable.garden),
            PlaceTypeListItem.PlaceType(Leisure.PLAYGROUND, R.drawable.playground),
            PlaceTypeListItem.PlaceType(Leisure.PITCH, R.drawable.pitch),
            PlaceTypeListItem.PlaceType(Leisure.SPORTS_CENTRE, R.drawable.sports_centre),
            PlaceTypeListItem.PlaceType(Leisure.SWIMMING_POOL, R.drawable.swimming_pool),
            PlaceTypeListItem.PlaceType(Leisure.GOLF_COURSE, R.drawable.golf_course),
            PlaceTypeListItem.PlaceCategory("Health"),
            PlaceTypeListItem.PlaceType(Amenity.PHARMACY, R.drawable.pharmacy),
            PlaceTypeListItem.PlaceType(Amenity.HOSPITAL, R.drawable.hospital),
            PlaceTypeListItem.PlaceType(Amenity.DOCTORS, R.drawable.doctors),
            PlaceTypeListItem.PlaceType(Amenity.VETERINARY, R.drawable.veterinary),
        )
}
