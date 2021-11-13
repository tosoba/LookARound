package com.lookaround.core.android.ext

import android.annotation.SuppressLint
import android.content.Context
import android.media.Image
import android.util.Size
import androidx.camera.core.*
import androidx.camera.core.impl.ImageOutputConfig.RotationValue
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import timber.log.Timber

@SuppressLint("UnsafeOptInUsageError")
suspend fun Context.initCamera(
    lifecycleOwner: LifecycleOwner,
    @RotationValue rotation: Int,
    screenSize: Size,
): CameraInitializationResult = suspendCoroutine { continuation ->
    val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
    cameraProviderFuture.addListener(
        {
            val preview =
                Preview.Builder()
                    .setTargetAspectRatio(aspectRatio(screenSize.width, screenSize.height))
                    .setTargetRotation(rotation)
                    .build()
            val imageAnalysis =
                ImageAnalysis.Builder()
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .setTargetResolution(screenSize / 10)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
            val imageFlow = MutableSharedFlow<Image>()
            imageAnalysis.setAnalyzer(
                Dispatchers.Default.asExecutor(),
                { imageProxy ->
                    imageProxy.use {
                        val image = imageProxy.image
                        if (image != null) {
                            lifecycleOwner.lifecycleScope.launchWhenResumed {
                                imageFlow.emit(image)
                            }
                        } else {
                            Timber.d("Analyzed image is null")
                        }
                    }
                }
            )
            try {
                val camera =
                    cameraProviderFuture
                        .get()
                        .bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                continuation.resume(CameraInitializationResult(preview, camera, imageFlow))
            } catch (ex: Exception) {
                continuation.resumeWithException(ex)
            }
        },
        ContextCompat.getMainExecutor(this)
    )
}

data class CameraInitializationResult(
    val preview: Preview,
    val camera: Camera,
    val imageFlow: Flow<Image>
)

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
