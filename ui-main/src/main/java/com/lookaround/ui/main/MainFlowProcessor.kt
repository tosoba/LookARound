package com.lookaround.ui.main

import androidx.lifecycle.SavedStateHandle
import com.lookaround.core.android.architecture.FlowProcessor
import com.lookaround.core.android.ext.roundToDecimalPlaces
import com.lookaround.core.android.model.LocationFactory
import com.lookaround.core.android.model.WithValue
import com.lookaround.core.ext.withLatestFrom
import com.lookaround.core.model.LocationDataDTO
import com.lookaround.core.usecase.*
import com.lookaround.ui.main.model.*
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@ExperimentalCoroutinesApi
@FlowPreview
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
    private val autocompleteSearch: AutocompleteSearch,
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
            intents
                .filterIsInstance<MainIntent.GetPlacesOfType>()
                .searchAroundUpdates(currentState, signal),
            intents.filterIsInstance<MainIntent.LocationPermissionGranted>().take(1).flatMapLatest {
                locationStateUpdatesFlow
            },
            totalSearchesCountFlow().distinctUntilChanged().map(::RecentSearchesCountUpdate),
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
            intents
                .filterIsInstance<MainIntent.SearchQueryChanged>()
                .autocompleteSearchUpdates(currentState, signal),
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

    private fun Flow<MainIntent.GetPlacesOfType>.searchAroundUpdates(
        currentState: () -> MainState,
        signal: suspend (MainSignal) -> Unit
    ): Flow<(MainState) -> MainState> =
        withLatestFrom(isConnectedFlow()) { (placeType), isConnected -> placeType to isConnected }
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
                        withTimeout(PLACES_LOADING_TIMEOUT) {
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
                        is LocationDataDTO.Success -> {
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
                }
                .onStart { if (!isLocationAvailable()) emit(LocationDisabledUpdate) }

    private fun Flow<MainIntent.SearchQueryChanged>.autocompleteSearchUpdates(
        currentState: () -> MainState,
        signal: suspend (MainSignal) -> Unit
    ): Flow<(MainState) -> MainState> =
        map { (query) -> query.trim() }
            .filterNot { query ->
                query.isBlank() ||
                    query.count(Char::isLetterOrDigit) <= 3 ||
                    currentState().searchMode != MainSearchMode.AUTOCOMPLETE
            }
            .distinctUntilChanged()
            .debounce(500L)
            .withLatestFrom(isConnectedFlow()) { query, isConnected -> query to isConnected }
            .transformLatest { (query, isConnected) ->
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
                    emit(
                        AutocompleteSearchResultsLoadedUpdate(
                            points =
                                withTimeout(PLACES_LOADING_TIMEOUT) {
                                    autocompleteSearch(
                                        query = query,
                                        priorityLat =
                                            currentLocation
                                                .value
                                                .latitude
                                                .roundToDecimalPlaces(3)
                                                .toDouble(),
                                        priorityLon =
                                            currentLocation
                                                .value
                                                .longitude
                                                .roundToDecimalPlaces(3)
                                                .toDouble()
                                    )
                                },
                        )
                    )
                } catch (throwable: Throwable) {
                    emit(SearchErrorUpdate(throwable))
                    signal(MainSignal.PlacesLoadingFailed(throwable))
                }
            }

    companion object {
        private const val LOCATION_UPDATES_INTERVAL_MILLIS = 5_000L
        private const val PLACES_LOADING_TIMEOUT = 10_000L
        private const val PLACES_LOADING_RADIUS_METERS = 5_000f
    }
}
