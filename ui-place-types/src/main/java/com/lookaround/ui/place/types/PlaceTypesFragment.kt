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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
            val placeTypesFlow =
                searchQueryFlow
                    .map { it.trim().lowercase() }
                    .distinctUntilChanged()
                    .map { query ->
                        allPlaceTypes.filter { placeType ->
                            placeType.wrapped.label.lowercase().contains(query) ||
                                placeType.wrapped.description.lowercase().contains(query)
                        }
                    }
                    .distinctUntilChanged()

            setContent {
                ProvideWindowInsets {
                    LookARoundTheme {
                        val bottomSheetState =
                            bottomSheetSignalsFlow.collectAsState(
                                    initial = BottomSheetBehavior.STATE_HIDDEN
                                )
                                .value

                        val searchQuery = searchQueryFlow.collectAsState(initial = "")
                        val searchFocused = rememberSaveable { mutableStateOf(false) }
                        val placeTypes = placeTypesFlow.collectAsState(initial = allPlaceTypes)

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
                            itemsIndexed(
                                listOf(
                                    PlaceTypeGroup(name = "General", placeTypes = placeTypes.value),
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

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(SavedStateKey.SEARCH_QUERY.name, searchQuery)
    }

    private enum class SavedStateKey {
        SEARCH_QUERY
    }

    companion object {
        private val allPlaceTypes =
            listOf(
                PlaceType(
                    wrapped = Amenity.PARKING,
                    imageUrl = "https://source.unsplash.com/UsSdMZ78Q3E"
                ),
                PlaceType(
                    wrapped = Amenity.RESTAURANT,
                    imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                ),
                PlaceType(
                    wrapped = Amenity.FUEL,
                    imageUrl = "https://source.unsplash.com/_jk8KIyN_uA"
                ),
                PlaceType(
                    wrapped = Amenity.BANK,
                    imageUrl = "https://source.unsplash.com/UsSdMZ78Q3E"
                )
            )
    }
}
