package com.lookaround.ui.main

import com.lookaround.core.android.ext.roundToDecimalPlaces
import com.lookaround.core.android.model.WithValue
import com.lookaround.core.ext.withLatestFrom
import com.lookaround.core.usecase.GetPlacesOfTypeAround
import com.lookaround.core.usecase.IsConnectedFlow
import com.lookaround.ui.main.model.*
import dagger.Reusable
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.withTimeout

@ExperimentalCoroutinesApi
@FlowPreview
@Reusable
class GetPlacesOfTypeUpdates
@Inject
constructor(
    private val isConnectedFlow: IsConnectedFlow,
    private val getPlacesOfTypeAround: GetPlacesOfTypeAround,
) {
    operator fun invoke(
        intents: Flow<MainIntent.GetPlacesOfType>,
        stateContainer: MainStateContainer,
    ): Flow<MainState.() -> MainState> =
        intents
            .withLatestFrom(isConnectedFlow()) { (placeType), isConnected ->
                placeType to isConnected
            }
            .transformLatest { (placeType, isConnected) ->
                val currentLocation = stateContainer.state.locationState
                if (currentLocation !is WithValue) {
                    stateContainer.signal(MainSignal.UnableToLoadPlacesWithoutLocation)
                    return@transformLatest
                }

                if (!isConnected) {
                    stateContainer.signal(MainSignal.UnableToLoadPlacesWithoutConnection)
                    return@transformLatest
                }

                emitPlacesUpdates(stateContainer, ::SearchAroundResultsLoadedUpdate) {
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
private suspend fun <T> FlowCollector<(MainState) -> MainState>.emitPlacesUpdates(
    stateContainer: MainStateContainer,
    placesLoadedUpdate: (Iterable<T>) -> (MainState) -> MainState,
    getPlaces: suspend () -> Collection<T>
) {
    emit(LoadingSearchResultsUpdate)
    try {
        val places = withTimeout(PLACES_LOADING_TIMEOUT_MILLIS) { getPlaces() }
        if (places.isEmpty()) {
            stateContainer.signal(MainSignal.NoPlacesFound)
            emit(NoPlacesFoundUpdate)
        } else {
            emit(placesLoadedUpdate(places))
        }
    } catch (ex: Exception) {
        emit(SearchErrorUpdate(ex))
        stateContainer.signal(MainSignal.PlacesLoadingFailed(ex))
    }
}

private const val LOCATION_UPDATES_INTERVAL_MILLIS = 5_000L
private const val PLACES_LOADING_TIMEOUT_MILLIS = 10_000L
private const val PLACES_LOADING_RADIUS_METERS = 5_000f

private val Double.roundedTo2DecimalPlaces: Double
    get() = roundToDecimalPlaces(2).toDouble()
