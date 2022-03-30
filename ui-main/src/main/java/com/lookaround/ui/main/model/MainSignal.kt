package com.lookaround.ui.main.model

import android.graphics.drawable.Drawable
import androidx.fragment.app.Fragment
import com.imxie.exvpbs.ViewPagerBottomSheetBehavior

sealed interface MainSignal {
    object UnableToLoadPlacesWithoutLocation : MainSignal
    object UnableToLoadPlacesWithoutConnection : MainSignal
    data class TopFragmentChanged(val fragmentClass: Class<out Fragment>) : MainSignal
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
    data class BlurBackgroundUpdated(val drawable: Drawable) : MainSignal
    data class DrawerToggled(val open: Boolean) : MainSignal
    object ShowPlaceListBottomSheet : MainSignal
    object HidePlaceListBottomSheet : MainSignal
}
