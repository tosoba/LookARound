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
): Flow<Pair<Boolean, Boolean>> =
    mainViewModel
        .states
        .combine(cameraViewModel.states) { mainState, cameraState ->
            (mainState.locationState is Ready) to cameraState.previewState.isLive
        }
        .distinctUntilChanged()
        .filter { (locationReady, cameraStreaming) -> locationReady && cameraStreaming }

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
        .combine(cameraViewModel.states) { mainState, cameraState ->
            mainState.locationState to cameraState.previewState
        }
        .distinctUntilChanged()
        .filter { (locationState, previewState) ->
            locationState !is Failed &&
                (locationState is LoadingInProgress || previewState.isLoading)
        }
        .map {}

@FlowPreview
@ExperimentalCoroutinesApi
internal fun arDisabledUpdates(
    mainViewModel: MainViewModel,
    cameraViewModel: CameraViewModel
): Flow<Pair<Boolean, Boolean>> =
    mainViewModel
        .states
        .combine(cameraViewModel.states) { mainState, cameraState ->
            Pair(
                mainState.locationState.isFailedWith<LocationPermissionDeniedException>() ||
                    cameraState.previewState is CameraPreviewState.PermissionDenied,
                mainState.locationState.isFailedWith<LocationDisabledException>()
            )
        }
        .distinctUntilChanged()
        .filter { (anyPermissionDenied, locationDisabled) ->
            anyPermissionDenied || locationDisabled
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

@FlowPreview
@ExperimentalCoroutinesApi
fun getMarkerUpdates(
    mainViewModel: MainViewModel,
    cameraViewModel: CameraViewModel
): Flow<Pair<Loadable<ParcelableSortedSet<Marker>>, Int>> =
    mainViewModel
        .states
        .map(MainState::markers::get)
        .drop(1)
        .onStart {
            emitAll(
                mainViewModel
                    .states
                    .map(MainState::markers::get)
                    .map { markers ->
                        when {
                            markers is FailedNext -> Ready(markers.value)
                            markers is FailedFirst -> Empty
                            else -> markers
                        }
                    }
                    .take(1)
            )
        }
        .combine(cameraViewModel.states.map(CameraState::firstMarkerIndex::get)) {
            markers,
            firstMarkerIndex ->
            markers to firstMarkerIndex
        }
        .distinctUntilChanged()
