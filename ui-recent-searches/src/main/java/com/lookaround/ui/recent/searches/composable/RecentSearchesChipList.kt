package com.lookaround.ui.recent.searches.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.lookaround.core.android.model.WithValue
import com.lookaround.core.android.view.composable.ChipList
import com.lookaround.ui.recent.searches.RecentSearchesViewModel
import com.lookaround.ui.recent.searches.model.RecentSearchModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@ExperimentalCoroutinesApi
@Composable
fun RecentSearchesChipList(
    modifier: Modifier = Modifier,
    chipModifier: Modifier = Modifier,
    recentSearchesViewModel: RecentSearchesViewModel = hiltViewModel(),
    onMoreClicked: () -> Unit,
    onItemClicked: (RecentSearchModel) -> Unit
) {
    val recentSearchesFlow = remember {
        recentSearchesViewModel
            .states
            .map { (searches) ->
                if (searches is WithValue) searches.value.take(10) else emptyList()
            }
            .distinctUntilChanged()
    }
    ChipList(
        itemsFlow = recentSearchesFlow,
        label = RecentSearchModel::label::get,
        modifier = modifier,
        chipModifier = chipModifier,
        onMoreClicked = onMoreClicked,
        onItemClicked = onItemClicked
    )
}
