package com.lookaround.core.android.view.composable

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun InfiniteListHandler(listState: LazyListState, buffer: Int = 2, onLoadMore: suspend() -> Unit) {
    val loadMore: State<Boolean> = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
            lastVisibleItemIndex > totalItemsNumber - buffer
        }
    }
    LaunchedEffect(loadMore) {
        snapshotFlow(loadMore::value).distinctUntilChanged().collect { onLoadMore() }
    }
}
