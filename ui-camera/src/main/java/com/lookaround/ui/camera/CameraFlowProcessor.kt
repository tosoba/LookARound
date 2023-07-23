package com.lookaround.ui.camera

import com.lookaround.core.android.architecture.FlowProcessor
import com.lookaround.ui.camera.model.*
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

@ExperimentalCoroutinesApi
class CameraFlowProcessor @Inject constructor() :
    FlowProcessor<CameraIntent, CameraState, CameraSignal> {
    override fun updates(
        intents: Flow<CameraIntent>,
        currentState: () -> CameraState,
        signal: suspend (CameraSignal) -> Unit
    ): Flow<(CameraState) -> CameraState> =
        merge(
            intents.filterIsInstance<CameraIntent.CameraStreamStateChanged>().map { (streamState) ->
                CameraPreviewStateUpdate(CameraPreviewState.Active(streamState))
            },
            intents.filterIsInstance<CameraIntent.CameraPermissionDenied>().map {
                CameraPreviewStateUpdate(CameraPreviewState.PermissionDenied)
            },
            intents.filterIsInstance<CameraIntent.CameraInitializationFailed>().map {
                CameraPreviewStateUpdate(CameraPreviewState.InitializationFailure)
            },
            intents.filterIsInstance()
        )
}
