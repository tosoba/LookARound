package com.lookaround.core.android.ext

import android.view.View
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.facebook.shimmer.ShimmerFrameLayout

fun ShimmerFrameLayout.showAndStart() {
    visibility = View.VISIBLE
    startShimmer()
}

fun ShimmerFrameLayout.stopAndHide() {
    stopShimmer()
    visibility = View.GONE
}

fun View.toggleVisibility(): Int {
    visibility = if (visibility == View.GONE) View.VISIBLE else View.GONE
    return visibility
}

fun View.slideChangeVisibility(
    targetVisibility: Int,
    fromXDelta: Float = 0f,
    toXDelta: Float = 0f,
    fromYDelta: Float = 0f,
    toYDelta: Float = 0f,
    duration: Long = 250L
) {
    if (visibility == targetVisibility) return
    if (targetVisibility == View.VISIBLE) visibility = View.VISIBLE
    startAnimation(
        TranslateAnimation(fromXDelta, toXDelta, fromYDelta, toYDelta).also {
            it.duration = duration
            if (targetVisibility == View.VISIBLE) return@also
            it.setAnimationListener(
                object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation) = Unit
                    override fun onAnimationEnd(animation: Animation) {
                        visibility = View.GONE
                    }
                    override fun onAnimationRepeat(animation: Animation) = Unit
                }
            )
        }
    )
}

fun ViewPager2.disableNestedScrolling() {
    (getChildAt(0) as? RecyclerView)?.apply {
        isNestedScrollingEnabled = false
        overScrollMode = View.OVER_SCROLL_NEVER
    }
        ?: throw IllegalStateException("First child of the ViewPager is not of type RecyclerView.")
}
