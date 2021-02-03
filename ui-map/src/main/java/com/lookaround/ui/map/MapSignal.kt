package com.lookaround.ui.map

sealed class MapSignal {
    object RetryLoadScene : MapSignal()
}
