package com.lookaround.ui.camera.model

import androidx.camera.view.PreviewView
import com.lookaround.core.android.base.arch.StateUpdate

sealed class CameraStateUpdate : StateUpdate<CameraState> {
    object CameraViewCreated : CameraStateUpdate() {
        override fun invoke(state: CameraState): CameraState =
            state.copy(previewState = CameraPreviewState.Initial)
    }

    data class CameraStreamStateChanged(
        val streamState: PreviewView.StreamState,
    ) : CameraStateUpdate() {
        override fun invoke(state: CameraState): CameraState =
            state.copy(previewState = CameraPreviewState.Active(streamState))
    }

    object CameraPermissionDenied : CameraStateUpdate() {
        override fun invoke(state: CameraState): CameraState =
            state.copy(previewState = CameraPreviewState.PermissionDenied)
    }

    data class CameraMarkersFirstIndexChanged(val difference: Int) : CameraStateUpdate() {
        override fun invoke(state: CameraState): CameraState =
            state.copy(firstMarkerIndex = state.firstMarkerIndex + difference)
    }
}
