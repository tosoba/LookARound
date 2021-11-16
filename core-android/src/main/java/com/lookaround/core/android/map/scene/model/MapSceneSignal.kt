package com.lookaround.core.android.map.scene.model

sealed interface MapSceneSignal {
    data class RetryLoadScene(val scene: MapScene) : MapSceneSignal
}
