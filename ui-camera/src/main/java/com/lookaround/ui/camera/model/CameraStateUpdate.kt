package com.lookaround.ui.camera.model

import androidx.camera.view.PreviewView
import com.lookaround.core.android.base.arch.StateUpdate
import com.lookaround.core.android.exception.LocationDisabledException
import com.lookaround.core.android.exception.LocationPermissionDeniedException
import com.lookaround.core.android.model.*

sealed class CameraStateUpdate : StateUpdate<CameraState> {
    data class LocationLoaded(val location: android.location.Location) : CameraStateUpdate() {
        override fun invoke(state: CameraState): CameraState =
            state.copy(locationState = Ready(location))
    }

    object LoadingLocation : CameraStateUpdate() {
        override fun invoke(state: CameraState): CameraState =
            state.copy(
                locationState =
                    if (state.locationState is WithValue) LoadingNext(state.locationState.value)
                    else LoadingFirst
            )
    }

    object LocationPermissionDenied : CameraStateUpdate() {
        override fun invoke(state: CameraState): CameraState =
            state.copy(
                locationState = state.locationState.copyWithError(LocationPermissionDeniedException)
            )
    }

    object LocationDisabled : CameraStateUpdate() {
        override fun invoke(state: CameraState): CameraState =
            state.copy(
                locationState =
                    if (state.locationState is WithValue) {
                        FailedNext(state.locationState.value, LocationDisabledException)
                    } else {
                        FailedFirst(LocationDisabledException)
                    }
            )
    }

    object CameraViewCreated : CameraStateUpdate() {
        override fun invoke(state: CameraState): CameraState =
            state.copy(cameraPreviewState = CameraPreviewState.Initial)
    }

    object CameraPermissionDenied : CameraStateUpdate() {
        override fun invoke(state: CameraState): CameraState =
            state.copy(cameraPreviewState = CameraPreviewState.PermissionDenied)
    }

    data class CameraStreamStateChanged(val streamState: PreviewView.StreamState) :
        CameraStateUpdate() {
        override fun invoke(state: CameraState): CameraState =
            state.copy(cameraPreviewState = CameraPreviewState.Active(streamState))
    }
}
