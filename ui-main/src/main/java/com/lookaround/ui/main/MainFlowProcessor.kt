package com.lookaround.ui.main

import com.lookaround.core.android.base.arch.FlowProcessor
import com.lookaround.core.android.model.LocationFactory
import com.lookaround.core.model.LocationDataDTO
import com.lookaround.core.usecase.GetPlacesOfType
import com.lookaround.core.usecase.IsLocationAvailable
import com.lookaround.core.usecase.LocationDataFlow
import com.lookaround.ui.main.model.MainIntent
import com.lookaround.ui.main.model.MainState
import com.lookaround.ui.main.model.MainStateUpdate
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

@ExperimentalCoroutinesApi
class MainFlowProcessor
@Inject
constructor(
    private val getPlacesOfType: GetPlacesOfType,
    private val isLocationAvailable: IsLocationAvailable,
    private val locationDataFlow: LocationDataFlow
) : FlowProcessor<MainIntent, MainStateUpdate, MainState, Unit> {
    override fun updates(
        coroutineScope: CoroutineScope,
        intents: Flow<MainIntent>,
        currentState: () -> MainState,
        states: Flow<MainState>,
        intent: suspend (MainIntent) -> Unit,
        signal: suspend (Unit) -> Unit
    ): Flow<MainStateUpdate> =
        merge(
            intents.filterIsInstance<MainIntent.LoadPlaces>().transformLatest { (type) ->
                emit(MainStateUpdate.LoadingPlaces)
                try {
                    val places = getPlacesOfType(type, 52.237049, 21.017532, 10_000f)
                    emit(MainStateUpdate.PlacesLoaded(places))
                } catch (throwable: Throwable) {
                    emit(MainStateUpdate.PlacesError(throwable))
                }
            },
            intents.filterIsInstance<MainIntent.LocationPermissionGranted>().take(1).flatMapLatest {
                locationStateUpdatesFlow
            },
            intents.filterIsInstance<MainIntent.LocationPermissionDenied>().map {
                MainStateUpdate.LocationPermissionDenied
            },
        )

    private val locationStateUpdatesFlow: Flow<MainStateUpdate>
        get() =
            locationDataFlow(LOCATION_UPDATES_INTERVAL_MILLIS)
                .distinctUntilChangedBy { it::class }
                .transformLatest {
                    when (it) {
                        is LocationDataDTO.Failure -> {
                            if (!isLocationAvailable()) {
                                emit(MainStateUpdate.LocationDisabled)
                                do {
                                    delay(LOCATION_UPDATES_INTERVAL_MILLIS)
                                } while (!isLocationAvailable())
                                emit(MainStateUpdate.LoadingLocation)
                            } else {
                                emit(MainStateUpdate.FailedToUpdateLocation)
                            }
                        }
                        is LocationDataDTO.Success ->
                            emit(
                                MainStateUpdate.LocationLoaded(
                                    LocationFactory.create(
                                        latitude = it.lat,
                                        longitude = it.lng,
                                        altitude = it.alt
                                    )
                                )
                            )
                    }
                }

    companion object {
        private const val LOCATION_UPDATES_INTERVAL_MILLIS = 5_000L
    }
}
