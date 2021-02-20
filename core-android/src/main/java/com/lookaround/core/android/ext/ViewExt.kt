package com.lookaround.core.android.ext

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import com.facebook.shimmer.ShimmerFrameLayout

fun View.fadeOut() {
    animate()
        .setDuration(500L)
        .alpha(0f)
        .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                visibility = View.GONE
            }
        })
}

fun ShimmerFrameLayout.showAndStart() {
    visibility = View.VISIBLE
    startShimmer()
}

fun ShimmerFrameLayout.stopAndHide() {
    stopShimmer()
    visibility = View.GONE
}
