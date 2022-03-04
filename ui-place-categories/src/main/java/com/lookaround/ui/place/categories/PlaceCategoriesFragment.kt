package com.lookaround.ui.place.categories

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import com.lookaround.core.android.model.Amenity
import com.lookaround.core.android.model.Leisure
import com.lookaround.core.android.model.Shop
import com.lookaround.core.android.model.Tourism
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.model.MainIntent
import com.lookaround.ui.main.model.MainSignal
import com.lookaround.ui.place.categories.databinding.FragmentPlaceCategoriesBinding
import com.lookaround.ui.place.categories.model.PlaceTypeListItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
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
        with(binding.placeTypesRecyclerView) {
            adapter =
                PlaceTypesRecyclerViewAdapter(PLACE_TYPE_LIST_ITEMS) { placeType ->
                    lifecycleScope.launch {
                        mainViewModel.intent(MainIntent.GetPlacesOfType(placeType))
                        mainViewModel.signal(MainSignal.HideBottomSheet)
                    }
                }
            val orientation = resources.configuration.orientation
            val spanCount = if (orientation == Configuration.ORIENTATION_LANDSCAPE) 4 else 2
            layoutManager =
                GridLayoutManager(requireContext(), spanCount, GridLayoutManager.VERTICAL, false)
                    .apply {
                        spanSizeLookup =
                            object : GridLayoutManager.SpanSizeLookup() {
                                override fun getSpanSize(position: Int): Int =
                                    when (PLACE_TYPE_LIST_ITEMS[position]) {
                                        is PlaceTypeListItem.PlaceCategory -> spanCount
                                        is PlaceTypeListItem.PlaceType -> 1
                                    }
                            }
                    }
            setHasFixedSize(true)
            addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    var verticalOffset = 0
                    var scrollingUp = false

                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            if (scrollingUp) {
                                if (verticalOffset > binding.placeTypesSearchBar.height) {
                                    binding.placeTypesSearchBar.visibility = View.GONE
                                    verticalOffset = binding.placeTypesSearchBar.height
                                }
                            } else {
                                if (binding.placeTypesSearchBar.translationY <
                                        binding.placeTypesSearchBar.height * -0.6 &&
                                        verticalOffset > binding.placeTypesSearchBar.height
                                ) {
                                    binding.placeTypesSearchBar.visibility = View.GONE
                                    verticalOffset = binding.placeTypesSearchBar.height
                                } else {
                                    binding.placeTypesSearchBar.visibility = View.VISIBLE
                                }
                            }
                        }
                    }

                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        verticalOffset += dy
                        scrollingUp = dy > 0
                        binding.placeTypesSearchBar.animate().cancel()

                        val toolbarYOffset = dy - binding.placeTypesSearchBar.translationY
                        if (scrollingUp) {
                            if (toolbarYOffset < binding.placeTypesSearchBar.height) {
                                binding.placeTypesSearchBar.translationY = -toolbarYOffset
                            } else {
                                binding.placeTypesSearchBar.translationY =
                                    -binding.placeTypesSearchBar.height.toFloat()
                            }
                        } else {
                            if (toolbarYOffset < 0) {
                                binding.placeTypesSearchBar.translationY = 0f
                            } else {
                                binding.placeTypesSearchBar.translationY = -toolbarYOffset
                            }
                        }
                    }
                }
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(SavedStateKey.SEARCH_QUERY.name, searchQuery)
    }

    private enum class SavedStateKey {
        SEARCH_QUERY
    }

    companion object {
        private val PLACE_TYPE_LIST_ITEMS: List<PlaceTypeListItem> =
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
