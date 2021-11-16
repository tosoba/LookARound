package com.lookaround.core.android.map.scene.model

import com.lookaround.core.android.base.arch.StateUpdate

sealed interface MapSceneStateUpdate : StateUpdate<MapSceneState> {
    data class LoadingScene(val scene: MapScene) : MapSceneStateUpdate {
        override fun invoke(state: MapSceneState): MapSceneState =
            state.copy(scene = scene, sceneLoaded = false, sceneLoadingTimeoutOccurred = false)
    }

    object SceneLoaded : MapSceneStateUpdate {
        override fun invoke(state: MapSceneState): MapSceneState =
            state.copy(sceneLoaded = true, sceneLoadingTimeoutOccurred = false)
    }

    object SceneLoadingTimeoutOccurred : MapSceneStateUpdate {
        override fun invoke(state: MapSceneState): MapSceneState =
            MapSceneState(sceneLoadingTimeoutOccurred = true)
    }
}
