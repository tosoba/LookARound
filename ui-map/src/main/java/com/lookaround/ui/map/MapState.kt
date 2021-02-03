package com.lookaround.ui.map

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class MapState(
    val scene: MapScene = MapScene.BUBBLE_WRAP,
    val sceneLoaded: Boolean = false
) : Parcelable
