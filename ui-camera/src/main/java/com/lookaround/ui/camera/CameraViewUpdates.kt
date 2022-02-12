package com.lookaround.ui.camera

import androidx.camera.view.PreviewView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.lookaround.core.android.exception.LocationDisabledException
import com.lookaround.core.android.exception.LocationPermissionDeniedException
import com.lookaround.core.android.model.*
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
): Flow<Unit> =
    combine(
            mainViewModel.mapStates(MainState::locationState),
            cameraViewModel.mapStates(CameraState::previewState),
            cameraViewModel.filterSignals<CameraSignal.PitchChanged>(),
            cameraViewObscuredUpdates(mainViewModel, cameraViewModel).onStart {
                emit(
                    CameraObscuredUpdate(obscuredByFragment = false, obscuredByBottomSheet = false)
                )
            },
            mainViewModel.states.map { it.markers.hasValue }
        ) {
        locationState,
        previewState,
        (pitchWithinLimit),
        (obscuredByFragment, obscuredByBottomSheet),
        showingAnyMarkers ->
        locationState is Ready &&
            previewState.isLive &&
            (!showingAnyMarkers || pitchWithinLimit) &&
            !obscuredByFragment &&
            !obscuredByBottomSheet
    }
        .distinctUntilChanged()
        .filter { it }
        .map {}

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
): Flow<Unit> =
    mainViewModel
        .mapStates(MainState::locationState)
        .combine(cameraViewModel.mapStates(CameraState::previewState)) { locationState, previewState
            ->
            locationState !is Failed && (locationState is Loading || previewState.isLoading)
        }
        .distinctUntilChanged()
        .filter { it }
        .map {}

@FlowPreview
@ExperimentalCoroutinesApi
internal fun arDisabledUpdates(
    mainViewModel: MainViewModel,
    cameraViewModel: CameraViewModel
): Flow<CameraARDisabledViewUpdate> =
    combine(
            mainViewModel.mapStates(MainState::locationState),
            cameraViewModel.mapStates(CameraState::previewState),
            cameraViewModel.filterSignals<CameraSignal.PitchChanged>(),
            cameraViewObscuredUpdates(mainViewModel, cameraViewModel),
            mainViewModel.states.map { it.markers.hasValue }
        ) {
        locationState,
        previewState,
        (pitchWithinLimit),
        (cameraObscuredByFragment, obscuredByBottomSheet),
        showingAnyMarkers ->
        CameraARDisabledViewUpdate(
            anyPermissionDenied =
                locationState.isFailedWith<LocationPermissionDeniedException>() ||
                    previewState is CameraPreviewState.PermissionDenied,
            locationDisabled = locationState.isFailedWith<LocationDisabledException>(),
            pitchOutsideRequiredLimit =
                !pitchWithinLimit &&
                    !cameraObscuredByFragment &&
                    !obscuredByBottomSheet &&
                    showingAnyMarkers,
            cameraInitializationFailure = previewState is CameraPreviewState.InitializationFailure
        )
    }
        .distinctUntilChanged()
        .filter { (anyPermissionDenied, locationDisabled, pitchOutsideLimit) ->
            anyPermissionDenied || locationDisabled || pitchOutsideLimit
        }

internal data class CameraARDisabledViewUpdate(
    val anyPermissionDenied: Boolean,
    val locationDisabled: Boolean,
    val pitchOutsideRequiredLimit: Boolean,
    val cameraInitializationFailure: Boolean,
)

@FlowPreview
@ExperimentalCoroutinesApi
internal fun cameraViewObscuredUpdates(
    mainViewModel: MainViewModel,
    cameraViewModel: CameraViewModel
): Flow<CameraObscuredUpdate> =
    combine(
            mainViewModel.filterSignals(MainSignal.TopFragmentChanged::cameraObscured).onStart {
                emit(false)
            },
            mainViewModel.filterSignals(MainSignal.BottomSheetStateChanged::state).onStart {
                emit(mainViewModel.state.lastLiveBottomSheetState)
            },
            cameraViewModel
                .mapStates(CameraState::previewState)
                .filter(CameraPreviewState::isLive::get)
        ) { obscured, sheetState, _ ->
            CameraObscuredUpdate(
                obscuredByFragment = obscured,
                obscuredByBottomSheet =
                    sheetState == BottomSheetBehavior.STATE_EXPANDED ||
                        sheetState == BottomSheetBehavior.STATE_DRAGGING ||
                        sheetState == BottomSheetBehavior.STATE_SETTLING
            )
        }
        .distinctUntilChanged()
        .debounce(500L)

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

internal data class CameraObscuredUpdate(
    val obscuredByFragment: Boolean,
    val obscuredByBottomSheet: Boolean
)
