package com.lookaround.core.android.map.scene.model

sealed class MapSceneSignal {
    data class RetryLoadScene(val scene: MapScene) : MapSceneSignal()
}
