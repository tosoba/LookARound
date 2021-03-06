package com.lookaround.core.android.ext

import android.view.View
import com.facebook.shimmer.ShimmerFrameLayout

fun ShimmerFrameLayout.showAndStart() {
    visibility = View.VISIBLE
    startShimmer()
}

fun ShimmerFrameLayout.stopAndHide() {
    stopShimmer()
    visibility = View.GONE
}
