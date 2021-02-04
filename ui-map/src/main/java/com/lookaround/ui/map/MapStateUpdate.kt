package com.lookaround.ui.map

import com.lookaround.core.android.base.arch.StateUpdate

sealed class MapStateUpdate : StateUpdate<MapState> {
    data class LoadingScene(val scene: MapScene) : MapStateUpdate() {
        override fun invoke(state: MapState): MapState = state.copy(
            scene = scene,
            sceneLoaded = false,
            sceneLoadingTimeoutOccurred = false
        )
    }

    object SceneLoaded : MapStateUpdate() {
        override fun invoke(state: MapState): MapState = state.copy(
            sceneLoaded = true,
            sceneLoadingTimeoutOccurred = false
        )
    }

    object SceneLoadingTimeoutOccurred : MapStateUpdate() {
        override fun invoke(state: MapState): MapState = MapState(
            sceneLoadingTimeoutOccurred = true
        )
    }
}
