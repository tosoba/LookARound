package com.lookaround.ui.camera.model

sealed interface CameraSignal {
    object CameraTouch : CameraSignal
    data class PitchChanged(val withinLimit: Boolean) : CameraSignal
}
