package com.lookaround.ui.camera.model

sealed class CameraSignal {
    object CameraTouch : CameraSignal()
    data class PitchChanged(val withinLimit: Boolean) : CameraSignal()
}
