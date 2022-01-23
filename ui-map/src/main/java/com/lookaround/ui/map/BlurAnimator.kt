package com.lookaround.ui.map

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
    private val context: Context,
    bitmapToBlur: Bitmap,
    private val initialRadius: Int,
    private val targetRadius: Int,
    private val durationMs: Long = 2_000L,
    private val sampleFactor: Float = 8f,
    private val scheme: Int = HokoBlur.SCHEME_RENDER_SCRIPT,
    private val mode: Int = HokoBlur.MODE_GAUSSIAN,
) {
    private val mutableAnimationStates = MutableStateFlow(AnimationState(bitmapToBlur, true))
    val animationStates: Flow<AnimationState>
        get() = mutableAnimationStates

    private val processor: BlurProcessor =
        HokoBlur.with(context).sampleFactor(sampleFactor).scheme(scheme).mode(mode).processor()

    private var animationJob: Job? = null
    private var animator: ValueAnimator? = null
    private var currentRadius: Int = initialRadius

    val animationType: AnimationType
        get() = if (initialRadius < targetRadius) AnimationType.BLUR else AnimationType.REVERSE_BLUR

    var isAnimating = false
        private set

    fun animateIn(scope: CoroutineScope) {
        if (targetRadius == currentRadius) return
        isAnimating = true
        animator =
            ValueAnimator.ofInt(
                    (currentRadius.toFloat() / targetRadius.toFloat() * 1_000).toInt(),
                    if (targetRadius > currentRadius) 1_000 else 0
                )
                .apply {
                    interpolator = LinearInterpolator()
                    duration = durationMs
                    addUpdateListener {
                        currentRadius = (it.animatedValue as Int) / 1_000 * targetRadius
                        animationJob =
                            scope.launch(Dispatchers.Default) {
                                val animationState = mutableAnimationStates.value
                                if (!animationState.inProgress) return@launch
                                val bitmap = animationState.blurredBitmap
                                if (!bitmap.isRecycled) {
                                    processor.radius(currentRadius)
                                    val blurred = processor.blur(bitmap)
                                    mutableAnimationStates.value = AnimationState(blurred, true)
                                }
                            }
                    }
                    addListener(
                        object : Animator.AnimatorListener {
                            fun complete() {
                                mutableAnimationStates.value =
                                    AnimationState(
                                        mutableAnimationStates.value.blurredBitmap,
                                        false
                                    )
                                isAnimating = false
                            }

                            override fun onAnimationEnd(animation: Animator?) {
                                complete()
                            }

                            override fun onAnimationCancel(animation: Animator?) {
                                complete()
                            }

                            override fun onAnimationStart(animation: Animator?) = Unit
                            override fun onAnimationRepeat(animation: Animator?) = Unit
                        }
                    )
                    start()
                }
    }

    fun cancel() {
        if (animator?.isStarted == true) animator?.end()
        animationJob?.cancel()
        animationJob = null
    }

    fun reversed(): BlurAnimator =
        BlurAnimator(
            context,
            mutableAnimationStates.value.blurredBitmap,
            initialRadius = currentRadius,
            targetRadius = targetRadius,
            sampleFactor = sampleFactor,
            scheme = scheme,
            mode = mode
        )

    fun saveInstanceState(bundle: Bundle) {
        bundle.putParcelable(
            SAVED_STATE_KEY,
            SavedState(
                isAnimating = isAnimating,
                targetRadius = targetRadius,
                currentRadius = currentRadius,
                sampleFactor = sampleFactor,
                scheme = scheme,
                mode = mode
            )
        )
    }

    class AnimationState(val blurredBitmap: Bitmap, val inProgress: Boolean)

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
                .apply { isAnimating = savedState.isAnimating }
        }
    }
}
