package com.lookaround.ui.camera.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CameraState(
    val previewState: CameraPreviewState = CameraPreviewState.Initial,
    val firstMarkerIndex: Int = 0,
    val radarEnlarged: Boolean = false,
) : Parcelable
