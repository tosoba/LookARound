package com.lookaround.ui.recent.searches

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.marlonlom.utilities.timeago.TimeAgo
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.lookaround.core.android.ext.assistedActivityViewModel
import com.lookaround.core.android.ext.assistedViewModel
import com.lookaround.core.android.model.Empty
import com.lookaround.core.android.model.Loading
import com.lookaround.core.android.model.LoadingFirst
import com.lookaround.core.android.model.WithValue
import com.lookaround.core.android.view.composable.*
import com.lookaround.core.android.view.theme.LookARoundTheme
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.model.MainIntent
import com.lookaround.ui.main.model.MainSignal
import com.lookaround.ui.main.model.MainState
import com.lookaround.ui.search.composable.SearchBar
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@AndroidEntryPoint
@WithFragmentBindings
@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
@FlowPreview
class RecentSearchesFragment : Fragment() {
    @Inject internal lateinit var recentSearchesViewModelFactory: RecentSearchesViewModel.Factory
    private val recentSearchesViewModel: RecentSearchesViewModel by assistedViewModel {
        recentSearchesViewModelFactory.create(it)
    }

    @Inject internal lateinit var mainViewModelFactory: MainViewModel.Factory
    private val mainViewModel: MainViewModel by assistedActivityViewModel {
        mainViewModelFactory.create(it)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        ComposeView(requireContext()).apply {
            val recentSearchesFlow =
                recentSearchesViewModel.states.map(RecentSearchesState::searches::get)
            val bottomSheetSignalsFlow =
                mainViewModel
                    .signals
                    .filterIsInstance<MainSignal.BottomSheetStateChanged>()
                    .map(MainSignal.BottomSheetStateChanged::state::get)
            val locationFlow =
                mainViewModel
                    .states
                    .map(MainState::locationState::get)
                    .filterIsInstance<WithValue<Location>>()

            setContent {
                ProvideWindowInsets {
                    LookARoundTheme {
                        val bottomSheetState =
                            bottomSheetSignalsFlow.collectAsState(
                                    initial = BottomSheetBehavior.STATE_HIDDEN
                                )
                                .value
                        if (bottomSheetState == BottomSheetBehavior.STATE_HIDDEN)
                            return@LookARoundTheme

                        val locationState = locationFlow.collectAsState(initial = Empty).value
                        if (locationState !is WithValue) return@LookARoundTheme

                        val recentSearches =
                            recentSearchesFlow.collectAsState(initial = Empty).value
                        if (recentSearches is LoadingFirst) CircularProgressIndicator()
                        if (recentSearches !is WithValue) return@LookARoundTheme

                        val lazyListState = rememberLazyListState()
                        LazyColumn(state = lazyListState) {
                            if (bottomSheetState != BottomSheetBehavior.STATE_EXPANDED) {
                                item { Spacer(Modifier.height(112.dp)) }
                            } else {
                                // TODO: maybe have it in a constraint layout (a compose one) above
                                // the list -> test how this variant will work when list is
                                // scrolling (in PlaceList)
                                stickyHeader {
                                    SearchBar(
                                        query = "",
                                        searchFocused = false,
                                        onBackPressedDispatcher =
                                            requireActivity().onBackPressedDispatcher,
                                        onSearchFocusChange = { focused -> },
                                        onTextValueChange = { textValue -> }
                                    )
                                }
                            }
                            items(recentSearches.value) { recentSearch ->
                                RecentSearchItem(recentSearch, locationState.value)
                            }
                            if (recentSearches is Loading) {
                                item { CircularProgressIndicator() }
                            }
                        }

                        InfiniteListHandler(listState = lazyListState) {
                            recentSearchesViewModel.intent(RecentSearchesIntent.IncreaseLimit)
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
                                RecentSearchModel.Type.AROUND ->
                                    MainIntent.LoadSearchAroundResults(recentSearch.id)
                                RecentSearchModel.Type.AUTOCOMPLETE ->
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
                    ItemNameText(
                        name =
                            recentSearch.label.replaceFirstChar {
                                if (it.isLowerCase()) {
                                    it.titlecase(Locale.getDefault())
                                } else {
                                    it.toString()
                                }
                            },
                        modifier = Modifier.padding(5.dp)
                    )
                    when (recentSearch.type) {
                        RecentSearchModel.Type.AROUND ->
                            Icon(
                                imageVector = Icons.Outlined.Category,
                                contentDescription = "Place type search",
                                modifier =
                                    Modifier.defaultMinSize(minWidth = 10.dp, minHeight = 10.dp)
                                        .padding(5.dp)
                            )
                        RecentSearchModel.Type.AUTOCOMPLETE ->
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Text search",
                                modifier =
                                    Modifier.defaultMinSize(minWidth = 10.dp, minHeight = 10.dp)
                                        .padding(5.dp)
                            )
                    }
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
