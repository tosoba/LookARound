package com.lookaround.ui.camera

import com.lookaround.core.android.base.arch.FlowProcessor
import com.lookaround.core.android.model.LocationFactory
import com.lookaround.core.model.LocationDataDTO
import com.lookaround.core.usecase.IsLocationAvailable
import com.lookaround.core.usecase.LocationDataFlow
import com.lookaround.ui.camera.model.CameraIntent
import com.lookaround.ui.camera.model.CameraSignal
import com.lookaround.ui.camera.model.CameraState
import com.lookaround.ui.camera.model.CameraStateUpdate
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

@ExperimentalCoroutinesApi
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
        merge(
            intents.filterIsInstance<CameraIntent.CameraViewCreated>().map {
                CameraStateUpdate.CameraViewCreated
            },
            intents
                .filterIsInstance<CameraIntent.LocationPermissionGranted>()
                .take(1)
                .flatMapLatest {
                    locationDataFlow(LOCATION_UPDATES_INTERVAL_MILLIS)
                        .distinctUntilChangedBy { it::class }
                        .mapLatest {
                            when (it) {
                                LocationDataDTO.Failure -> {
                                    do {
                                        delay(LOCATION_UPDATES_INTERVAL_MILLIS)
                                    } while (!isLocationAvailable())
                                    CameraStateUpdate.LoadingLocation
                                }
                                is LocationDataDTO.Success ->
                                    CameraStateUpdate.LocationLoaded(
                                        LocationFactory.create(
                                            latitude = it.lat,
                                            longitude = it.lng,
                                            altitude = it.alt
                                        )
                                    )
                            }
                        }
                },
            intents.filterIsInstance<CameraIntent.LocationPermissionDenied>().map {
                CameraStateUpdate.LocationPermissionDenied
            },
            intents.filterIsInstance<CameraIntent.CameraPermissionDenied>().map {
                CameraStateUpdate.CameraPermissionDenied
            },
            intents.filterIsInstance<CameraIntent.CameraStreamStateChanged>().map { (streamState) ->
                CameraStateUpdate.CameraStreamStateChanged(streamState)
            }
        )

    companion object {
        private const val LOCATION_UPDATES_INTERVAL_MILLIS = 3_000L
    }
}
