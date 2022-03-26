package com.lookaround.core.android.map.scene.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MapSceneState(
    val scene: MapScene = MapScene.DARK,
    val sceneLoaded: Boolean = false,
    val sceneLoadingTimeoutOccurred: Boolean = false
) : Parcelable
