package com.lookaround.ui.main.transfomer

import com.lookaround.core.android.architecture.FlowTransformer
import com.lookaround.core.android.ext.roundToDecimalPlaces
import com.lookaround.core.android.model.WithValue
import com.lookaround.core.ext.withLatestFrom
import com.lookaround.core.usecase.AutocompleteSearch
import com.lookaround.core.usecase.GetAttractionsAround
import com.lookaround.core.usecase.GetPlacesOfTypeAround
import com.lookaround.core.usecase.IsConnectedFlow
import com.lookaround.ui.main.model.*
import dagger.Reusable
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withTimeout

@ExperimentalCoroutinesApi
@FlowPreview
@Reusable
class GetPlacesOfTypeUpdates
@Inject
constructor(
    private val isConnectedFlow: IsConnectedFlow,
    private val getPlacesOfTypeAround: GetPlacesOfTypeAround,
) : FlowTransformer<MainState, MainIntent.GetPlacesOfType, MainSignal> {
    override operator fun invoke(
        flow: Flow<MainIntent.GetPlacesOfType>,
        currentState: () -> MainState,
        signal: suspend (MainSignal) -> Unit,
    ): Flow<MainState.() -> MainState> =
        flow
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

                emitPlacesUpdates(signal, ::SearchAroundResultsLoadedUpdate) {
                    getPlacesOfTypeAround(
                        placeType = placeType,
                        lat = currentLocation.value.latitude.roundedTo2DecimalPlaces,
                        lng = currentLocation.value.longitude.roundedTo2DecimalPlaces,
                        radiusInMeters = PLACES_LOADING_RADIUS_METERS
                    )
                }
            }
}

@ExperimentalCoroutinesApi
@FlowPreview
@Reusable
class GetAttractionsUpdates
@Inject
constructor(
    private val isConnectedFlow: IsConnectedFlow,
    private val getAttractionsAround: GetAttractionsAround,
) : FlowTransformer<MainState, MainIntent.GetAttractions, MainSignal> {
    override fun invoke(
        flow: Flow<MainIntent.GetAttractions>,
        currentState: () -> MainState,
        signal: suspend (MainSignal) -> Unit
    ): Flow<MainState.() -> MainState> =
        flow
            .withLatestFrom(isConnectedFlow()) { _, isConnected -> isConnected }
            .transformLatest { isConnected ->
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
}

@ExperimentalCoroutinesApi
@FlowPreview
@Reusable
class AutocompleteSearchUpdates
@Inject
constructor(
    private val isConnectedFlow: IsConnectedFlow,
    private val autocompleteSearch: AutocompleteSearch,
) : FlowTransformer<MainState, MainIntent.SearchQueryChanged, MainSignal> {
    override fun invoke(
        flow: Flow<MainIntent.SearchQueryChanged>,
        currentState: () -> MainState,
        signal: suspend (MainSignal) -> Unit
    ): Flow<MainState.() -> MainState> =
        flow
            .map { (query) -> query.trim() }
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
}

@ExperimentalCoroutinesApi
@FlowPreview
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

private const val PLACES_LOADING_TIMEOUT_MILLIS = 10_000L
private const val PLACES_LOADING_RADIUS_METERS = 5_000f

private val Double.roundedTo2DecimalPlaces: Double
    get() = roundToDecimalPlaces(2).toDouble()
