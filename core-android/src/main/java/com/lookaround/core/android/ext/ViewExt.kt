package com.lookaround.core.android.ext

import android.view.View
import com.facebook.shimmer.ShimmerFrameLayout
import com.lookaround.core.android.view.BoxedSeekbar

fun ShimmerFrameLayout.showAndStart() {
    visibility = View.VISIBLE
    startShimmer()
}

fun ShimmerFrameLayout.stopAndHide() {
    stopShimmer()
    visibility = View.GONE
}

fun View.toggleVisibility() {
    visibility = if (visibility == View.GONE) View.VISIBLE else View.GONE
}

fun BoxedSeekbar.updateValueButtonsEnabled(points: Int = value, upBtn: View, downBtn: View) {
    upBtn.isEnabled = points < max
    downBtn.isEnabled = points > min
}

fun BoxedSeekbar.setValueButtonsOnClickListeners(upBtn: View, downBtn: View) {
    upBtn.setOnClickListener { ++value }
    downBtn.setOnClickListener { --value }
}
