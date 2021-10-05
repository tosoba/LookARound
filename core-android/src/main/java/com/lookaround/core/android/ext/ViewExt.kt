package com.lookaround.core.android.ext

import android.view.View
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.viewpager.widget.ViewPager
import biz.laenger.android.vpbs.ViewPagerBottomSheetBehavior
import com.facebook.shimmer.ShimmerFrameLayout
import com.lookaround.core.android.view.BoxedSeekbar
import java.lang.IllegalArgumentException

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

fun ViewPager.setupBottomSheetBehavior(): ViewPagerBottomSheetBehavior<View> {
    bottomSheetParentView?.let { parentView ->
        val behavior = ViewPagerBottomSheetBehavior.from(parentView)
        val invalidate =
            ViewPagerBottomSheetBehavior::class.java.getDeclaredMethod("invalidateScrollingChild")
        invalidate.isAccessible = true
        addOnPageChangeListener(
            object : ViewPager.SimpleOnPageChangeListener() {
                override fun onPageSelected(position: Int) {
                    post { invalidate.invoke(behavior) }
                }
            })
        return behavior
    }
        ?: throw IllegalArgumentException()
}

private val View.bottomSheetParentView: View?
    get() {
        var current: View? = this
        while (current != null) {
            val params = current.layoutParams
            if (params is CoordinatorLayout.LayoutParams &&
                params.behavior is ViewPagerBottomSheetBehavior<*>) {
                return current
            }
            val parent = current.parent
            current = if (parent == null || parent !is View) null else parent
        }
        return null
    }
