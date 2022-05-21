package com.lookaround.ui.camera.model

import androidx.camera.view.PreviewView

sealed interface CameraIntent {
    object CameraViewCreated : CameraIntent, (CameraState) -> CameraState {
        override fun invoke(state: CameraState): CameraState =
            state.copy(previewState = CameraPreviewState.Initial)
    }

    data class CameraStreamStateChanged(val streamState: PreviewView.StreamState) : CameraIntent

    object CameraPermissionDenied : CameraIntent

    object CameraInitializationFailed : CameraIntent

    data class CameraMarkersFirstIndexChanged(
        val difference: Int,
    ) : CameraIntent, (CameraState) -> CameraState {
        override fun invoke(state: CameraState): CameraState =
            state.copy(firstMarkerIndex = state.firstMarkerIndex + difference)
    }

    object ToggleRadarEnlarged : CameraIntent, (CameraState) -> CameraState {
        override fun invoke(state: CameraState): CameraState =
            state.copy(radarEnlarged = !state.radarEnlarged)
    }
}
