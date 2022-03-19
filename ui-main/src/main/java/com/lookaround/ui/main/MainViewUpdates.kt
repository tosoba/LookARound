package com.lookaround.ui.main

import android.location.Location
import androidx.annotation.StringRes
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.imxie.exvpbs.ViewPagerBottomSheetBehavior
import com.lookaround.core.android.architecture.ListFragmentHost
import com.lookaround.core.android.model.*
import com.lookaround.ui.main.model.MainSignal
import com.lookaround.ui.main.model.MainState
import java.util.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

@FlowPreview
@ExperimentalCoroutinesApi
val MainViewModel.locationUpdateFailureUpdates: Flow<Throwable?>
    get() =
        mapStates(MainState::locationState)
            .filterIsInstance<Failed<Location>>()
            .map(Failed<Location>::error::get)

@FlowPreview
@ExperimentalCoroutinesApi
val MainViewModel.placesBottomNavItemVisibilityUpdates: Flow<Boolean>
    get() = states.map { it.markers.hasValue }.distinctUntilChanged()

@FlowPreview
@ExperimentalCoroutinesApi
val MainViewModel.recentSearchesBottomNavItemVisibilityUpdates: Flow<Boolean>
    get() = states.map { it.recentSearchesCount > 0 }.distinctUntilChanged()

@FlowPreview
@ExperimentalCoroutinesApi
val MainViewModel.locationReadyUpdates: Flow<Location>
    get() =
        mapStates(MainState::locationState)
            .filterIsInstance<WithValue<Location>>()
            .map(WithValue<Location>::value::get)
            .distinctUntilChangedBy { Objects.hash(it.latitude, it.longitude) }

@FlowPreview
@ExperimentalCoroutinesApi
val MainViewModel.markerUpdates: Flow<Loadable<ParcelableSortedSet<Marker>>>
    get() = mapStates(MainState::markers).distinctUntilChanged()

@FlowPreview
@ExperimentalCoroutinesApi
val MainViewModel.nearMeFabVisibilityUpdates: Flow<Boolean>
    get() =
        combine(
                filterSignals(MainSignal.BottomSheetStateChanged::state),
                filterSignals(MainSignal.SnackbarStatusChanged::isShowing).onStart { emit(false) },
                states.map { it.markers.hasNoValue }
            ) { sheetState, isSnackbarShowing, noMarkers ->
                noMarkers &&
                    !isSnackbarShowing &&
                    sheetState != ViewPagerBottomSheetBehavior.STATE_EXPANDED &&
                    sheetState != ViewPagerBottomSheetBehavior.STATE_DRAGGING &&
                    sheetState != ViewPagerBottomSheetBehavior.STATE_SETTLING
            }
            .distinctUntilChanged()

@FlowPreview
@ExperimentalCoroutinesApi
val MainViewModel.snackbarUpdates: Flow<SnackbarUpdate>
    get() =
        merge(
                markerUpdates
                    .map { loadable ->
                        when (loadable) {
                            is Loading -> {
                                SnackbarUpdate.Show(
                                    R.string.loading_places_in_progress,
                                    Snackbar.LENGTH_INDEFINITE
                                )
                            }
                            is Ready -> SnackbarUpdate.Dismiss
                            else -> null
                        }
                    }
                    .filterNotNull(),
                signals.filterIsInstance<MainSignal.PlacesLoadingFailed>().map {
                    SnackbarUpdate.Show(R.string.loading_places_failed, Snackbar.LENGTH_SHORT)
                },
                signals.filterIsInstance<MainSignal.UnableToLoadPlacesWithoutLocation>().map {
                    SnackbarUpdate.Show(R.string.location_unavailable, Snackbar.LENGTH_SHORT)
                },
                signals.filterIsInstance<MainSignal.UnableToLoadPlacesWithoutConnection>().map {
                    SnackbarUpdate.Show(R.string.no_internet_connection, Snackbar.LENGTH_SHORT)
                },
                signals.filterIsInstance<MainSignal.NoPlacesFound>().map {
                    SnackbarUpdate.Show(R.string.no_places_found, Snackbar.LENGTH_SHORT)
                }
            )
            .debounce(250L)

sealed interface SnackbarUpdate {
    data class Show(
        @StringRes val msgRes: Int,
        @BaseTransientBottomBar.Duration val length: Int,
    ) : SnackbarUpdate

    object Dismiss : SnackbarUpdate
}

@FlowPreview
@ExperimentalCoroutinesApi
val MainViewModel.listFragmentItemBackgroundUpdates: Flow<ListFragmentHost.ItemBackground>
    get() =
        filterSignals<MainSignal.TopFragmentChanged>()
            .map { (cameraObscured) ->
                if (cameraObscured) ListFragmentHost.ItemBackground.OPAQUE
                else ListFragmentHost.ItemBackground.TRANSPARENT
            }
            .distinctUntilChanged()
