package com.lookaround.ui.recent.searches

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Chip
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lookaround.core.android.model.WithValue
import com.lookaround.core.android.view.theme.LookARoundTheme
import com.lookaround.core.android.view.theme.Ocean8
import com.lookaround.core.ext.titleCaseWithSpacesInsteadOfUnderscores
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@ExperimentalCoroutinesApi
@ExperimentalMaterialApi
@Composable
fun RecentSearchesChipList(
    modifier: Modifier = Modifier,
    recentSearchesViewModel: RecentSearchesViewModel = hiltViewModel(),
    onMoreClicked: () -> Unit,
    onSearchClicked: (RecentSearchModel) -> Unit
) {
    val recentSearchesFlow = remember {
        recentSearchesViewModel
            .states
            .map { (searches) ->
                if (searches is WithValue) searches.value.take(10) else emptyList()
            }
            .distinctUntilChanged()
    }

    val recentSearches = recentSearchesFlow.collectAsState(initial = emptyList())
    if (recentSearches.value.isEmpty()) return

    val lazyListState = rememberLazyListState()
    LazyRow(state = lazyListState, modifier = modifier) {
        item { Box(modifier = Modifier.size(10.dp)) }
        items(recentSearches.value) { recentSearch ->
            Chip(
                onClick = { onSearchClicked(recentSearch) },
                colors = ChipDefaults.chipColors(backgroundColor = Ocean8),
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                Text(
                    text = recentSearch.label.titleCaseWithSpacesInsteadOfUnderscores,
                    color = LookARoundTheme.colors.textLink
                )
            }
        }
        item {
            Chip(
                onClick = onMoreClicked,
                colors = ChipDefaults.chipColors(backgroundColor = Ocean8),
                modifier = Modifier.padding(horizontal = 2.dp)
            ) { Text(text = "More...", color = LookARoundTheme.colors.textLink) }
        }
        item { Box(modifier = Modifier.size(10.dp)) }
    }
}
