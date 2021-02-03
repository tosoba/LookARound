package com.lookaround.ui.map

sealed class MapIntent {
    data class LoadingScene(val scene: MapScene) : MapIntent()
    object SceneLoaded : MapIntent()
}
