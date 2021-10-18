package com.lookaround.ui.main.model

import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.lookaround.core.model.IPlaceType

sealed class MainIntent {
    data class LoadPlaces(val type: IPlaceType) : MainIntent()

    object LocationPermissionGranted : MainIntent()
    object LocationPermissionDenied : MainIntent()

    data class LiveBottomSheetStateChanged(
        @BottomSheetBehavior.State val state: Int,
    ) : MainIntent()

    data class SearchQueryChanged(val query: String) : MainIntent()
    data class SearchFocusChanged(val focused: Boolean) : MainIntent()
}
