package com.lookaround.ui.camera.model

import android.os.Parcelable
import androidx.camera.view.PreviewView
import kotlinx.parcelize.Parcelize

sealed interface CameraPreviewState : Parcelable {
    @Parcelize object Initial : CameraPreviewState
    @Parcelize object PermissionDenied : CameraPreviewState
    @Parcelize object InitializationFailure : CameraPreviewState
    @Parcelize data class Active(val streamState: PreviewView.StreamState) : CameraPreviewState
}
