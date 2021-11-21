package com.lookaround.core.android.map.scene.model

sealed interface MapSceneIntent {
    data class LoadingScene(val scene: MapScene) : MapSceneIntent
    object SceneLoaded : MapSceneIntent, (MapSceneState) -> MapSceneState {
        override fun invoke(state: MapSceneState): MapSceneState =
            state.copy(sceneLoaded = true, sceneLoadingTimeoutOccurred = false)
    }
}
