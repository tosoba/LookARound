package com.lookaround.ui.camera.model

import androidx.camera.view.PreviewView

sealed class CameraIntent {
    object CameraViewCreated : CameraIntent()
    data class CameraStreamStateChanged(val streamState: PreviewView.StreamState) : CameraIntent()
    object CameraPermissionDenied : CameraIntent()
    data class CameraMarkersFirstIndexChanged(val difference: Int) : CameraIntent()
}
