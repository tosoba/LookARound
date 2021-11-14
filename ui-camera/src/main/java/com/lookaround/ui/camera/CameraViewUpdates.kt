package com.lookaround.ui.camera

import androidx.camera.view.PreviewView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.lookaround.core.android.exception.LocationDisabledException
import com.lookaround.core.android.exception.LocationPermissionDeniedException
import com.lookaround.core.android.model.*
import com.lookaround.ui.camera.model.CameraARDisabledViewUpdate
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
        mainViewModel.states.map(MainState::locationState::get),
        cameraViewModel.states.map(CameraState::previewState::get),
        cameraViewModel.signals.filterIsInstance<CameraSignal.PitchChanged>(),
        cameraViewObscuredUpdates(mainViewModel, cameraViewModel),
        mainViewModel.states.map { it.markers is WithValue }
    ) { locationState, previewState, (pitchWithinLimit), obscured, showingAnyMarkers ->
        (locationState is Ready) &&
            previewState.isLive &&
            (!showingAnyMarkers || pitchWithinLimit) &&
            !obscured
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
        .states
        .map(MainState::locationState::get)
        .combine(cameraViewModel.states.map(CameraState::previewState::get)) {
            locationState,
            previewState ->
            locationState !is Failed &&
                (locationState is LoadingInProgress || previewState.isLoading)
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
        mainViewModel.states.map(MainState::locationState::get),
        cameraViewModel.states.map(CameraState::previewState::get),
        cameraViewModel.signals.filterIsInstance<CameraSignal.PitchChanged>(),
        cameraViewObscuredUpdates(mainViewModel, cameraViewModel),
        mainViewModel.states.map { it.markers is WithValue }
    ) { locationState, previewState, (pitchWithinLimit), obscured, showingAnyMarkers ->
        CameraARDisabledViewUpdate(
            anyPermissionDenied =
                locationState.isFailedWith<LocationPermissionDeniedException>() ||
                    previewState is CameraPreviewState.PermissionDenied,
            locationDisabled = locationState.isFailedWith<LocationDisabledException>(),
            pitchOutsideRequiredLimit = !pitchWithinLimit && !obscured && showingAnyMarkers,
            cameraInitializationFailure = previewState is CameraPreviewState.InitializationFailure
        )
    }
        .distinctUntilChanged()
        .filter { (anyPermissionDenied, locationDisabled, pitchOutsideLimit) ->
            anyPermissionDenied || locationDisabled || pitchOutsideLimit
        }

@FlowPreview
@ExperimentalCoroutinesApi
fun cameraViewObscuredUpdates(
    mainViewModel: MainViewModel,
    cameraViewModel: CameraViewModel
): Flow<Boolean> =
    combine(
            mainViewModel
                .signals
                .filterIsInstance<MainSignal.TopFragmentChanged>()
                .map(MainSignal.TopFragmentChanged::cameraObscured::get),
            mainViewModel
                .signals
                .filterIsInstance<MainSignal.BottomSheetStateChanged>()
                .map(MainSignal.BottomSheetStateChanged::state::get),
            cameraViewModel
                .states
                .map(CameraState::previewState::get)
                .filter(CameraPreviewState::isLive::get)
        ) { obscured, sheetState, _ ->
            obscured ||
                sheetState == BottomSheetBehavior.STATE_EXPANDED ||
                sheetState == BottomSheetBehavior.STATE_DRAGGING ||
                sheetState == BottomSheetBehavior.STATE_SETTLING
        }
        .distinctUntilChanged()
        .onStart { emit(false) }
        .debounce(500L)

@FlowPreview
@ExperimentalCoroutinesApi
fun cameraTouchUpdates(mainViewModel: MainViewModel, cameraViewModel: CameraViewModel): Flow<Unit> =
    cameraViewModel
        .signals
        .filterIsInstance<CameraSignal.CameraTouch>()
        .filter {
            mainViewModel.state.locationState is Ready && cameraViewModel.state.previewState.isLive
        }
        .map {}
        .debounce(250L)

@FlowPreview
@ExperimentalCoroutinesApi
fun markerUpdates(
    mainViewModel: MainViewModel,
    cameraViewModel: CameraViewModel
): Flow<Pair<Loadable<ParcelableSortedSet<Marker>>, Int>> =
    mainViewModel
        .states
        .map(MainState::markers::get)
        .combine(cameraViewModel.states.map(CameraState::firstMarkerIndex::get)) {
            markers,
            firstMarkerIndex ->
            markers to firstMarkerIndex
        }
        .distinctUntilChanged()

@FlowPreview
@ExperimentalCoroutinesApi
val CameraViewModel.radarEnlargedUpdates: Flow<Boolean>
    get() = states.map(CameraState::radarEnlarged::get).distinctUntilChanged().debounce(250L)
