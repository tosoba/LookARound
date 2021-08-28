package com.lookaround.ui.main

import androidx.lifecycle.SavedStateHandle
import com.lookaround.core.android.base.arch.FlowProcessor
import com.lookaround.core.android.model.BottomSheetState
import com.lookaround.core.android.model.LocationFactory
import com.lookaround.core.android.model.WithValue
import com.lookaround.core.model.LocationDataDTO
import com.lookaround.core.usecase.GetPlacesOfType
import com.lookaround.core.usecase.IsLocationAvailable
import com.lookaround.core.usecase.LocationDataFlow
import com.lookaround.ui.main.model.MainIntent
import com.lookaround.ui.main.model.MainSignal
import com.lookaround.ui.main.model.MainState
import com.lookaround.ui.main.model.MainStateUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@ExperimentalCoroutinesApi
class MainFlowProcessor
@Inject
constructor(
    private val getPlacesOfType: GetPlacesOfType,
    private val isLocationAvailable: IsLocationAvailable,
    private val locationDataFlow: LocationDataFlow
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
            bottomSheetStateUpdates(intents, currentState),
            intents.filterIsInstance<MainIntent.SearchQueryChanged>().map { (query) ->
                MainStateUpdate.SearchQueryChanged(query)
            },
            intents.filterIsInstance<MainIntent.SearchFocusChanged>().map { (focused) ->
                MainStateUpdate.SearchFocusChanged(focused)
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
        intents.filterIsInstance<MainIntent.LoadPlaces>().transformLatest { (type) ->
            val currentLocation = currentState().locationState
            if (currentLocation !is WithValue) {
                signal(MainSignal.UnableToLoadPlacesWithoutLocation)
                return@transformLatest
            }

            emit(MainStateUpdate.LoadingPlaces)
            try {
                emit(
                    MainStateUpdate.PlacesLoaded(
                        getPlacesOfType(
                            type,
                            currentLocation.value.latitude,
                            currentLocation.value.longitude,
                            10_000f
                        )
                    )
                )
            } catch (throwable: Throwable) {
                emit(MainStateUpdate.PlacesError(throwable))
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

    private fun bottomSheetStateUpdates(
        intents: Flow<MainIntent>,
        currentState: () -> MainState
    ): Flow<MainStateUpdate.BottomSheetStateChanged> =
        intents
            .filterIsInstance<MainIntent.BottomSheetStateChanged>()
            .distinctUntilChanged()
            .filterNot { (newSheetState, isChangedByUser) ->
                val (currentSheetState, wasChangedByUser) = currentState().bottomSheetState
                currentSheetState == newSheetState && !wasChangedByUser && isChangedByUser
            }
            .map { (sheetState, changedByUser) ->
                MainStateUpdate.BottomSheetStateChanged(BottomSheetState(sheetState, changedByUser))
            }

    companion object {
        private const val LOCATION_UPDATES_INTERVAL_MILLIS = 5_000L
    }
}
