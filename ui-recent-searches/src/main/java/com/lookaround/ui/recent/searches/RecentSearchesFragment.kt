package com.lookaround.ui.recent.searches

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.lookaround.core.android.ext.assistedActivityViewModel
import com.lookaround.core.android.ext.assistedViewModel
import com.lookaround.core.android.model.Empty
import com.lookaround.core.android.model.LoadingFirst
import com.lookaround.core.android.model.LoadingInProgress
import com.lookaround.core.android.model.WithValue
import com.lookaround.core.android.view.composable.InfiniteListHandler
import com.lookaround.core.android.view.composable.ItemDistanceText
import com.lookaround.core.android.view.composable.ItemNameText
import com.lookaround.core.android.view.composable.LookARoundCard
import com.lookaround.core.android.view.theme.LookARoundTheme
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.model.MainSignal
import com.lookaround.ui.main.model.MainState
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map

@AndroidEntryPoint
@WithFragmentBindings
@ExperimentalCoroutinesApi
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
                LookARoundTheme {
                    val bottomSheetState =
                        bottomSheetSignalsFlow.collectAsState(
                                initial = BottomSheetBehavior.STATE_HIDDEN
                            )
                            .value
                    if (bottomSheetState == BottomSheetBehavior.STATE_HIDDEN) return@LookARoundTheme

                    val locationState = locationFlow.collectAsState(initial = Empty).value
                    if (locationState !is WithValue) return@LookARoundTheme

                    val recentSearches = recentSearchesFlow.collectAsState(initial = Empty).value
                    if (recentSearches is LoadingFirst) CircularProgressIndicator()
                    if (recentSearches !is WithValue) return@LookARoundTheme

                    val lazyListState = rememberLazyListState()
                    LazyColumn(state = lazyListState) {
                        item { Spacer(Modifier.height(112.dp)) }
                        items(recentSearches.value) { recentSearch ->
                            LookARoundCard(
                                backgroundColor = Color.White.copy(alpha = .75f),
                                elevation = 0.dp,
                                modifier = Modifier.padding(10.dp).fillMaxWidth().clickable {}
                            ) {
                                Column {
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
                                    if (recentSearch.location != null) {
                                        Row(
                                            modifier = Modifier.padding(5.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            ItemDistanceText(
                                                location1 = locationState.value,
                                                location2 = recentSearch.location
                                            )
                                            when (recentSearch.type) {
                                                RecentSearchModel.Type.AROUND ->
                                                    Icon(
                                                        imageVector = Icons.Outlined.Category,
                                                        contentDescription = "Place type search"
                                                    )
                                                RecentSearchModel.Type.AUTOCOMPLETE ->
                                                    Icon(
                                                        imageVector = Icons.Outlined.Search,
                                                        contentDescription = "Text search"
                                                    )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (recentSearches is LoadingInProgress) {
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
