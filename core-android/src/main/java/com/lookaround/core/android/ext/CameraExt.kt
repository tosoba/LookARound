package com.lookaround.core.android.ext

import android.content.Context
import android.util.DisplayMetrics
import android.view.ViewStub
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.lookaround.core.android.camera.OpenGLRenderer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

fun aspectRatio(width: Int, height: Int): Int {
    val previewRatio = max(width, height).toDouble() / min(width, height)
    return if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
        AspectRatio.RATIO_4_3
    } else {
        AspectRatio.RATIO_16_9
    }
}

private const val RATIO_4_3_VALUE = 4.0 / 3.0
private const val RATIO_16_9_VALUE = 16.0 / 9.0

fun Context.initCamera(
    lifecycleOwner: LifecycleOwner,
    openGLRenderer: OpenGLRenderer,
    cameraPreviewStub: ViewStub
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
    cameraProviderFuture.addListener(
        {
            val metrics = DisplayMetrics().also(cameraPreviewStub.display::getRealMetrics)
            val preview =
                Preview.Builder()
                    .setTargetAspectRatio(aspectRatio(metrics.widthPixels, metrics.heightPixels))
                    .setTargetRotation(cameraPreviewStub.display.rotation)
                    .build()
            openGLRenderer.attachInputPreview(preview, cameraPreviewStub)
            cameraProviderFuture
                .get()
                .bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
        },
        ContextCompat.getMainExecutor(this))
}
