package com.lookaround.ui.camera.model

import android.location.Location
import androidx.camera.view.PreviewView
import com.lookaround.core.android.exception.LocationDisabledException
import com.lookaround.core.android.exception.LocationPermissionDeniedException
import com.lookaround.core.android.model.Failed
import com.lookaround.core.android.model.LoadingInProgress
import com.lookaround.core.android.model.Ready
import com.lookaround.core.android.model.WithValue
import com.lookaround.ui.camera.CameraViewModel
import java.util.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

@FlowPreview
@ExperimentalCoroutinesApi
internal val CameraViewModel.arEnabledUpdates: Flow<Pair<Boolean, Boolean>>
    get() =
        states
            .map { (locationState, cameraPreviewState) ->
                (locationState is Ready) to
                    (cameraPreviewState is CameraPreviewState.Active &&
                        cameraPreviewState.streamState == PreviewView.StreamState.STREAMING)
            }
            .distinctUntilChanged()
            .filter { (locationReady, cameraStreaming) -> locationReady && cameraStreaming }

@FlowPreview
@ExperimentalCoroutinesApi
internal val CameraViewModel.locationReadyUpdates: Flow<Location>
    get() =
        states
            .map { it.locationState }
            .filterIsInstance<WithValue<Location>>()
            .map { it.value }
            .distinctUntilChangedBy { Objects.hash(it.latitude, it.longitude) }

@FlowPreview
@ExperimentalCoroutinesApi
internal val CameraViewModel.loadingStartedUpdates: Flow<Pair<Boolean, Boolean>>
    get() =
        states
            .map { (locationState, cameraPreviewState) ->
                (locationState is LoadingInProgress) to
                    (cameraPreviewState is CameraPreviewState.Initial ||
                        (cameraPreviewState is CameraPreviewState.Active &&
                            cameraPreviewState.streamState == PreviewView.StreamState.IDLE))
            }
            .distinctUntilChanged()
            .filter { (loadingLocation, loadingCamera) -> loadingLocation || loadingCamera }

@FlowPreview
@ExperimentalCoroutinesApi
internal val CameraViewModel.arDisabledUpdates: Flow<Pair<Boolean, Boolean>>
    get() =
        states
            .map { (locationState, cameraPreviewState) ->
                ((locationState is Failed &&
                    locationState.error is LocationPermissionDeniedException) ||
                    (cameraPreviewState is CameraPreviewState.PermissionDenied)) to
                    (locationState is Failed && locationState.error is LocationDisabledException)
            }
            .distinctUntilChanged()
            .filter { (anyPermissionDenied, locationDisabled) ->
                anyPermissionDenied || locationDisabled
            }
