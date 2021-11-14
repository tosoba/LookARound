package com.lookaround.ui.main

import androidx.lifecycle.SavedStateHandle
import com.lookaround.core.android.base.arch.FlowProcessor
import com.lookaround.core.android.model.LocationFactory
import com.lookaround.core.android.model.WithValue
import com.lookaround.core.ext.withLatestFrom
import com.lookaround.core.model.LocationDataDTO
import com.lookaround.core.usecase.GetPlacesOfType
import com.lookaround.core.usecase.IsConnectedFlow
import com.lookaround.core.usecase.IsLocationAvailable
import com.lookaround.core.usecase.LocationDataFlow
import com.lookaround.ui.main.model.MainIntent
import com.lookaround.ui.main.model.MainSignal
import com.lookaround.ui.main.model.MainState
import com.lookaround.ui.main.model.MainStateUpdate
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@ExperimentalCoroutinesApi
class MainFlowProcessor
@Inject
constructor(
    private val getPlacesOfType: GetPlacesOfType,
    private val isLocationAvailable: IsLocationAvailable,
    private val locationDataFlow: LocationDataFlow,
    private val isConnectedFlow: IsConnectedFlow
) : FlowProcessor<MainIntent, MainStateUpdate, MainState, MainSignal> {
    override fun updates(
        coroutineScope: CoroutineScope,
        intents: Flow<MainIntent>,
        currentState: () -> MainState,
        states: Flow<MainState>,
        intent: suspend (MainIntent) -> Unit,
        signal: suspend (MainSignal) -> Unit
    ): Flow<MainStateUpdate> =
        merge(
            loadPlacesUpdates(intents, currentState, signal),
            intents.filterIsInstance<MainIntent.LocationPermissionGranted>().take(1).flatMapLatest {
                locationStateUpdatesFlow
            },
            intents.filterIsInstance<MainIntent.LocationPermissionDenied>().map {
                MainStateUpdate.LocationPermissionDenied
            },
            intents.filterIsInstance<MainIntent.SearchQueryChanged>().map { (query) ->
                MainStateUpdate.SearchQueryChanged(query)
            },
            intents.filterIsInstance<MainIntent.SearchFocusChanged>().map { (focused) ->
                MainStateUpdate.SearchFocusChanged(focused)
            },
            intents.filterIsInstance<MainIntent.LiveBottomSheetStateChanged>().map { (sheetState) ->
                MainStateUpdate.LiveBottomSheetStateChanged(sheetState)
            },
            intents.filterIsInstance<MainIntent.BottomNavigationViewItemSelected>().map { (itemId)
                ->
                MainStateUpdate.BottomNavigationViewItemSelected(itemId)
            }
        )

    override fun stateWillUpdate(
        currentState: MainState,
        nextState: MainState,
        update: MainStateUpdate,
        savedStateHandle: SavedStateHandle
    ) {
        savedStateHandle[MainState::class.java.simpleName] = nextState
    }

    private fun loadPlacesUpdates(
        intents: Flow<MainIntent>,
        currentState: () -> MainState,
        signal: suspend (MainSignal) -> Unit
    ): Flow<MainStateUpdate> =
        intents
            .filterIsInstance<MainIntent.LoadPlaces>()
            .withLatestFrom(isConnectedFlow()) { (placeType), isConnected ->
                placeType to isConnected
            }
            .transformLatest { (type, isConnected) ->
                val currentLocation = currentState().locationState
                if (currentLocation !is WithValue) {
                    signal(MainSignal.UnableToLoadPlacesWithoutLocation)
                    return@transformLatest
                }

                if (!isConnected) {
                    signal(MainSignal.UnableToLoadPlacesWithoutConnection)
                    return@transformLatest
                }

                emit(MainStateUpdate.LoadingPlaces)
                try {
                    val places =
                        withTimeout(10_000) {
                            getPlacesOfType(
                                placeType = type,
                                lat = currentLocation.value.latitude,
                                lng = currentLocation.value.longitude,
                                radiusInMeters = PLACES_LOADING_RADIUS_METERS
                            )
                        }
                    emit(MainStateUpdate.PlacesLoaded(places))
                } catch (throwable: Throwable) {
                    emit(MainStateUpdate.PlacesError(throwable))
                    signal(MainSignal.PlacesLoadingFailed(throwable))
                }
            }

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
                .onStart { if (!isLocationAvailable()) emit(MainStateUpdate.LocationDisabled) }

    companion object {
        private const val LOCATION_UPDATES_INTERVAL_MILLIS = 5_000L
        private const val PLACES_LOADING_RADIUS_METERS = 5_000f
    }
}
