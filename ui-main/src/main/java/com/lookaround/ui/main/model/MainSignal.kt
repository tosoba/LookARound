package com.lookaround.ui.main.model

import android.graphics.drawable.Drawable
import androidx.fragment.app.Fragment
import androidx.palette.graphics.Palette
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.imxie.exvpbs.ViewPagerBottomSheetBehavior
import com.lookaround.core.android.model.Marker
import java.util.*

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
    data class BlurBackgroundUpdated(val drawable: Drawable, val palette: Palette) : MainSignal
    data class DrawerToggled(val open: Boolean) : MainSignal
    data class ShowPlaceInBottomSheet(val id: UUID) : MainSignal
    object HidePlacesListBottomSheet : MainSignal
    data class UpdateSelectedMarker(val marker: Marker) : MainSignal
    data class ShowPlaceFragment(val marker: Marker) : MainSignal
    data class ShowMapFragment(val marker: Marker? = null) : MainSignal
    data class PlaceListBottomSheetStateChanged(
        @BottomSheetBehavior.State val state: Int,
    ) : MainSignal
}
