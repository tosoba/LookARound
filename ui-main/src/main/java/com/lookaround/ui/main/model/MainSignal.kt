package com.lookaround.ui.main.model

import android.graphics.Bitmap
import com.imxie.exvpbs.ViewPagerBottomSheetBehavior

sealed interface MainSignal {
    object UnableToLoadPlacesWithoutLocation : MainSignal
    object UnableToLoadPlacesWithoutConnection : MainSignal
    data class TopFragmentChanged(val cameraObscured: Boolean, val onResume: Boolean) : MainSignal
    data class BottomSheetStateChanged(
        @ViewPagerBottomSheetBehavior.State val state: Int,
    ) : MainSignal
    object HideBottomSheet : MainSignal
    data class PlacesLoadingFailed(val throwable: Throwable) : MainSignal
    data class SnackbarStatusChanged(val isShowing: Boolean) : MainSignal
    object ARLoading : MainSignal
    object AREnabled : MainSignal
    object ARDisabled : MainSignal
    data class ToggleSearchBarVisibility(val targetVisibility: Int) : MainSignal
    object NoPlacesFound : MainSignal
    data class ContrastingColorUpdated(val color: Int) : MainSignal
    data class BlurBackgroundUpdated(val bitmap: Bitmap) : MainSignal
}
