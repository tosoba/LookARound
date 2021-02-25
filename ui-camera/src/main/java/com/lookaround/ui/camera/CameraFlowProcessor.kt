package com.lookaround.ui.camera

import android.location.Location
import com.lookaround.core.android.base.arch.FlowProcessor
import com.lookaround.core.model.LocationDataDTO
import com.lookaround.core.usecase.IsLocationAvailable
import com.lookaround.core.usecase.LocationDataFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class CameraFlowProcessor
@Inject
constructor(
    private val isLocationAvailable: IsLocationAvailable,
    private val locationDataFlow: LocationDataFlow
) : FlowProcessor<CameraIntent, CameraStateUpdate, CameraState, CameraSignal> {
    override fun updates(
        coroutineScope: CoroutineScope,
        intents: Flow<CameraIntent>,
        currentState: () -> CameraState,
        states: Flow<CameraState>,
        intent: suspend (CameraIntent) -> Unit,
        signal: suspend (CameraSignal) -> Unit
    ): Flow<CameraStateUpdate> =
        locationDataFlow()
            .onEach {
                if (it is LocationDataDTO.Failure && !isLocationAvailable()) {
                    signal(CameraSignal.LocationUnavailable)
                }
            }
            .filterIsInstance<LocationDataDTO.Success>()
            .map {
                CameraStateUpdate.Location(
                    Location("").apply {
                        latitude = it.lat
                        longitude = it.lng
                        altitude = it.alt
                    })
            }
}
