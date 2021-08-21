package com.lookaround.core.android.camera

import android.view.Surface
import androidx.annotation.IntDef

internal object Surfaces {
    const val ROTATION_0_DEG = 0
    const val ROTATION_90_DEG = 90
    const val ROTATION_180_DEG = 180
    const val ROTATION_270_DEG = 270

    @RotationDegrees
    fun toSurfaceRotationDegrees(@RotationEnum rotationEnum: Int): Int =
        when (rotationEnum) {
            Surface.ROTATION_0 -> ROTATION_0_DEG
            Surface.ROTATION_90 -> ROTATION_90_DEG
            Surface.ROTATION_180 -> ROTATION_180_DEG
            Surface.ROTATION_270 -> ROTATION_270_DEG
            else -> throw UnsupportedOperationException("Unsupported rotation enum: $rotationEnum")
        }

    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    @IntDef(ROTATION_0_DEG, ROTATION_90_DEG, ROTATION_180_DEG, ROTATION_270_DEG)
    annotation class RotationDegrees

    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    @IntDef(Surface.ROTATION_0, Surface.ROTATION_90, Surface.ROTATION_180, Surface.ROTATION_270)
    annotation class RotationEnum
}
