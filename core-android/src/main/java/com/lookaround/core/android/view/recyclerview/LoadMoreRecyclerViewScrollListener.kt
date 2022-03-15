package com.lookaround.core.android.view.recyclerview

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LoadMoreRecyclerViewScrollListener(
    private val visibleThreshold: Int = 4,
    private val loadMore: () -> Unit,
) : RecyclerView.OnScrollListener() {
    private var previousTotal = 0
    private var isLoading = true

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        val visibleItemCount = recyclerView.childCount
        val totalItemCount = layoutManager.itemCount
        val firstVisibleItemIndex = layoutManager.findFirstVisibleItemPosition()
        if (isLoading) {
            if (totalItemCount > previousTotal) {
                isLoading = false
                previousTotal = totalItemCount
            }
        } else if (totalItemCount - visibleItemCount <= firstVisibleItemIndex + visibleThreshold) {
            isLoading = true
            loadMore()
        }
    }
}
