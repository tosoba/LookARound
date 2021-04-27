package com.lookaround.ui.map

import android.os.Parcelable
import com.lookaround.core.android.map.MapScene
import kotlinx.parcelize.Parcelize

@Parcelize
data class MapState(
    val scene: MapScene = MapScene.BUBBLE_WRAP,
    val sceneLoaded: Boolean = false,
    val sceneLoadingTimeoutOccurred: Boolean = false
) : Parcelable
