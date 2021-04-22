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

fun View.slideUp(duration: Long = 250L, toYDelta: Float = -250f) {
    startAnimation(
        TranslateAnimation(0f, 0f, 0f, toYDelta).also {
            it.duration = duration
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

fun View.slideDown(duration: Long = 250L, fromYDelta: Float = -250f) {
    visibility = View.VISIBLE
    startAnimation(TranslateAnimation(0f, 0f, fromYDelta, 0f).also { it.duration = duration })
}
