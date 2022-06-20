package com.lookaround.core.android.ext

import android.animation.Animator
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.View
import android.view.ViewTreeObserver.OnPreDrawListener
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import androidx.annotation.ColorRes
import androidx.annotation.IntRange
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
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
    if (this.visibility == visibility) return
    animate()
        .setDuration(250L)
        .alpha(if (visibility == View.GONE) 0f else 1f)
        .setListener(
            object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) {
                    if (visibility == View.VISIBLE) {
                        alpha = 0f
                        this@fadeSetVisibility.visibility = visibility
                    }
                }
                override fun onAnimationEnd(animation: Animator?) {
                    if (visibility == View.GONE) {
                        this@fadeSetVisibility.visibility = visibility
                    }
                    alpha = 1f
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
        ResourcesCompat.getDrawable(resources, R.drawable.rounded_elevated_background, null)
            as LayerDrawable
    val backgroundLayer =
        backgroundDrawable.findDrawableByLayerId(R.id.rounded_transparent_shadow_background_layer)
            as GradientDrawable
    backgroundLayer.color =
        ColorStateList.valueOf(ColorUtils.setAlphaComponent(contrastingColor, alpha))
    background = backgroundDrawable
}

fun View.runOnPreDrawOnce(action: () -> Unit) {
    val preDrawListener: OnPreDrawListener =
        object : OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                viewTreeObserver.removeOnPreDrawListener(this)
                action()
                return true
            }
        }
    viewTreeObserver.addOnPreDrawListener(preDrawListener)
}

fun AlertDialog.setButtonsTextColor(@ColorRes colorRes: Int) {
    val color = ContextCompat.getColor(context, colorRes)
    getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color)
    getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color)
}
