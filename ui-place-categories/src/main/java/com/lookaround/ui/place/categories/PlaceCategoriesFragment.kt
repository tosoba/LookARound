package com.lookaround.ui.place.categories

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.accompanist.insets.ProvideWindowInsets
import com.lookaround.core.android.ext.addCollapseTopViewOnScrollListener
import com.lookaround.core.android.ext.scrollToTopAndShow
import com.lookaround.core.android.model.Amenity
import com.lookaround.core.android.model.Leisure
import com.lookaround.core.android.model.Shop
import com.lookaround.core.android.model.Tourism
import com.lookaround.core.android.view.composable.SearchBar
import com.lookaround.core.android.view.recyclerview.ChipsRecyclerViewAdapter
import com.lookaround.core.android.view.theme.LookARoundTheme
import com.lookaround.core.model.IPlaceType
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.model.MainIntent
import com.lookaround.ui.main.model.MainSignal
import com.lookaround.ui.place.categories.databinding.FragmentPlaceCategoriesBinding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
@FlowPreview
class PlaceCategoriesFragment : Fragment(R.layout.fragment_place_categories) {
    private val binding by viewBinding(FragmentPlaceCategoriesBinding::bind)

    private val mainViewModel: MainViewModel by activityViewModels()

    private var placeTypeListItems = ArrayList(allPlaceTypeListItems)
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
    private var searchFocused: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) initFromSavedState(savedInstanceState)
    }

    private fun initFromSavedState(savedInstanceState: Bundle) {
        with(savedInstanceState) {
            getString(SavedStateKey.SEARCH_QUERY.name)?.let(::searchQuery::set)
            searchFocused = getBoolean(SavedStateKey.SEARCH_FOCUSED.name)
            getParcelableArrayList<PlaceTypeListItem>(SavedStateKey.PLACE_TYPE_ITEMS.name)
                ?.let(::placeTypeListItems::set)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val searchQueryFlow = MutableStateFlow(searchQuery)
        binding.placeTypesRecyclerView.initWith(searchQueryFlow)
        initSearchBarWith(searchQueryFlow)
    }

    private fun initSearchBarWith(searchQueryFlow: MutableStateFlow<String>) {
        binding.placeTypesSearchBar.setContent {
            val scope = rememberCoroutineScope()

            val topSpacerHeightPx = remember { mutableStateOf(0) }
            remember {
                snapshotFlow(topSpacerHeightPx::value)
                    .drop(1)
                    .distinctUntilChanged()
                    .debounce(500L)
                    .onEach(::addTopSpacer)
                    .launchIn(scope)
            }

            val searchQueryState = searchQueryFlow.collectAsState(initial = searchQuery)
            var searchFocusedState by remember { mutableStateOf(searchFocused) }

            ProvideWindowInsets {
                LookARoundTheme {
                    Column(
                        modifier = Modifier.onSizeChanged { topSpacerHeightPx.value = it.height }
                    ) {
                        SearchBar(
                            query = searchQueryState.value,
                            focused = searchFocusedState,
                            onBackPressedDispatcher = requireActivity().onBackPressedDispatcher,
                            onSearchFocusChange = {
                                searchFocusedState = it
                                searchFocused = it
                            },
                            onTextFieldValueChange = {
                                searchQueryFlow.value = it.text
                                searchQuery = it.text
                            },
                            leadingUnfocused = {
                                IconButton(onClick = { requireActivity().onBackPressed() }) {
                                    Icon(
                                        imageVector = Icons.Outlined.ArrowBack,
                                        tint = LookARoundTheme.colors.iconPrimary,
                                        contentDescription = stringResource(R.string.back)
                                    )
                                }
                            }
                        )
                        PlaceCategoriesRecyclerView(searchQueryFlow, topSpacerHeightPx.value)
                    }
                }
            }
        }
    }

    @Composable
    private fun PlaceCategoriesRecyclerView(searchQueryFlow: Flow<String>, topSpacerHeightPx: Int) {
        AndroidView(
            factory = { context ->
                RecyclerView(context).apply {
                    layoutParams =
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                }
            },
            update = { recyclerView ->
                recyclerView.layoutManager =
                    LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                val adapter =
                    ChipsRecyclerViewAdapter(
                        items = emptyList(),
                        label = PlaceTypeListItem.PlaceCategory::name::get,
                        transparent = false
                    ) { category ->
                        (binding.placeTypesRecyclerView.layoutManager as GridLayoutManager)
                            .scrollToPositionWithOffset(
                                placeTypeListItems.indexOf(category),
                                topSpacerHeightPx
                            )
                    }
                searchQueryFlow
                    .map { query -> query.trim().lowercase() }
                    .distinctUntilChanged()
                    .map(::placeCategoriesMatching)
                    .distinctUntilChanged()
                    .onEach(adapter::updateItems)
                    .launchIn(viewLifecycleOwner.lifecycleScope)
                recyclerView.adapter = adapter
            }
        )
    }

    private fun addTopSpacer(height: Int): Int {
        if (placeTypeListItems.firstOrNull() is PlaceTypeListItem.Spacer) {
            binding.placeTypesRecyclerView.visibility = View.VISIBLE
            return height
        }
        val layoutManager = binding.placeTypesRecyclerView.layoutManager as LinearLayoutManager
        val wasNotScrolled = layoutManager.findFirstCompletelyVisibleItemPosition() == 0
        placeTypeListItems.add(0, PlaceTypeListItem.Spacer(height))
        placeTypesAdapter.notifyItemInserted(0)
        binding.placeTypesRecyclerView.apply {
            if (wasNotScrolled) scrollToTopAndShow() else visibility = View.VISIBLE
        }
        return height
    }

    private fun placeCategoriesMatching(query: String): List<PlaceTypeListItem.PlaceCategory> =
        allPlaceTypeListItems.filterIsInstance<PlaceTypeListItem.PlaceCategory>().filter {
            var index = allPlaceTypeListItems.indexOf(it) + 1
            while (index < allPlaceTypeListItems.size &&
                allPlaceTypeListItems[index] is PlaceTypeListItem.PlaceType<*>) {
                val placeType =
                    (allPlaceTypeListItems[index] as PlaceTypeListItem.PlaceType<*>).wrapped
                if (placeType.matchesQuery(query)) {
                    return@filter true
                }
                ++index
            }
            false
        }

    private fun IPlaceType.matchesQuery(query: String) =
        label.lowercase().contains(query) || description.lowercase().contains(query)

    private fun RecyclerView.initWith(searchQueryFlow: Flow<String>) {
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

        searchQueryFlow
            .map { it.trim().lowercase() }
            .distinctUntilChanged()
            .map { query ->
                placeTypeListItems =
                    ArrayList(allPlaceTypeListItems).apply {
                        if (placeTypeListItems.firstOrNull() is PlaceTypeListItem.Spacer) {
                            add(0, placeTypeListItems[0])
                        }
                    }
                if (query.isNotBlank()) {
                    for (index in placeTypeListItems.size - 1 downTo 0) {
                        when (val item = placeTypeListItems[index]) {
                            is PlaceTypeListItem.PlaceCategory -> {
                                if (index + 1 == placeTypeListItems.size ||
                                        placeTypeListItems[index + 1]
                                            is PlaceTypeListItem.PlaceCategory
                                ) {
                                    placeTypeListItems.removeAt(index)
                                }
                            }
                            is PlaceTypeListItem.PlaceType<*> -> {
                                if (!item.wrapped.matchesQuery(query)) {
                                    placeTypeListItems.removeAt(index)
                                }
                            }
                            else -> continue
                        }
                    }
                }
                placeTypeListItems
            }
            .distinctUntilChanged()
            .onEach(placeTypesAdapter::updateItems)
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(SavedStateKey.SEARCH_QUERY.name, searchQuery)
        outState.putBoolean(SavedStateKey.SEARCH_FOCUSED.name, searchFocused)
        outState.putParcelableArrayList(SavedStateKey.PLACE_TYPE_ITEMS.name, placeTypeListItems)
    }

    private enum class SavedStateKey {
        SEARCH_QUERY,
        SEARCH_FOCUSED,
        PLACE_TYPE_ITEMS
    }

    companion object {
        private val allPlaceTypeListItems: List<PlaceTypeListItem> =
            listOf(
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
}
