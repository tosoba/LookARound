package com.lookaround.core.android.view

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Parcelable
import android.view.animation.LinearInterpolator
import com.hoko.blur.HokoBlur
import com.hoko.blur.processor.BlurProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class BlurAnimator(
    context: Context,
    initialBitmap: Bitmap,
    private val initialRadius: Int,
    private val targetRadius: Int,
    private val durationMs: Long = 500L,
    private val sampleFactor: Float = 8f,
    private val scheme: Int = HokoBlur.SCHEME_OPENGL,
    private val mode: Int = HokoBlur.MODE_GAUSSIAN,
) {
    private val mutableAnimationStates = MutableStateFlow(AnimationState(initialBitmap, true))
    val animationStates: Flow<AnimationState>
        get() = mutableAnimationStates

    private val processor: BlurProcessor =
        HokoBlur.with(context).sampleFactor(sampleFactor).scheme(scheme).mode(mode).processor()

    private var animationJob: Job? = null
    private var animator: ValueAnimator? = null
    private var currentRadius: Int = initialRadius

    var inProgress = false
        private set

    val animationType: AnimationType
        get() = if (initialRadius < targetRadius) AnimationType.BLUR else AnimationType.REVERSE_BLUR

    fun animateIn(scope: CoroutineScope) {
        if (targetRadius == currentRadius) return
        inProgress = true
        animator =
            ValueAnimator.ofInt(
                    (currentRadius.toFloat() / targetRadius.toFloat() * ANIMATION_UPDATES_COUNT)
                        .toInt(),
                    if (targetRadius > currentRadius) ANIMATION_UPDATES_COUNT else 0
                )
                .apply {
                    interpolator = LinearInterpolator()
                    duration = durationMs
                    addUpdateListener {
                        currentRadius =
                            (it.animatedValue as Int) / ANIMATION_UPDATES_COUNT * targetRadius
                        animationJob = scope.launch(Dispatchers.Default) { animateBlur() }
                    }
                    addListener(
                        object : Animator.AnimatorListener {
                            fun complete() {
                                mutableAnimationStates.value =
                                    AnimationState(
                                        mutableAnimationStates.value.currentBitmap,
                                        false
                                    )
                                inProgress = false
                            }

                            override fun onAnimationEnd(animation: Animator?) = complete()
                            override fun onAnimationCancel(animation: Animator?) = complete()
                            override fun onAnimationStart(animation: Animator?) = Unit
                            override fun onAnimationRepeat(animation: Animator?) = Unit
                        }
                    )
                    start()
                }
    }

    fun reversed(context: Context): BlurAnimator =
        BlurAnimator(
            context,
            mutableAnimationStates.value.currentBitmap,
            initialRadius = currentRadius,
            targetRadius = initialRadius,
            sampleFactor = sampleFactor,
            scheme = scheme,
            mode = mode
        )

    fun cancel() {
        if (animator?.isStarted == true) animator?.end()
        animationJob?.cancel()
        animationJob = null
    }

    fun saveInstanceState(bundle: Bundle) {
        bundle.putParcelable(
            SAVED_STATE_KEY,
            SavedState(
                isAnimating = inProgress,
                targetRadius = targetRadius,
                currentRadius = currentRadius,
                sampleFactor = sampleFactor,
                scheme = scheme,
                mode = mode
            )
        )
    }

    private fun animateBlur() {
        val (bitmap, inProgress) = mutableAnimationStates.value
        if (!inProgress || bitmap.isRecycled) return

        processor.radius(currentRadius)
        mutableAnimationStates.value = AnimationState(processor.blur(bitmap), true)
    }

    data class AnimationState(val currentBitmap: Bitmap, val inProgress: Boolean)

    @Parcelize
    private data class SavedState(
        val isAnimating: Boolean,
        val targetRadius: Int,
        val currentRadius: Int,
        val sampleFactor: Float,
        val scheme: Int,
        val mode: Int,
    ) : Parcelable

    enum class AnimationType {
        BLUR,
        REVERSE_BLUR
    }

    companion object {
        private val SAVED_STATE_KEY = BlurAnimator::class.java.name
        private const val ANIMATION_UPDATES_COUNT = 1_000

        fun fromSavedInstanceState(
            context: Context,
            bitmapToBlur: Bitmap,
            bundle: Bundle
        ): BlurAnimator? {
            val savedState = bundle.getParcelable<SavedState>(SAVED_STATE_KEY) ?: return null
            return BlurAnimator(
                    context,
                    bitmapToBlur,
                    initialRadius = savedState.currentRadius,
                    targetRadius = savedState.targetRadius,
                    sampleFactor = savedState.sampleFactor,
                    scheme = savedState.scheme,
                    mode = savedState.mode
                )
                .apply { inProgress = savedState.isAnimating }
        }
    }
}
