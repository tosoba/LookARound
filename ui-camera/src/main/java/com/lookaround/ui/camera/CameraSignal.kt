package com.lookaround.ui.camera

sealed class CameraSignal {
    object LocationUnavailable : CameraSignal()
}
