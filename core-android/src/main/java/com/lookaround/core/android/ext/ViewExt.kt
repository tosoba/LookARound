package com.lookaround.core.android.ext

import android.animation.Animator
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.View
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import androidx.annotation.IntRange
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.facebook.shimmer.ShimmerFrameLayout
import com.lookaround.core.android.R

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

fun View.slideSetVisibility(
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

fun View.fadeSetVisibility(visibility: Int) {
    animate()
        .alpha(if (visibility == View.GONE) 0f else 1f)
        .setListener(
            object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) = Unit
                override fun onAnimationEnd(animation: Animator?) {
                    this@fadeSetVisibility.visibility = visibility
                }
                override fun onAnimationCancel(animation: Animator?) = Unit
                override fun onAnimationRepeat(animation: Animator?) = Unit
            }
        )
}

fun View.setListBackgroundItemDrawableWith(
    contrastingColor: Int,
    @IntRange(from = 0x0, to = 0xFF) alpha: Int = 0x30
) {
    val backgroundDrawable =
        ResourcesCompat.getDrawable(resources, R.drawable.rounded_elevated_background, null) as
            LayerDrawable
    val backgroundLayer =
        backgroundDrawable.findDrawableByLayerId(
            R.id.rounded_transparent_shadow_background_layer
        ) as
            GradientDrawable
    backgroundLayer.color =
        ColorStateList.valueOf(ColorUtils.setAlphaComponent(contrastingColor, alpha))
    background = backgroundDrawable
}

fun ViewPager2.disableNestedScrolling() {
    (getChildAt(0) as? RecyclerView)?.apply {
        isNestedScrollingEnabled = false
        overScrollMode = View.OVER_SCROLL_NEVER
    }
        ?: throw IllegalStateException("First child of the ViewPager is not of type RecyclerView.")
}
