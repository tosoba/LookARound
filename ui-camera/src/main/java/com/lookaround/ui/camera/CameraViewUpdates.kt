package com.lookaround.ui.camera

import android.location.Location
import androidx.camera.view.PreviewView
import com.lookaround.core.android.exception.LocationDisabledException
import com.lookaround.core.android.exception.LocationPermissionDeniedException
import com.lookaround.core.android.model.*
import com.lookaround.ui.camera.model.CameraPreviewState
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.model.MainState
import java.util.*
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
            .map(MainState::markers::get)
            .distinctUntilChanged()
            .filterIsInstance<WithValue<ParcelableList<Marker>>>()
            .map { it.value.items }

@FlowPreview
@ExperimentalCoroutinesApi
internal val MainViewModel.locationReadyUpdates: Flow<Location>
    get() =
        states
            .map { it.locationState }
            .filterIsInstance<WithValue<Location>>()
            .map { it.value }
            .distinctUntilChangedBy { Objects.hash(it.latitude, it.longitude) }