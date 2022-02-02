package com.lookaround.ui.main.model

import com.google.android.material.bottomsheet.BottomSheetBehavior

sealed interface MainSignal {
    object UnableToLoadPlacesWithoutLocation : MainSignal
    object UnableToLoadPlacesWithoutConnection : MainSignal
    data class TopFragmentChanged(val cameraObscured: Boolean, val onResume: Boolean) : MainSignal
    data class BottomSheetStateChanged(@BottomSheetBehavior.State val state: Int) : MainSignal
    object HideBottomSheet : MainSignal
    data class PlacesLoadingFailed(val throwable: Throwable) : MainSignal
    data class SnackbarStatusChanged(val isShowing: Boolean) : MainSignal
    object ARLoading : MainSignal
    object AREnabled : MainSignal
    object ARDisabled : MainSignal
    data class ToggleSearchBarVisibility(val targetVisibility: Int) : MainSignal
    object NoPlacesFound : MainSignal
}
