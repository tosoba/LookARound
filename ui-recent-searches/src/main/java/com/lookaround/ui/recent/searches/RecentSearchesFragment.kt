package com.lookaround.ui.recent.searches

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.CircularProgressIndicator
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
import com.lookaround.core.android.view.composable.LookARoundCard
import com.lookaround.core.android.view.composable.PlaceItemNameText
import com.lookaround.core.android.view.theme.LookARoundTheme
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.model.MainSignal
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map

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

            setContent {
                LookARoundTheme {
                    val bottomSheetState =
                        bottomSheetSignalsFlow.collectAsState(
                                initial = BottomSheetBehavior.STATE_HIDDEN
                            )
                            .value
                    if (bottomSheetState == BottomSheetBehavior.STATE_HIDDEN) return@LookARoundTheme

                    val recentSearches = recentSearchesFlow.collectAsState(initial = Empty).value
                    if (recentSearches is LoadingFirst) CircularProgressIndicator()
                    if (recentSearches !is WithValue) return@LookARoundTheme

                    val lazyListState = rememberLazyListState()
                    LazyColumn(state = lazyListState) {
                        item { Spacer(Modifier.height(112f.dp)) }
                        items(recentSearches.value) {
                            LookARoundCard(
                                backgroundColor = Color.White.copy(alpha = .5f),
                                elevation = 0.dp,
                                modifier = Modifier.clickable {}
                            ) {
                                Column {
                                    PlaceItemNameText(it.label, modifier = Modifier.padding(5.dp))
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
