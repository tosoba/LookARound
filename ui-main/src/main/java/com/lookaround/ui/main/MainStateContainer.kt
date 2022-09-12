package com.lookaround.ui.main

import androidx.lifecycle.SavedStateHandle
import com.lookaround.core.android.architecture.MviFlowStateContainer
import com.lookaround.core.android.architecture.StateContainerFactory
import com.lookaround.core.android.exception.LocationUpdateFailureException
import com.lookaround.core.android.ext.locationWith
import com.lookaround.core.android.ext.roundToDecimalPlaces
import com.lookaround.core.android.model.WithValue
import com.lookaround.core.ext.withLatestFrom
import com.lookaround.core.model.LocationDataDTO
import com.lookaround.core.usecase.*
import com.lookaround.ui.main.model.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withTimeout

@ExperimentalCoroutinesApi
@FlowPreview
class MainStateContainer
@AssistedInject
constructor(
    @Assisted savedStateHandle: SavedStateHandle,
    private val getPlacesOfTypeUpdates: GetPlacesOfTypeUpdates,
    private val isLocationAvailable: IsLocationAvailable,
    private val locationDataFlow: LocationDataFlow,
    private val isConnectedFlow: IsConnectedFlow,
    private val totalSearchesCountFlow: TotalSearchesCountFlow,
    private val getAttractionsAround: GetAttractionsAround,
    private val autocompleteSearch: AutocompleteSearch,
    private val getSearchAroundResults: GetSearchAroundResults,
    private val getAutocompleteSearchResults: GetAutocompleteSearchResults,
) :
    MviFlowStateContainer<MainState, MainIntent, MainSignal>(
        initialState = MainState(),
        savedStateHandle = savedStateHandle,
        fromSavedState = { it[MainState::class.java.simpleName] },
        saveState = { this[MainState::class.java.simpleName] = it }
    ) {

    override fun Flow<MainIntent>.updates(): Flow<MainState.() -> MainState> =
        merge(
            filterIsInstance<MainIntent.GetPlacesOfType>().mapTo(getPlacesOfTypeUpdates),
            filterIsInstance<MainIntent.GetAttractions>().attractionsAroundUpdates,
            filterIsInstance<MainIntent.LocationPermissionGranted>().take(1).flatMapLatest {
                locationStateUpdatesFlow
            },
            totalSearchesCountFlow().distinctUntilChanged().map(::RecentSearchesCountUpdate),
            filterIsInstance<MainIntent.LoadSearchAroundResults>().transformLatest { (searchId) ->
                emit(LoadingSearchResultsUpdate)
                emit(SearchAroundResultsLoadedUpdate(getSearchAroundResults(searchId)))
            },
            filterIsInstance<MainIntent.LoadSearchAutocompleteResults>().transformLatest {
                (searchId) ->
                emit(LoadingSearchResultsUpdate)
                emit(AutocompleteSearchResultsLoadedUpdate(getAutocompleteSearchResults(searchId)))
            },
            filterIsInstance<MainIntent.SearchQueryChanged>().autocompleteSearchUpdates,
            filterIsInstance()
        )

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

    private val Flow<MainIntent.GetAttractions>.attractionsAroundUpdates:
        Flow<(MainState) -> MainState>
        get() =
            withLatestFrom(isConnectedFlow()) { _, isConnected -> isConnected }
                .transformLatest { isConnected ->
                    val currentLocation = state.locationState
                    if (currentLocation !is WithValue) {
                        signal(MainSignal.UnableToLoadPlacesWithoutLocation)
                        return@transformLatest
                    }

                    if (!isConnected) {
                        signal(MainSignal.UnableToLoadPlacesWithoutConnection)
                        return@transformLatest
                    }

                    emitPlacesUpdates(::SearchAroundResultsLoadedUpdate) {
                        getAttractionsAround(
                            lat = currentLocation.value.latitude.roundedTo2DecimalPlaces,
                            lng = currentLocation.value.longitude.roundedTo2DecimalPlaces,
                            radiusInMeters = PLACES_LOADING_RADIUS_METERS
                        )
                    }
                }

    private val Flow<MainIntent.SearchQueryChanged>.autocompleteSearchUpdates:
        Flow<(MainState) -> MainState>
        get() =
            map { (query) -> query.trim() }
                .filterNot { query -> query.isBlank() || query.count(Char::isLetterOrDigit) <= 3 }
                .distinctUntilChanged()
                .debounce(500L)
                .withLatestFrom(isConnectedFlow()) { query, isConnected -> query to isConnected }
                .transformLatest { (query, isConnected) ->
                    val currentLocation = state.locationState
                    if (currentLocation !is WithValue) {
                        signal(MainSignal.UnableToLoadPlacesWithoutLocation)
                        return@transformLatest
                    }

                    if (!isConnected) {
                        signal(MainSignal.UnableToLoadPlacesWithoutConnection)
                        return@transformLatest
                    }

                    emitPlacesUpdates(::AutocompleteSearchResultsLoadedUpdate) {
                        autocompleteSearch(
                            query = query,
                            priorityLat = currentLocation.value.latitude.roundedTo2DecimalPlaces,
                            priorityLon = currentLocation.value.longitude.roundedTo2DecimalPlaces
                        )
                    }
                }

    private suspend fun <T> FlowCollector<(MainState) -> MainState>.emitPlacesUpdates(
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

    @AssistedFactory interface Factory : StateContainerFactory<MainStateContainer>

    companion object {
        private const val LOCATION_UPDATES_INTERVAL_MILLIS = 5_000L
        private const val PLACES_LOADING_TIMEOUT_MILLIS = 10_000L
        private const val PLACES_LOADING_RADIUS_METERS = 5_000f

        private val Double.roundedTo2DecimalPlaces: Double
            get() = roundToDecimalPlaces(2).toDouble()
    }
}
