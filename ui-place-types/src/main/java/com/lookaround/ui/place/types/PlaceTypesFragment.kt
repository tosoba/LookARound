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
                                imageUrl =
                                    "https://img.freepik.com/free-photo/car-car-park_1150-8889.jpg?t=st=1645276573~exp=1645277173~hmac=5d7fb6b1d778177ffc8072f3c3968a254448667a186cc4f18bf634f1028c613c&w=1380"
                            ),
                            PlaceType(
                                wrapped = Amenity.FUEL,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/human-hand-is-holding-electric-car-charging-connect-electric-car_1153-5059.jpg?t=st=1645276688~exp=1645277288~hmac=1ddf286a149a974d56062acb6e65af9cabad0319b7dd85d7446cd44728a95b62&w=1060"
                            ),
                            PlaceType(
                                wrapped = Amenity.CAR_WASH,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/car-wash-detailing-station_1303-22319.jpg?t=st=1645276744~exp=1645277344~hmac=f183a10e0ce27050893f715f3c102c2e80df85c2b420a215252cc686be6f3ba7&w=1060"
                            ),
                            PlaceType(
                                wrapped = Amenity.ATM,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/woman-using-banking-machine-close-up_1391-333.jpg?t=st=1645276967~exp=1645277567~hmac=483c746658a75b8c3c01e2231a8d6191ddec90477bd2584f2ef3754573faa514&w=1060"
                            ),
                            PlaceType(
                                wrapped = Amenity.POST_OFFICE,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/woman-picking-up-mail_53876-144800.jpg?t=st=1645277132~exp=1645277732~hmac=e84cd046c5729d9878c73229363631b974d81ba91dd2c4a4f398fe61aed7e6dc&w=1060"
                            ),
                            PlaceType(
                                wrapped = Amenity.TOILETS,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/public-toilet_1417-1759.jpg?t=st=1645277208~exp=1645277808~hmac=db8cad1e258edbaeaae14bebb99375e61f94c87741e3b8eae915d9751c8309b7&w=1060"
                            )
                        )
                ),
                PlaceTypeGroup(
                    name = "Food & drinks",
                    placeTypes =
                        listOf(
                            PlaceType(
                                wrapped = Amenity.RESTAURANT,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/restaurant-hall-with-red-brick-walls-wooden-tables-pipes-ceiling_140725-8504.jpg?t=st=1645276062~exp=1645276662~hmac=0600602b99ce087adda48f8d65a5a631ccca58b72c6400c44aebb22fc65c1db6&w=996"
                            ),
                            PlaceType(
                                wrapped = Amenity.CAFE,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/high-angle-shot-coffee-beans-jars-breakfast-table-with-some-pastry_181624-5864.jpg?t=st=1645277441~exp=1645278041~hmac=0c22a95763841682df547c0af33aff67bcbad4a5c377c1066d169d16d3261aca&w=1060"
                            ),
                            PlaceType(
                                wrapped = Amenity.FAST_FOOD,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/top-view-fast-food-mix-hamburger-doner-sandwich-chicken-nuggets-rice-vegetable-salad-chicken-sticks-caesar-salad-mushrooms-pizza-chicken-ragout-french-fries-mayo_141793-3997.jpg?t=st=1645277467~exp=1645278067~hmac=5e6dd13eda7deb1679acd6b2744da5aea0a87273c3986096b196b642d98d8270&w=1380"
                            ),
                            PlaceType(
                                wrapped = Amenity.BAR,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/mid-section-bar-tender-filling-beer-from-bar-pump_107420-65356.jpg?t=st=1645277884~exp=1645278484~hmac=212ec528f36fed9d94c18c4e8b75e66b9df0f2e9751f259d10024326d5a102db&w=1060"
                            ),
                            PlaceType(
                                wrapped = Amenity.PUB,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/happy-friends-drinking-beer-counter-pub_155003-10075.jpg?t=st=1645278037~exp=1645278637~hmac=964d598b21c15ad12183c960ebbed99e18c15bf85ed6b860786a48fe24de42e2&w=1060"
                            ),
                            PlaceType(
                                wrapped = Amenity.ICE_CREAM,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/ice-cream-cone_144627-41497.jpg?t=st=1645278067~exp=1645278667~hmac=33459dd8cd68596483b9ad47ee256cb1abde93eb5f3cedec9a179925b177ae47&w=1060"
                            )
                        )
                ),
                PlaceTypeGroup(
                    name = "Transport",
                    placeTypes =
                        listOf(
                            PlaceType(
                                wrapped = Amenity.BUS_STATION,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/buses-bus-terminal-valletta_1398-182.jpg?t=st=1645278550~exp=1645279150~hmac=2f3b3e6ccb22f0940f46398ca3d8c14bc8d4484c62ed47cf77869c964c888766&w=1060"
                            ),
                            PlaceType(
                                wrapped = Amenity.TAXI,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/selective-focus-shot-yellow-taxi-sign-traffic-jam_181624-42635.jpg?t=st=1645278606~exp=1645279206~hmac=e497136e0237d5ac9329129b1aaef508c474578c962e81a04aac7240cc662d8c&w=1060"
                            ),
                            PlaceType(
                                wrapped = Amenity.CAR_RENTAL,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/stylish-black-woman-car-salon_1157-21402.jpg?t=st=1645278660~exp=1645279260~hmac=236139a96d6071919c72d77c2c9d1d0278d43e975058360c03e648bca6ff01cf&w=1060"
                            ),
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
