package com.lookaround.core.android.ext

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import com.facebook.shimmer.ShimmerFrameLayout

fun View.fadeOut(duration: Long = 500L) {
    animate()
        .setDuration(duration)
        .alpha(0f)
        .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                visibility = View.GONE
            }
        })
}

fun View.fadeIn(duration: Long = 500L) {
    alpha = 0f
    visibility = View.VISIBLE
    animate().setDuration(duration).alpha(1f)
}

fun ShimmerFrameLayout.showAndStart() {
    visibility = View.VISIBLE
    startShimmer()
}

fun ShimmerFrameLayout.stopAndHide() {
    stopShimmer()
    visibility = View.GONE
}
