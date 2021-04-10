package com.lookaround.ui.camera.model

enum class CameraRenderDistance(val meters: Int, val label: String) {
    _100_M(100, "100 m"),
    _250_M(250, "250 m"),
    _500_M(500, "500 m"),
    _1_KM(1_000, "1 km"),
    _2_KM(2_000, "2 km"),
    _5_KM(5_000, "5 km")
}
