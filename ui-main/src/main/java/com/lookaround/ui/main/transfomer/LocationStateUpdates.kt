package com.lookaround.ui.main.transfomer

import com.lookaround.core.android.architecture.FlowTransformer
import com.lookaround.core.android.exception.LocationUpdateFailureException
import com.lookaround.core.android.ext.locationWith
import com.lookaround.core.model.LocationDataDTO
import com.lookaround.core.usecase.IsLocationAvailable
import com.lookaround.core.usecase.LocationDataFlow
import com.lookaround.ui.main.model.*
import dagger.Reusable
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

@ExperimentalCoroutinesApi
@FlowPreview
@Reusable
class LocationStateUpdates
@Inject
constructor(
    private val isLocationAvailable: IsLocationAvailable,
    private val locationDataFlow: LocationDataFlow,
) : FlowTransformer<MainState, MainIntent.LocationPermissionGranted, MainSignal> {
    override fun invoke(
        flow: Flow<MainIntent.LocationPermissionGranted>,
        currentState: () -> MainState,
        signal: suspend (MainSignal) -> Unit
    ): Flow<MainState.() -> MainState> = flow.take(1).flatMapLatest { locationStateUpdatesFlow }

    private val locationStateUpdatesFlow: Flow<(MainState) -> MainState>
        get() =
            locationDataFlow(LOCATION_UPDATES_INTERVAL_MILLIS)
                .distinctUntilChangedBy { it::class }
                .transformLatest {
                    when (it) {
                        is LocationDataDTO.Failure -> {
                            if (!isLocationAvailable()) {
                                emit(LocationDisabledUpdate)
                                do {
                                    delay(LOCATION_UPDATES_INTERVAL_MILLIS)
                                } while (!isLocationAvailable())
                                emit(LoadingLocationUpdate)
                            } else {
                                emit(
                                    FailedToUpdateLocationUpdate(
                                        it.exception ?: LocationUpdateFailureException
                                    )
                                )
                            }
                        }
                        is LocationDataDTO.Success -> {
                            emit(
                                LocationLoadedUpdate(
                                    locationWith(
                                        latitude = it.lat,
                                        longitude = it.lng,
                                        altitude = it.alt
                                    )
                                )
                            )
                        }
                    }
                }
                .onStart {
                    if (!isLocationAvailable()) emit(LocationDisabledUpdate)
                    else emit(LoadingLocationUpdate)
                }

    companion object {
        private const val LOCATION_UPDATES_INTERVAL_MILLIS = 5_000L
    }
}
