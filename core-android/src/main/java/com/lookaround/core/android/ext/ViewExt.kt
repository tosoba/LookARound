package com.lookaround.core.android.ext

import android.view.View
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
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

fun View.toggleVisibility(): Int {
    visibility = if (visibility == View.GONE) View.VISIBLE else View.GONE
    return visibility
}

fun BoxedSeekbar.updateValueButtonsEnabled(points: Int = value, upBtn: View, downBtn: View) {
    upBtn.isEnabled = points < max
    downBtn.isEnabled = points > min
}

fun BoxedSeekbar.setValueButtonsOnClickListeners(upBtn: View, downBtn: View) {
    upBtn.setOnClickListener { ++value }
    downBtn.setOnClickListener { --value }
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
                })
        })
}
