package com.lookaround.ui.recent.searches.composable

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lookaround.core.android.model.WithValue
import com.lookaround.core.android.view.recyclerview.ChipsRecyclerViewAdapter
import com.lookaround.core.ext.titleCaseWithSpacesInsteadOfUnderscores
import com.lookaround.ui.recent.searches.RecentSearchesViewModel
import com.lookaround.ui.recent.searches.model.RecentSearchModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@ExperimentalCoroutinesApi
@Composable
fun RecentSearchesChipList(
    modifier: Modifier = Modifier,
    recentSearchesViewModel: RecentSearchesViewModel = hiltViewModel(),
    displayedItemsLimit: Int = 10,
    onMoreClicked: () -> Unit,
    onItemClicked: (RecentSearchModel) -> Unit
) {
    val recentSearchesFlow = remember {
        recentSearchesViewModel
            .states
            .map { (searches) ->
                if (searches is WithValue) {
                    searches.value.take(displayedItemsLimit) to searches.value.size
                } else {
                    emptyList<RecentSearchModel>() to 0
                }
            }
            .distinctUntilChanged()
    }
    val recentSearchesState = recentSearchesFlow.collectAsState(emptyList<RecentSearchModel>() to 0)
    if (recentSearchesState.value.first.isEmpty()) return

    AndroidView(
        factory = { context ->
            RecyclerView(context).apply {
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            }
        },
        update = { recyclerView ->
            recyclerView.layoutManager =
                LinearLayoutManager(recyclerView.context, LinearLayoutManager.HORIZONTAL, false)
            val adapter =
                ChipsRecyclerViewAdapter(
                    recentSearchesState.value.first,
                    transparent = false,
                    label = { item -> item.label.titleCaseWithSpacesInsteadOfUnderscores },
                    onMoreClicked =
                        if (recentSearchesState.value.second > displayedItemsLimit) onMoreClicked
                        else null,
                    onItemClicked = onItemClicked,
                )
            recyclerView.adapter = adapter
        },
        modifier = modifier,
    )
}
