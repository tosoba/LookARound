package com.lookaround.core.android.ext

import android.util.DisplayMetrics
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import timber.log.Timber

fun PreviewView.init(
    lifecycleOwner: LifecycleOwner,
    onError: (Exception) -> Unit = { Timber.e(it, "PreviewView initialization error.") }
) {
    post {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                try {
                    val cameraSelector =
                        CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                            .build()

                    val metrics = DisplayMetrics().also(display::getRealMetrics)
                    val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
                    val rotation = display.rotation

                    val preview =
                        Preview.Builder()
                            .setTargetAspectRatio(screenAspectRatio)
                            .setTargetRotation(rotation)
                            .build()

                    val imageCapture =
                        ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .setTargetAspectRatio(screenAspectRatio)
                            .setTargetRotation(rotation)
                            .build()

                    val imageAnalyzer =
                        ImageAnalysis.Builder()
                            .setTargetAspectRatio(screenAspectRatio)
                            .setTargetRotation(rotation)
                            .build()

                    cameraProviderFuture
                        .get()
                        .bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture,
                            imageAnalyzer
                        )

                    preview.setSurfaceProvider(surfaceProvider)
                } catch (ex: Exception) {
                    onError(ex)
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }
}

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
