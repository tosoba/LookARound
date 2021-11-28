package com.lookaround.ui.main

import androidx.lifecycle.SavedStateHandle
import com.lookaround.core.android.architecture.FlowProcessor
import com.lookaround.core.android.model.LocationFactory
import com.lookaround.core.android.model.WithValue
import com.lookaround.core.ext.withLatestFrom
import com.lookaround.core.model.LocationDataDTO
import com.lookaround.core.usecase.*
import com.lookaround.ui.main.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@ExperimentalCoroutinesApi
class MainFlowProcessor
@Inject
constructor(
    private val getPlacesOfTypeAround: GetPlacesOfTypeAround,
    private val isLocationAvailable: IsLocationAvailable,
    private val locationDataFlow: LocationDataFlow,
    private val isConnectedFlow: IsConnectedFlow,
    private val totalSearchesCountFlow: TotalSearchesCountFlow,
    private val getSearchAroundResults: GetSearchAroundResults,
    private val getAutocompleteSearchResults: GetAutocompleteSearchResults,
) : FlowProcessor<MainIntent, MainState, MainSignal> {
    override fun updates(
        coroutineScope: CoroutineScope,
        intents: Flow<MainIntent>,
        currentState: () -> MainState,
        states: Flow<MainState>,
        intent: suspend (MainIntent) -> Unit,
        signal: suspend (MainSignal) -> Unit
    ): Flow<(MainState) -> MainState> =
        merge(
            searchAroundUpdates(intents, currentState, signal),
            intents.filterIsInstance<MainIntent.LocationPermissionGranted>().take(1).flatMapLatest {
                locationStateUpdatesFlow
            },
            totalSearchesCountFlow().distinctUntilChanged().map(::SearchesCountUpdate),
            intents.filterIsInstance<MainIntent.LoadSearchAroundResults>().transformLatest {
                (searchId) ->
                emit(LoadingSearchResultsUpdate)
                emit(SearchAroundResultsLoadedUpdate(getSearchAroundResults(searchId)))
            },
            intents.filterIsInstance<MainIntent.LoadSearchAutocompleteResults>().transformLatest {
                (searchId) ->
                emit(LoadingSearchResultsUpdate)
                emit(AutocompleteSearchResultsLoadedUpdate(getAutocompleteSearchResults(searchId)))
            },
            intents.filterIsInstance()
        )

    override fun stateWillUpdate(
        currentState: MainState,
        nextState: MainState,
        update: (MainState) -> MainState,
        savedStateHandle: SavedStateHandle
    ) {
        savedStateHandle[MainState::class.java.simpleName] = nextState
    }

    private fun searchAroundUpdates(
        intents: Flow<MainIntent>,
        currentState: () -> MainState,
        signal: suspend (MainSignal) -> Unit
    ): Flow<(MainState) -> MainState> =
        intents
            .filterIsInstance<MainIntent.GetPlacesOfType>()
            .withLatestFrom(isConnectedFlow()) { (placeType), isConnected ->
                placeType to isConnected
            }
            .transformLatest { (placeType, isConnected) ->
                val currentLocation = currentState().locationState
                if (currentLocation !is WithValue) {
                    signal(MainSignal.UnableToLoadPlacesWithoutLocation)
                    return@transformLatest
                }

                if (!isConnected) {
                    signal(MainSignal.UnableToLoadPlacesWithoutConnection)
                    return@transformLatest
                }

                emit(LoadingSearchResultsUpdate)
                try {
                    val places =
                        withTimeout(10_000) {
                            getPlacesOfTypeAround(
                                placeType = placeType,
                                lat = currentLocation.value.latitude,
                                lng = currentLocation.value.longitude,
                                radiusInMeters = PLACES_LOADING_RADIUS_METERS
                            )
                        }
                    emit(SearchAroundResultsLoadedUpdate(places))
                } catch (throwable: Throwable) {
                    emit(SearchErrorUpdate(throwable))
                    signal(MainSignal.PlacesLoadingFailed(throwable))
                }
            }

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
                                emit(FailedToUpdateLocationUpdate)
                            }
                        }
                        is LocationDataDTO.Success ->
                            emit(
                                LocationLoadedUpdate(
                                    LocationFactory.create(
                                        latitude = it.lat,
                                        longitude = it.lng,
                                        altitude = it.alt
                                    )
                                )
                            )
                    }
                }
                .onStart { if (!isLocationAvailable()) emit(LocationDisabledUpdate) }

    companion object {
        private const val LOCATION_UPDATES_INTERVAL_MILLIS = 5_000L
        private const val PLACES_LOADING_RADIUS_METERS = 5_000f
    }
}
