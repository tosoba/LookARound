package com.lookaround.ui.map

import com.lookaround.core.android.map.MapScene

sealed class MapSignal {
    data class RetryLoadScene(val scene: MapScene) : MapSignal()
}
