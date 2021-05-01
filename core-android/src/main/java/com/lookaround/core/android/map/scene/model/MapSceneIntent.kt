package com.lookaround.core.android.map.scene.model

sealed class MapSceneIntent {
    data class LoadingScene(val scene: MapScene) : MapSceneIntent()
    object SceneLoaded : MapSceneIntent()
}
