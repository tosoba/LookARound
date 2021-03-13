package com.lookaround.ui.main.model

import com.lookaround.core.model.IPlaceType

sealed class MainIntent {
    data class LoadPlaces(val type: IPlaceType) : MainIntent()
}
