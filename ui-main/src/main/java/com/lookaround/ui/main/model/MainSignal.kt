package com.lookaround.ui.main.model

sealed class MainSignal {
    object UnableToLoadPlacesWithoutLocation : MainSignal()
    data class TopFragmentChanged(val cameraObscured: Boolean) : MainSignal()
}
