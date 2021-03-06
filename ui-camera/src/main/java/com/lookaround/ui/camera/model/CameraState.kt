package com.lookaround.ui.camera.model

import android.location.Location
import android.os.Parcelable
import com.lookaround.core.android.model.Empty
import com.lookaround.core.android.model.Loadable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CameraState(
    val locationState: Loadable<Location> = Empty,
    val cameraPreviewState: CameraPreviewState = CameraPreviewState.Initial
) : Parcelable
