package com.lookaround.ui.main.model

import com.google.android.material.bottomsheet.BottomSheetBehavior

sealed interface MainSignal {
    object UnableToLoadPlacesWithoutLocation : MainSignal

    object UnableToLoadPlacesWithoutConnection : MainSignal

    data class TopFragmentChanged(val cameraObscured: Boolean, val onResume: Boolean) : MainSignal

    data class BottomSheetStateChanged(@BottomSheetBehavior.State val state: Int) : MainSignal

    data class PlacesLoadingFailed(val throwable: Throwable) : MainSignal

    data class SnackbarStatusChanged(val isShowing: Boolean) : MainSignal
}
