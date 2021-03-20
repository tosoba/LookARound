package com.lookaround.ui.camera

import com.lookaround.core.android.base.arch.FlowProcessor
import com.lookaround.ui.camera.model.CameraIntent
import com.lookaround.ui.camera.model.CameraSignal
import com.lookaround.ui.camera.model.CameraState
import com.lookaround.ui.camera.model.CameraStateUpdate
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
                CameraStateUpdate.CameraViewCreated
            },
            intents.filterIsInstance<CameraIntent.CameraStreamStateChanged>().map { (streamState) ->
                CameraStateUpdate.CameraStreamStateChanged(streamState)
            },
            intents.filterIsInstance<CameraIntent.CameraPermissionDenied>().map {
                CameraStateUpdate.CameraPermissionDenied
            },
        )
}
