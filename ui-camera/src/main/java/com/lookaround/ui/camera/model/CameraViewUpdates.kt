package com.lookaround.ui.camera.model

import androidx.camera.view.PreviewView
import com.lookaround.core.android.exception.LocationDisabledException
import com.lookaround.core.android.exception.LocationPermissionDeniedException
import com.lookaround.core.android.model.*
import com.lookaround.ui.camera.CameraViewModel
import com.lookaround.ui.main.MainViewModel
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
            (mainState.locationState is Ready) to
                (cameraState.previewState is CameraPreviewState.Active &&
                    cameraState.previewState.streamState == PreviewView.StreamState.STREAMING)
        }
        .distinctUntilChanged()
        .filter { (locationReady, cameraStreaming) -> locationReady && cameraStreaming }

@FlowPreview
@ExperimentalCoroutinesApi
internal fun loadingStartedUpdates(
    mainViewModel: MainViewModel,
    cameraViewModel: CameraViewModel
): Flow<Pair<Boolean, Boolean>> =
    mainViewModel
        .states
        .combine(cameraViewModel.states) { mainState, cameraState ->
            (mainState.locationState is LoadingInProgress) to
                (cameraState.previewState is CameraPreviewState.Initial ||
                    (cameraState.previewState is CameraPreviewState.Active &&
                        cameraState.previewState.streamState == PreviewView.StreamState.IDLE))
        }
        .distinctUntilChanged()
        .filter { (loadingLocation, loadingCamera) -> loadingLocation || loadingCamera }

@FlowPreview
@ExperimentalCoroutinesApi
internal fun arDisabledUpdates(
    mainViewModel: MainViewModel,
    cameraViewModel: CameraViewModel
): Flow<Pair<Boolean, Boolean>> =
    mainViewModel
        .states
        .combine(cameraViewModel.states) { mainState, cameraState ->
            ((mainState.locationState as? Failed)?.error is LocationPermissionDeniedException ||
                (cameraState.previewState is CameraPreviewState.PermissionDenied)) to
                ((mainState.locationState as? Failed)?.error is LocationDisabledException)
        }
        .distinctUntilChanged()
        .filter { (anyPermissionDenied, locationDisabled) ->
            anyPermissionDenied || locationDisabled
        }

@FlowPreview
@ExperimentalCoroutinesApi
internal val MainViewModel.markerUpdates: Flow<List<Marker>>
    get() =
        states
            .map { it.markers }
            .distinctUntilChanged()
            .filterIsInstance<WithValue<ParcelableList<Marker>>>()
            .map { it.value.items }
