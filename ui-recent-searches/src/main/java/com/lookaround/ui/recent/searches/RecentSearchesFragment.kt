package com.lookaround.ui.recent.searches

import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.github.marlonlom.utilities.timeago.TimeAgo
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.lookaround.core.android.ext.assistedActivityViewModel
import com.lookaround.core.android.ext.assistedViewModel
import com.lookaround.core.android.model.*
import com.lookaround.core.android.view.composable.*
import com.lookaround.core.android.view.theme.LookARoundTheme
import com.lookaround.core.ext.titleCaseWithSpacesInsteadOfUnderscores
import com.lookaround.core.model.SearchType
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.bottomSheetStateUpdates
import com.lookaround.ui.main.model.MainIntent
import com.lookaround.ui.main.model.MainState
import com.lookaround.ui.recent.searches.databinding.FragmentRecentSearchesBinding
import com.lookaround.ui.search.composable.SearchBar
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@AndroidEntryPoint
@WithFragmentBindings
@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
@FlowPreview
class RecentSearchesFragment : Fragment(R.layout.fragment_recent_searches) {
    private val binding: FragmentRecentSearchesBinding by
        viewBinding(FragmentRecentSearchesBinding::bind)

    @Inject internal lateinit var recentSearchesViewModelFactory: RecentSearchesViewModel.Factory
    private val recentSearchesViewModel: RecentSearchesViewModel by assistedViewModel {
        recentSearchesViewModelFactory.create(it)
    }

    @Inject internal lateinit var mainViewModelFactory: MainViewModel.Factory
    private val mainViewModel: MainViewModel by assistedActivityViewModel {
        mainViewModelFactory.create(it)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val bottomSheetSignalsFlow =
            mainViewModel
                .bottomSheetStateUpdates
                .onStart { emit(mainViewModel.state.lastLiveBottomSheetState) }
                .distinctUntilChanged()
        val locationFlow =
            mainViewModel
                .states
                .map(MainState::locationState::get)
                .filterIsInstance<WithValue<Location>>()
        val recentSearchesFlow =
            recentSearchesViewModel
                .states
                .map(RecentSearchesState::searches::get)
                .filterIsInstance<WithValue<ParcelableList<RecentSearchModel>>>()
                .map(WithValue<ParcelableList<RecentSearchModel>>::value::get)
                .distinctUntilChanged()

        binding.recentSearchesList.setContent {
            ProvideWindowInsets {
                LookARoundTheme {
                    val scope = rememberCoroutineScope()

                    val bottomSheetState =
                        bottomSheetSignalsFlow.collectAsState(
                            initial = BottomSheetBehavior.STATE_HIDDEN
                        )

                    val locationState = locationFlow.collectAsState(initial = Empty).value
                    if (locationState !is WithValue) return@LookARoundTheme

                    val recentSearches = recentSearchesFlow.collectAsState(initial = emptyList())

                    val searchQuery = rememberSaveable { mutableStateOf("") }
                    val searchFocused = rememberSaveable { mutableStateOf(false) }

                    val lazyListState = rememberLazyListState()
                    binding
                        .disallowInterceptTouchContainer
                        .shouldRequestDisallowInterceptTouchEvent =
                        (lazyListState.firstVisibleItemIndex != 0 ||
                            lazyListState.firstVisibleItemScrollOffset != 0) &&
                            bottomSheetState.value == BottomSheetBehavior.STATE_EXPANDED
                    LazyColumn(state = lazyListState) {
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
                                        searchQuery.value = it.text
                                        scope.launch {
                                            recentSearchesViewModel.intent(
                                                RecentSearchesIntent.LoadSearches(it.text)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                        items(recentSearches.value) { recentSearch ->
                            RecentSearchItem(recentSearch, locationState.value)
                        }
                    }

                    InfiniteListHandler(listState = lazyListState) {
                        recentSearchesViewModel.intent(
                            RecentSearchesIntent.LoadSearches(searchQuery.value)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun RecentSearchItem(recentSearch: RecentSearchModel, location: Location) {
        LookARoundCard(
            backgroundColor = Color.White.copy(alpha = .75f),
            elevation = 0.dp,
            modifier =
                Modifier.padding(10.dp).fillMaxWidth().clickable {
                    lifecycleScope.launch {
                        mainViewModel.intent(
                            when (recentSearch.type) {
                                SearchType.AROUND ->
                                    MainIntent.LoadSearchAroundResults(recentSearch.id)
                                SearchType.AUTOCOMPLETE ->
                                    MainIntent.LoadSearchAutocompleteResults(recentSearch.id)
                            }
                        )
                    }
                }
        ) {
            Column {
                Row(
                    modifier = Modifier.padding(5.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    when (recentSearch.type) {
                        SearchType.AROUND -> {
                            Icon(
                                imageVector = Icons.Outlined.Category,
                                contentDescription = "Place type search",
                                modifier =
                                    Modifier.defaultMinSize(minWidth = 10.dp, minHeight = 10.dp)
                                        .padding(5.dp)
                            )
                        }
                        SearchType.AUTOCOMPLETE -> {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Text search",
                                modifier =
                                    Modifier.defaultMinSize(minWidth = 10.dp, minHeight = 10.dp)
                                        .padding(5.dp)
                            )
                        }
                    }
                    ItemNameText(
                        name = recentSearch.label.titleCaseWithSpacesInsteadOfUnderscores,
                        modifier = Modifier.padding(5.dp)
                    )
                    IconButton(
                        onClick = {
                            lifecycleScope.launch {
                                recentSearchesViewModel.intent(
                                    RecentSearchesIntent.DeleteSearch(
                                        recentSearch.id,
                                        recentSearch.type
                                    )
                                )
                            }
                            Toast.makeText(
                                    requireContext(),
                                    getString(R.string.recent_search_deleted),
                                    Toast.LENGTH_SHORT
                                )
                                .show()
                        }
                    ) { Icon(painterResource(id = R.drawable.ic_baseline_delete_24), "") }
                }
                if (recentSearch.location != null) {
                    Row(
                        modifier = Modifier.padding(5.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ItemDistanceText(
                            location1 = location,
                            location2 = recentSearch.location,
                            modifier = Modifier.padding(5.dp)
                        )
                        InfoItemText(
                            text = TimeAgo.using(recentSearch.lastSearchedAt.time),
                            color = LookARoundTheme.colors.textSecondary,
                            modifier =
                                Modifier.heightIn(min = 16.dp).wrapContentHeight().padding(5.dp)
                        )
                    }
                }
            }
        }
    }
}
