package com.lookaround.ui.map

sealed class MapSignal {
    data class RetryLoadScene(val scene: MapScene) : MapSignal()
}
