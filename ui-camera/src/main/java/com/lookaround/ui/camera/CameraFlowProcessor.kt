package com.lookaround.ui.camera

import com.lookaround.core.android.base.arch.FlowProcessor
import com.lookaround.ui.camera.model.*
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

@ExperimentalCoroutinesApi
class CameraFlowProcessor @Inject constructor() :
    FlowProcessor<CameraIntent, CameraStateUpdate, CameraState, CameraSignal> {
    override fun updates(
        coroutineScope: CoroutineScope,
        intents: Flow<CameraIntent>,
        currentState: () -> CameraState,
        states: Flow<CameraState>,
        intent: suspend (CameraIntent) -> Unit,
        signal: suspend (CameraSignal) -> Unit
    ): Flow<CameraStateUpdate> =
        merge(
            intents.filterIsInstance<CameraIntent.CameraViewCreated>().map {
                CameraStateUpdate.CameraPreviewStateUpdate(CameraPreviewState.Initial)
            },
            intents.filterIsInstance<CameraIntent.CameraStreamStateChanged>().map { (streamState) ->
                CameraStateUpdate.CameraPreviewStateUpdate(CameraPreviewState.Active(streamState))
            },
            intents.filterIsInstance<CameraIntent.CameraPermissionDenied>().map {
                CameraStateUpdate.CameraPreviewStateUpdate(CameraPreviewState.PermissionDenied)
            },
            intents.filterIsInstance<CameraIntent.CameraInitializationFailed>().map {
                CameraStateUpdate.CameraPreviewStateUpdate(CameraPreviewState.InitializationFailure)
            },
            intents.filterIsInstance<CameraIntent.CameraMarkersFirstIndexChanged>().map {
                (difference) ->
                CameraStateUpdate.CameraMarkersFirstIndexChanged(difference)
            },
            intents.filterIsInstance<CameraIntent.ToggleRadarEnlarged>().map {
                CameraStateUpdate.ToggleRadarEnlarged
            }
        )
}
