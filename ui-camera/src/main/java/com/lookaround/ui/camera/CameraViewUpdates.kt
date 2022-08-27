package com.lookaround.ui.camera

import androidx.camera.view.PreviewView
import com.imxie.exvpbs.ViewPagerBottomSheetBehavior
import com.lookaround.core.android.exception.GooglePayServicesNotAvailableException
import com.lookaround.core.android.exception.LocationDisabledException
import com.lookaround.core.android.exception.LocationPermissionDeniedException
import com.lookaround.core.android.model.*
import com.lookaround.core.ext.withLatestFrom
import com.lookaround.ui.camera.model.CameraPreviewState
import com.lookaround.ui.camera.model.CameraSignal
import com.lookaround.ui.camera.model.CameraState
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.model.MainSignal
import com.lookaround.ui.main.model.MainState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

@FlowPreview
@ExperimentalCoroutinesApi
internal fun arEnabledUpdates(
    mainViewModel: MainViewModel,
    cameraViewModel: CameraViewModel
): Flow<ARUpdate> =
    combine(
            mainViewModel
                .mapStates(MainState::locationState)
                .onStart { emit(mainViewModel.state.locationState) }
                .distinctUntilChanged(),
            cameraViewModel
                .mapStates(CameraState::previewState)
                .onStart { emit(cameraViewModel.state.previewState) }
                .distinctUntilChanged(),
            cameraViewModel
                .filterSignals(CameraSignal.PitchChanged::withinLimit)
                .onStart { emit(true) }
                .distinctUntilChanged(),
            cameraObscuredUpdates(mainViewModel, cameraViewModel)
                .onStart {
                    emit(
                        CameraObscuredUpdate(
                            obscuredByBottomSheet = false,
                            obscuredByDrawer = false
                        )
                    )
                }
                .distinctUntilChanged(),
            mainViewModel.states
                .map { it.markers.hasValue }
                .onStart { emit(mainViewModel.state.markers.hasValue) }
                .distinctUntilChanged()
        ) { locationState, previewState, pitchWithinLimit, obscuredUpdate, showingAnyMarkers ->
            ARUpdate.Enabled(
                enabled =
                    locationState is Ready &&
                        previewState.isLive &&
                        (!showingAnyMarkers || pitchWithinLimit) &&
                        !obscuredUpdate.obscured,
                showingAnyMarkers = showingAnyMarkers
            )
        }
        .filter(ARUpdate.Enabled::enabled::get)

sealed interface ARUpdate {
    object Loading : ARUpdate
    data class Enabled(val enabled: Boolean, val showingAnyMarkers: Boolean) : ARUpdate
}

private val CameraPreviewState.isLoading: Boolean
    get() =
        this is CameraPreviewState.Initial ||
            (this is CameraPreviewState.Active && streamState == PreviewView.StreamState.IDLE)

private val CameraPreviewState.isLive: Boolean
    get() = this is CameraPreviewState.Active && streamState == PreviewView.StreamState.STREAMING

@FlowPreview
@ExperimentalCoroutinesApi
internal fun loadingStartedUpdates(
    mainViewModel: MainViewModel,
    cameraViewModel: CameraViewModel
): Flow<ARUpdate> =
    mainViewModel
        .mapStates(MainState::locationState)
        .combine(cameraViewModel.mapStates(CameraState::previewState)) { locationState, previewState
            ->
            locationState !is Failed &&
                previewState !is CameraPreviewState.InitializationFailure &&
                (locationState is Loading || previewState.isLoading)
        }
        .distinctUntilChanged()
        .filter { it }
        .map { ARUpdate.Loading }

@FlowPreview
@ExperimentalCoroutinesApi
internal fun arDisabledUpdates(
    mainViewModel: MainViewModel,
    cameraViewModel: CameraViewModel
): Flow<ARDisabledViewUpdate> =
    combine(
            mainViewModel
                .mapStates(MainState::locationState)
                .onStart { emit(mainViewModel.state.locationState) }
                .distinctUntilChanged(),
            cameraViewModel
                .mapStates(CameraState::previewState)
                .onStart { emit(cameraViewModel.state.previewState) }
                .distinctUntilChanged(),
            cameraViewModel.filterSignals(CameraSignal.PitchChanged::withinLimit).onStart {
                emit(true)
            },
            cameraObscuredUpdates(mainViewModel, cameraViewModel)
                .onStart {
                    emit(
                        CameraObscuredUpdate(
                            obscuredByBottomSheet = false,
                            obscuredByDrawer = false,
                        )
                    )
                }
                .distinctUntilChanged(),
            mainViewModel.states
                .map { it.markers.hasValue }
                .onStart { emit(mainViewModel.state.markers.hasValue) }
                .distinctUntilChanged()
        ) { locationState, previewState, pitchWithinLimit, obscuredUpdate, showingAnyMarkers ->
            ARDisabledViewUpdate(
                anyPermissionDenied =
                    locationState.isFailedWith<LocationPermissionDeniedException>() ||
                        previewState is CameraPreviewState.PermissionDenied,
                googlePlayServicesNotAvailable =
                    locationState.isFailedWith<GooglePayServicesNotAvailableException>(),
                locationDisabled = locationState.isFailedWith<LocationDisabledException>(),
                pitchOutsideRequiredLimit =
                    !pitchWithinLimit && !obscuredUpdate.obscured && showingAnyMarkers,
                cameraInitializationFailure =
                    previewState is CameraPreviewState.InitializationFailure
            )
        }
        .distinctUntilChanged()
        .filter(ARDisabledViewUpdate::isDisabled::get)

internal data class ARDisabledViewUpdate(
    val anyPermissionDenied: Boolean,
    val googlePlayServicesNotAvailable: Boolean,
    val locationDisabled: Boolean,
    val pitchOutsideRequiredLimit: Boolean,
    val cameraInitializationFailure: Boolean,
) {
    val isDisabled: Boolean
        get() =
            anyPermissionDenied ||
                googlePlayServicesNotAvailable ||
                locationDisabled ||
                pitchOutsideRequiredLimit ||
                cameraInitializationFailure
}

@FlowPreview
@ExperimentalCoroutinesApi
internal fun cameraObscuredUpdates(
    mainViewModel: MainViewModel,
    cameraViewModel: CameraViewModel
): Flow<CameraObscuredUpdate> =
    combine(
            mainViewModel.filterSignals(MainSignal.BottomSheetStateChanged::state).onStart {
                emit(mainViewModel.state.lastLiveBottomSheetState)
            },
            mainViewModel.mapStates(MainState::drawerOpen),
            cameraViewModel
                .mapStates(CameraState::previewState)
                .filter(CameraPreviewState::isLive::get),
        ) { sheetState, drawerOpen, _ ->
            CameraObscuredUpdate(
                obscuredByBottomSheet = sheetState != ViewPagerBottomSheetBehavior.STATE_HIDDEN,
                obscuredByDrawer = drawerOpen,
            )
        }
        .distinctUntilChanged()

@FlowPreview
@ExperimentalCoroutinesApi
internal fun cameraViewObscuredUpdates(
    mainViewModel: MainViewModel,
    cameraViewModel: CameraViewModel
): Flow<CameraObscuredViewUpdate> =
    cameraObscuredUpdates(mainViewModel, cameraViewModel)
        .withLatestFrom(mainViewModel.states.map { it.markers.hasValue }) {
            obscuredUpdate,
            showingAnyMarkers ->
            CameraObscuredViewUpdate(obscuredUpdate.obscured, showingAnyMarkers)
        }
        .debounce(500L)

internal data class CameraObscuredUpdate(
    val obscuredByBottomSheet: Boolean,
    val obscuredByDrawer: Boolean
) {
    val obscured: Boolean
        get() = obscuredByBottomSheet || obscuredByDrawer
}

internal data class CameraObscuredViewUpdate(
    val obscured: Boolean,
    val showingAnyMarkers: Boolean,
)

@FlowPreview
@ExperimentalCoroutinesApi
internal fun cameraTouchUpdates(
    mainViewModel: MainViewModel,
    cameraViewModel: CameraViewModel
): Flow<Unit> =
    cameraViewModel
        .filterSignals<CameraSignal.CameraTouch>()
        .filter {
            mainViewModel.state.locationState is Ready && cameraViewModel.state.previewState.isLive
        }
        .map {}
        .debounce(250L)

@FlowPreview
@ExperimentalCoroutinesApi
internal fun markerUpdates(
    mainViewModel: MainViewModel,
    cameraViewModel: CameraViewModel
): Flow<Pair<Loadable<ParcelableSortedSet<Marker>>, Int>> =
    mainViewModel
        .mapStates(MainState::markers)
        .combine(cameraViewModel.mapStates(CameraState::firstMarkerIndex)) {
            markers,
            firstMarkerIndex ->
            markers to firstMarkerIndex
        }
        .distinctUntilChanged()

@FlowPreview
@ExperimentalCoroutinesApi
internal val CameraViewModel.radarEnlargedUpdates: Flow<Boolean>
    get() = mapStates(CameraState::radarEnlarged).distinctUntilChanged().debounce(250L)

internal data class CameraMarkersDrawnViewUpdate(
    val firstMarkerIndex: Int,
    val markersSize: Int,
    val currentPage: Int,
    val maxPage: Int,
    val cameraObscured: Boolean
)
