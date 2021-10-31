package com.lookaround.ui.main.model

import com.google.android.material.bottomsheet.BottomSheetBehavior

sealed class MainSignal {
    object UnableToLoadPlacesWithoutLocation : MainSignal()

    data class TopFragmentChanged(
        val cameraObscured: Boolean,
        val onResume: Boolean,
    ) : MainSignal()

    data class BottomSheetStateChanged(@BottomSheetBehavior.State val state: Int) : MainSignal()

    data class PlacesLoadingFailed(val throwable: Throwable) : MainSignal()
}
