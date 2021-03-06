package com.lookaround.ui.camera.model

import androidx.camera.view.PreviewView

sealed class CameraIntent {
    object CameraViewCreated : CameraIntent()
    object LocationPermissionGranted : CameraIntent()
    object LocationPermissionDenied : CameraIntent()
    object CameraPermissionDenied : CameraIntent()
    data class CameraStreamStateChanged(val streamState: PreviewView.StreamState) : CameraIntent()
}
