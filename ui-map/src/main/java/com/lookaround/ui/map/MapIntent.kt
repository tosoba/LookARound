package com.lookaround.ui.map

import com.lookaround.core.android.map.MapScene

sealed class MapIntent {
    data class LoadingScene(val scene: MapScene) : MapIntent()
    object SceneLoaded : MapIntent()
}
