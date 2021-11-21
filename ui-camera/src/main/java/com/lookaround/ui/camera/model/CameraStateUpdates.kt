package com.lookaround.ui.camera.model

data class CameraPreviewStateUpdate(
    private val previewState: CameraPreviewState,
) : (CameraState) -> CameraState {
    override fun invoke(state: CameraState): CameraState = CameraState(previewState = previewState)
}
