package com.lookaround.ui.camera.model

import com.lookaround.core.android.base.arch.StateUpdate

sealed class CameraStateUpdate : StateUpdate<CameraState> {
    data class CameraPreviewStateUpdate(
        private val previewState: CameraPreviewState,
    ) : CameraStateUpdate() {
        override fun invoke(state: CameraState): CameraState =
            CameraState(previewState = previewState)
    }

    data class CameraMarkersFirstIndexChanged(val difference: Int) : CameraStateUpdate() {
        override fun invoke(state: CameraState): CameraState =
            state.copy(firstMarkerIndex = state.firstMarkerIndex + difference)
    }

    object ToggleRadarEnlarged : CameraStateUpdate() {
        override fun invoke(state: CameraState): CameraState =
            state.copy(radarEnlarged = !state.radarEnlarged)
    }
}
