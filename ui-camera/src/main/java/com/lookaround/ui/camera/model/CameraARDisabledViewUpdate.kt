package com.lookaround.ui.camera.model

internal data class CameraARDisabledViewUpdate(
    val anyPermissionDenied: Boolean,
    val locationDisabled: Boolean,
    val pitchOutsideRequiredLimit: Boolean,
    val cameraInitializationFailure: Boolean,
)
