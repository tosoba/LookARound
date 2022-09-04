package com.lookaround.ui.main

import androidx.lifecycle.SavedStateHandle
import com.lookaround.core.android.architecture.FlowProcessor
import com.lookaround.core.android.exception.LocationUpdateFailureException
import com.lookaround.core.android.ext.locationWith
import com.lookaround.core.android.ext.roundToDecimalPlaces
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
    private val isLocationAvailable: IsLocationAvailable,
    private val locationDataFlow: LocationDataFlow,
    private val isConnectedFlow: IsConnectedFlow,
    private val totalSearchesCountFlow: TotalSearchesCountFlow,
    private val getPlacesOfTypeAround: GetPlacesOfTypeAround,
    private val getAttractionsAround: GetAttractionsAround,
    private val autocompleteSearch: AutocompleteSearch,
    private val getSearchAroundResults: GetSearchAroundResults,
    private val getAutocompleteSearchResults: GetAutocompleteSearchResults,
) : FlowProcessor<MainIntent, MainState, MainSignal> {
    override fun updates(
        intents: Flow<MainIntent>,
        currentState: () -> MainState,
        signal: suspend (MainSignal) -> Unit
    ): Flow<(MainState) -> MainState> =
        merge(
            intents
                .filterIsInstance<MainIntent.GetPlacesOfType>()
                .placesOfTypeAroundUpdates(currentState, signal),
            intents
                .filterIsInstance<MainIntent.GetAttractions>()
                .attractionsAroundUpdates(currentState, signal),
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

    private fun Flow<MainIntent.GetPlacesOfType>.placesOfTypeAroundUpdates(
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

                emitPlacesUpdates(signal, ::SearchAroundResultsLoadedUpdate) {
                    getPlacesOfTypeAround(
                        placeType = placeType,
                        lat = currentLocation.value.latitude.roundedTo2DecimalPlaces,
                        lng = currentLocation.value.longitude.roundedTo2DecimalPlaces,
                        radiusInMeters = PLACES_LOADING_RADIUS_METERS
                    )
                }
            }

    private fun Flow<MainIntent.GetAttractions>.attractionsAroundUpdates(
        currentState: () -> MainState,
        signal: suspend (MainSignal) -> Unit
    ): Flow<(MainState) -> MainState> =
        withLatestFrom(isConnectedFlow()) { _, isConnected -> isConnected }.transformLatest {
            isConnected ->
            val currentLocation = currentState().locationState
            if (currentLocation !is WithValue) {
                signal(MainSignal.UnableToLoadPlacesWithoutLocation)
                return@transformLatest
            }

            if (!isConnected) {
                signal(MainSignal.UnableToLoadPlacesWithoutConnection)
                return@transformLatest
            }

            emitPlacesUpdates(signal, ::SearchAroundResultsLoadedUpdate) {
                getAttractionsAround(
                    lat = currentLocation.value.latitude.roundedTo2DecimalPlaces,
                    lng = currentLocation.value.longitude.roundedTo2DecimalPlaces,
                    radiusInMeters = PLACES_LOADING_RADIUS_METERS
                )
            }
        }

    private fun Flow<MainIntent.SearchQueryChanged>.autocompleteSearchUpdates(
        currentState: () -> MainState,
        signal: suspend (MainSignal) -> Unit
    ): Flow<(MainState) -> MainState> =
        map { (query) -> query.trim() }
            .filterNot { query -> query.isBlank() || query.count(Char::isLetterOrDigit) <= 3 }
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

                emitPlacesUpdates(signal, ::AutocompleteSearchResultsLoadedUpdate) {
                    autocompleteSearch(
                        query = query,
                        priorityLat = currentLocation.value.latitude.roundedTo2DecimalPlaces,
                        priorityLon = currentLocation.value.longitude.roundedTo2DecimalPlaces
                    )
                }
            }

    private suspend fun <T> FlowCollector<(MainState) -> MainState>.emitPlacesUpdates(
        signal: suspend (MainSignal) -> Unit,
        placesLoadedUpdate: (Iterable<T>) -> (MainState) -> MainState,
        getPlaces: suspend () -> Collection<T>
    ) {
        emit(LoadingSearchResultsUpdate)
        try {
            val places = withTimeout(PLACES_LOADING_TIMEOUT_MILLIS) { getPlaces() }
            if (places.isEmpty()) {
                signal(MainSignal.NoPlacesFound)
                emit(NoPlacesFoundUpdate)
            } else {
                emit(placesLoadedUpdate(places))
            }
        } catch (ex: Exception) {
            emit(SearchErrorUpdate(ex))
            signal(MainSignal.PlacesLoadingFailed(ex))
        }
    }

    companion object {
        private const val LOCATION_UPDATES_INTERVAL_MILLIS = 5_000L
        private const val PLACES_LOADING_TIMEOUT_MILLIS = 10_000L
        private const val PLACES_LOADING_RADIUS_METERS = 5_000f

        private val Double.roundedTo2DecimalPlaces: Double
            get() = roundToDecimalPlaces(2).toDouble()
    }
}
