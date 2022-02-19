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
                                imageUrl =
                                    "https://img.freepik.com/free-photo/variety-organic-vegetables-supermarket_53876-138173.jpg?t=st=1645288192~exp=1645288792~hmac=d6b549de2bf0f0765dc62739182368eff010ba1c6d1a2a1fb765a7aad0232f0a&w=826"
                            ),
                            PlaceType(
                                wrapped = Shop.SUPERMARKET,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/supermarket-cart_53876-65615.jpg?t=st=1645288285~exp=1645288885~hmac=77cbfc8afd50805bf5da4f98e4e218e611bd0a52f74e03a0633121ff5d49179c&w=826"
                            ),
                            PlaceType(
                                wrapped = Shop.MALL,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/indoor-hotel-view_1417-1566.jpg?t=st=1645288221~exp=1645288821~hmac=cdb76d3014c9d431c2c2316138725656c784e227fd387eefb3e3a7b57986a45a&w=826"
                            ),
                            PlaceType(
                                wrapped = Shop.CLOTHES,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/shop-clothing-clothes-shop-hanger-modern-shop-boutique_1150-8886.jpg?t=st=1645286783~exp=1645287383~hmac=316958f5de05f30509223c9fbfb5226a0b936cc5faa962cb1d9d733e9c26b06a&w=826"
                            ),
                            PlaceType(
                                wrapped = Shop.SHOES,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/stylish-girl-sitting-floor-dressing-room-with-smartphone-hands-writes-message-surrounded-by-variety-shoes-she-is-dressed-black-skirt-her-feet-silver-luxury-shoes_197531-1717.jpg?t=st=1645287011~exp=1645287611~hmac=94925b15d85d0cccc24d2d9b0c224d5f6adbca5747b566ce58f4564a1a90124e&w=826"
                            ),
                            PlaceType(
                                wrapped = Shop.ALCOHOL,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/pouring-strong-alcohol-drink-into-glasses-which-are-wooden-table_8353-11029.jpg?t=st=1645287091~exp=1645287691~hmac=12042d4a49a8cb96980ddd8a23617bf0ed7cb6de5fa13c56fb7a15bbf23f9e44&w=826"
                            ),
                            PlaceType(
                                wrapped = Shop.HAIRDRESSER,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/vintage-chair-barbershop_155003-1387.jpg?t=st=1645287263~exp=1645287863~hmac=292aa936c756f409973dd2bd58f6c50aea83385a5b58d776b8bea76d0a596e6c&w=826"
                            ),
                            PlaceType(
                                wrapped = Shop.CAR,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/close-up-sales-manager-black-suit-selling-car-customer_146671-14738.jpg?t=st=1645287303~exp=1645287903~hmac=a74d3a61df70d527591f65a91774af02f9eb7c52408c35a977ae2baf734eff28&w=826"
                            ),
                            PlaceType(
                                wrapped = Shop.HARDWARE,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/diy-tools_144627-32164.jpg?t=st=1645287346~exp=1645287946~hmac=6422da09ba86002d4ff5bf9f63592e02352133d92e21e64082f9350fdb0628ca&w=826"
                            ),
                            PlaceType(
                                wrapped = Shop.ELECTRONICS,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/tablets-lined-up-display-shopping-mall_53876-97239.jpg?t=st=1645287403~exp=1645288003~hmac=554a5bb13011ebd23bada697b6c914daf20a3d8e30d112a9f040444426a66d3b&w=826"
                            ),
                            PlaceType(
                                wrapped = Shop.BOOKS,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/book-library-with-open-textbook_1150-5924.jpg?t=st=1645287534~exp=1645288134~hmac=ce9c9929dc54775c35ebbf2aa6ce745d3ca75d4171b89f5630c3fb98d9a299a6&w=826"
                            ),
                        )
                ),
                PlaceTypeGroup(
                    name = "Tourism",
                    placeTypes =
                        listOf(
                            PlaceType(
                                wrapped = Tourism.HOTEL,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/hotel-sign_1101-846.jpg?t=st=1645281531~exp=1645282131~hmac=aa9d5b48978d74f6b041f4cc78be46aea4c2a5ebe767c41bcec638fb735b9bc8&w=826"
                            ),
                            PlaceType(
                                wrapped = Tourism.VIEWPOINT,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/panoramic-view-barcelona-multiple-building-s-roofs-view-from-parc-guell-spain_1268-18048.jpg?t=st=1645281799~exp=1645282399~hmac=1d73a3f71089e7447e0dd39846d55f406ff28e42f69eab33ff1a52757f953c1d&w=826"
                            ),
                            PlaceType(
                                wrapped = Tourism.MUSEUM,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/low-angle-shot-michelangelo-s-david-gallery-academy-florence_181624-11718.jpg?t=st=1645287883~exp=1645288483~hmac=d40b2491fd88904740fd5a9eb9092f8f6ff85e2419526704e8115b917af7d1c3&w=826"
                            ),
                            PlaceType(
                                wrapped = Tourism.GALLERY,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/long-narrow-painting-art-exhibition_53876-15353.jpg?t=st=1645288129~exp=1645288729~hmac=db5b8c99315531f62ec53055ff319d66f716b4e3f02ba1a1551c9625283e1944&w=826"
                            ),
                            PlaceType(
                                wrapped = Tourism.CAMP_SITE,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/tourist-tents-are-green-misty-forest-mountains_146671-18467.jpg?t=st=1645281449~exp=1645282049~hmac=d56831ca32ab3d00af71bbbfe4422016810e7813d11b658d3bc304e4f0acd0d2&w=826"
                            ),
                            PlaceType(
                                wrapped = Tourism.THEME_PARK,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/carousel-horses-carnival-merry-go-round_1417-102.jpg?t=st=1645281426~exp=1645282026~hmac=1cb5fa331302b31003e23858cbc4514351b40e8961362e977b8b622547ea36ef&w=826"
                            ),
                            PlaceType(
                                wrapped = Leisure.NATURE_RESERVE,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/river-surrounded-by-forests-cloudy-sky-thuringia-germany_181624-30863.jpg?t=st=1645281370~exp=1645281970~hmac=0468b46dad7b250b0c56dc095084584fa40110c11c5059d55f3b807458f89f5d&w=826"
                            ),
                            PlaceType(
                                wrapped = Tourism.ZOO,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/snow-leopard-portrait-amazing-light-wild-animal-nature-habitat-very-rare-unique-wild-cat-irbis-panthera-uncia-uncia-uncia_475641-1807.jpg?t=st=1645281229~exp=1645281829~hmac=1177491bce4179bf54ed10cfde2b5b5f5a30336469c496c5a2d4ef25485d7c12&w=826"
                            ),
                        )
                ),
                PlaceTypeGroup(
                    name = "Entertainment",
                    placeTypes =
                        listOf(
                            PlaceType(
                                wrapped = Amenity.CINEMA,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/rows-red-seats-theater_53876-64710.jpg?t=st=1645287720~exp=1645288320~hmac=b36b7817a5ee42d3be9ec0fcbfb130cd7d4e151dd04022b3e51387c30f693f38&w=826"
                            ),
                            PlaceType(
                                wrapped = Amenity.THEATRE,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/beautiful-masks-pieces-clothes_23-2147745290.jpg?t=st=1645287740~exp=1645288340~hmac=ef0a8e0c265ae4f737411a74c9282e5f349cdc7381fe069d55dae4a483eba306&w=826"
                            ),
                            PlaceType(
                                wrapped = Amenity.NIGHTCLUB,
                                imageUrl =
                                    "https://img.freepik.com/free-vector/party-crowd-silhouettes-dancing-nightclub_1048-11557.jpg?t=st=1645287822~exp=1645288422~hmac=c967d6d051290aaeeaa25bd0788f61c7760908987fbef6effe39584d26d11a9b&w=826"
                            ),
                            PlaceType(
                                wrapped = Amenity.EVENTS_VENUE,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/decorated-hall-wedding-is-ready-celebration_8353-10236.jpg?t=st=1645287665~exp=1645288265~hmac=3bb5b4556f80f51567ee403a36e7851f5e1e20a166bb784049b5188d98464f8e&w=826"
                            ),
                            PlaceType(
                                wrapped = Amenity.CASINO,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/red-dice-human-hand-casino_23-2147881393.jpg?t=st=1645287612~exp=1645288212~hmac=420bb451bdd6f6af0bfb1f7301be21e6695737ebc4c667c4748a1603c6136e06&w=826"
                            ),
                            PlaceType(
                                wrapped = Amenity.LIBRARY,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/laptop-computer-book-workplace-library-room_1150-5925.jpg?t=st=1645287587~exp=1645288187~hmac=f927e2d9c8d4ae239dff4ad5463daabea24917d4165743fbb3a86a0cfdeb1112&w=826"
                            )
                        )
                ),
                PlaceTypeGroup(
                    name = "Leisure",
                    placeTypes =
                        listOf(
                            PlaceType(
                                wrapped = Leisure.PARK,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/park-with-wooden-pathway-benches_1137-254.jpg?t=st=1645281105~exp=1645281705~hmac=ee99275e2c4c941a8c72d7af9679d40da0dbdc1c7a2a1d8fdfe7dfb2bc8eb898&w=826"
                            ),
                            PlaceType(
                                wrapped = Leisure.GARDEN,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/empty-pavilion-garden_1339-3183.jpg?t=st=1645281071~exp=1645281671~hmac=0b74caf0be2195d1a0a706dda753af964d5046dc60475413231452ca3111fe45&w=826"
                            ),
                            PlaceType(
                                wrapped = Leisure.PLAYGROUND,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/playground-city-street_1398-4747.jpg?t=st=1645281018~exp=1645281618~hmac=707ec4df4c0c3f2e8a37762e76665a281bde93db1c7667355799825c9eabbc35&w=826"
                            ),
                            PlaceType(
                                wrapped = Leisure.PITCH,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/empty-football-field-green-grass_155003-2491.jpg?t=st=1645280930~exp=1645281530~hmac=9349cb385e0fd561076462629f33caf2043ec19cc8f01a9a6b0e47ede1e4de3b&w=826"
                            ),
                            PlaceType(
                                wrapped = Leisure.SPORTS_CENTRE,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/3d-rendering-modern-loft-gym-fitness_105762-2020.jpg?t=st=1645280814~exp=1645281414~hmac=e335375a645fef4b8cf2ab4702b93c9a6b3e706e2f23cf0e3348c1cd9f48cc4c&w=826"
                            ),
                            PlaceType(
                                wrapped = Leisure.SWIMMING_POOL,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/swimming-pool-with-sea-views_74190-16276.jpg?t=st=1645280747~exp=1645281347~hmac=b7e46cb5713cf22d5fdb79f69732fd2accf2d17f59098411faece8c11b5d9a85&w=826"
                            ),
                            PlaceType(
                                wrapped = Leisure.GOLF_COURSE,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/man-practicing-golf-field_23-2148822941.jpg?t=st=1645280697~exp=1645281297~hmac=8985e3de545bdfc7057c039cba02178a5df2ec8e0f04ed0d8cf58ffd9ae1e93e&w=826"
                            )
                        )
                ),
                PlaceTypeGroup(
                    name = "Health",
                    placeTypes =
                        listOf(
                            PlaceType(
                                wrapped = Amenity.PHARMACY,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/closeup-view-pharmacist-hand-taking-medicine-box-from-shelf-drug-store_342744-320.jpg?t=st=1645280158~exp=1645280758~hmac=2e1c1925fd6f7527e9402559a107b85011ae6fc481978dc64e3397a0242e5d31&w=826"
                            ),
                            PlaceType(
                                wrapped = Amenity.HOSPITAL,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/hospital-room-interior_181624-34428.jpg?t=st=1645280630~exp=1645281230~hmac=9508a017f6064c61d66ea75379925bd78917c3d9ec03e0b9a101d98401089e6a&w=826"
                            ),
                            PlaceType(
                                wrapped = Amenity.DOCTORS,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/hands-unrecognizable-female-doctor-writing-form-typing-laptop-keyboard_1098-20374.jpg?t=st=1645280202~exp=1645280802~hmac=bbf2a67fdbbf2751b914923fd4fddbc404ce53b2b4d30e6a3b39856f3378696e&w=826"
                            ),
                            PlaceType(
                                wrapped = Amenity.VETERINARY,
                                imageUrl =
                                    "https://img.freepik.com/free-photo/doctor-testing-animal-with-stethoscope_329181-10392.jpg?t=st=1645280665~exp=1645281265~hmac=93bf478908768b20fcf964dd5436233f984ff5cade989c4bcc2d764e350fd0bf&w=826"
                            ),
                        )
                )
            )
    }
}
