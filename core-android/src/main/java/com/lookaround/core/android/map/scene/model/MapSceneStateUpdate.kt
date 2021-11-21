package com.lookaround.core.android.map.scene.model

data class LoadingSceneUpdate(val scene: MapScene) : (MapSceneState) -> MapSceneState {
    override fun invoke(state: MapSceneState): MapSceneState =
        state.copy(scene = scene, sceneLoaded = false, sceneLoadingTimeoutOccurred = false)
}

object SceneLoadingTimeoutUpdate : (MapSceneState) -> MapSceneState {
    override fun invoke(state: MapSceneState): MapSceneState =
        MapSceneState(sceneLoadingTimeoutOccurred = true)
}
