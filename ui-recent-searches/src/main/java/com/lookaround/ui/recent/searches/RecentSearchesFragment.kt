package com.lookaround.ui.recent.searches

import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.github.marlonlom.utilities.timeago.TimeAgo
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.lookaround.core.android.model.*
import com.lookaround.core.android.view.composable.*
import com.lookaround.core.android.view.theme.LookARoundTheme
import com.lookaround.core.ext.titleCaseWithSpacesInsteadOfUnderscores
import com.lookaround.core.model.SearchType
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.model.MainIntent
import com.lookaround.ui.main.model.MainSignal
import com.lookaround.ui.main.model.MainState
import com.lookaround.ui.recent.searches.databinding.FragmentRecentSearchesBinding
import com.lookaround.ui.recent.searches.model.RecentSearchModel
import com.lookaround.ui.recent.searches.model.RecentSearchesIntent
import com.lookaround.ui.recent.searches.model.RecentSearchesState
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import java.util.*
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

    private val recentSearchesViewModel: RecentSearchesViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val bottomSheetSignalsFlow =
            mainViewModel
                .filterSignals(MainSignal.BottomSheetStateChanged::state)
                .onStart { emit(mainViewModel.state.lastLiveBottomSheetState) }
                .distinctUntilChanged()
        val locationFlow =
            mainViewModel
                .mapStates(MainState::locationState)
                .filterIsInstance<WithValue<Location>>()
        val recentSearchesFlow =
            recentSearchesViewModel
                .mapStates(RecentSearchesState::searches)
                .filterIsInstance<WithValue<ParcelableList<RecentSearchModel>>>()
                .map(WithValue<ParcelableList<RecentSearchModel>>::value::get)
                .distinctUntilChanged()
        val emptyRecentSearchesFlow =
            recentSearchesViewModel
                .states
                .map { it.searches.hasNoValueOrEmpty() }
                .distinctUntilChanged()

        binding.recentSearchesList.setContent {
            ProvideWindowInsets {
                LookARoundTheme {
                    val scope = rememberCoroutineScope()

                    val emptyRecentSearches =
                        emptyRecentSearchesFlow.collectAsState(initial = false)
                    if (emptyRecentSearches.value) {
                        LaunchedEffect(true) { mainViewModel.signal(MainSignal.HideBottomSheet) }
                    }

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
            backgroundColor = Color.White.copy(alpha = .85f),
            elevation = 0.dp,
            modifier =
                Modifier.padding(10.dp)
                    .fillMaxWidth()
                    .border(
                        width = 2.dp,
                        brush =
                            Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, Color.LightGray)
                            ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        lifecycleScope.launch {
                            mainViewModel.intent(
                                when (recentSearch.type) {
                                    SearchType.AROUND ->
                                        MainIntent.LoadSearchAroundResults(recentSearch.id)
                                    SearchType.AUTOCOMPLETE ->
                                        MainIntent.LoadSearchAutocompleteResults(recentSearch.id)
                                }
                            )
                            mainViewModel.signal(MainSignal.HideBottomSheet)
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
