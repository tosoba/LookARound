package com.lookaround.ui.camera

import android.location.Location
import com.lookaround.core.android.base.arch.FlowProcessor
import com.lookaround.core.model.LocationDataDTO
import com.lookaround.core.usecase.IsLocationAvailable
import com.lookaround.core.usecase.LocationDataFlow
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
        locationDataFlow(LOCATION_UPDATES_INTERVAL_MILLIS)
            .distinctUntilChangedBy { it::class }
            .onEach {
                if (it is LocationDataDTO.Failure) {
                    signal(CameraSignal.LocationUnavailable)
                    coroutineScope.launch { observeLocationAvailability(signal) }
                }
            }
            .filterIsInstance<LocationDataDTO.Success>()
            .map {
                CameraStateUpdate.Location(
                    Location("").apply {
                        latitude = it.lat
                        longitude = it.lng
                        altitude = it.alt
                    }
                )
            }

    private suspend fun observeLocationAvailability(signal: suspend (CameraSignal) -> Unit) {
        do {
            delay(LOCATION_UPDATES_INTERVAL_MILLIS)
        } while (!isLocationAvailable())
        signal(CameraSignal.LocationLoading)
    }

    companion object {
        private const val LOCATION_UPDATES_INTERVAL_MILLIS = 3000L
    }
}
