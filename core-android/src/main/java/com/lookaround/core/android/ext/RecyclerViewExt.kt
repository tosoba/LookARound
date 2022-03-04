package com.lookaround.core.android.ext

import android.view.View
import androidx.recyclerview.widget.RecyclerView

fun RecyclerView.addCollapseTopViewOnScrollListener(topView: View) {
    addOnScrollListener(
        object : RecyclerView.OnScrollListener() {
            var verticalOffset = 0

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE && verticalOffset > topView.height) {
                    verticalOffset = topView.height
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                verticalOffset += dy
                val toolbarYOffset = dy - topView.translationY
                topView.translationY =
                    if (dy > 0) {
                        if (toolbarYOffset < topView.height) -toolbarYOffset
                        else -topView.height.toFloat()
                    } else {
                        if (toolbarYOffset < 0) 0f else -toolbarYOffset
                    }
            }
        }
    )
}
