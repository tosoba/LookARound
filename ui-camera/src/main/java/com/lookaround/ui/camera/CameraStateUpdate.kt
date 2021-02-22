package com.lookaround.ui.camera

import com.lookaround.core.android.base.arch.StateUpdate

sealed class CameraStateUpdate : StateUpdate<CameraState> {
    data class Location(val location: android.location.Location) : CameraStateUpdate() {
        override fun invoke(state: CameraState): CameraState = state.copy(location = location)
    }
}
