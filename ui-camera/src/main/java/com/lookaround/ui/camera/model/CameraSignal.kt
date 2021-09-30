package com.lookaround.ui.camera.model

sealed class CameraSignal {
    object CameraTouch : CameraSignal()
}
