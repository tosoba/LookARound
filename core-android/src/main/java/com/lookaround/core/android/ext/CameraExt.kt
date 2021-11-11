package com.lookaround.core.android.ext

import android.content.Context
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.impl.ImageOutputConfig.RotationValue
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

suspend fun Context.initCamera(
    lifecycleOwner: LifecycleOwner,
    @RotationValue rotation: Int,
    widthPx: Int,
    heightPx: Int
): Preview = suspendCoroutine { continuation ->
    val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
    cameraProviderFuture.addListener(
        {
            val preview =
                Preview.Builder()
                    .setTargetAspectRatio(aspectRatio(widthPx, heightPx))
                    .setTargetRotation(rotation)
                    .build()
            try {
                cameraProviderFuture
                    .get()
                    .bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
                continuation.resume(preview)
            } catch (ex: Exception) {
                continuation.resumeWithException(ex)
            }
        },
        ContextCompat.getMainExecutor(this)
    )
}

private fun aspectRatio(width: Int, height: Int): Int {
    val previewRatio = max(width, height).toDouble() / min(width, height)
    return if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
        AspectRatio.RATIO_4_3
    } else {
        AspectRatio.RATIO_16_9
    }
}

private const val RATIO_4_3_VALUE = 4.0 / 3.0
private const val RATIO_16_9_VALUE = 16.0 / 9.0
