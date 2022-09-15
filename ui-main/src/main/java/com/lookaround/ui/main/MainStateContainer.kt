package com.lookaround.ui.main

import androidx.lifecycle.SavedStateHandle
import com.lookaround.core.android.architecture.MviFlowStateContainer
import com.lookaround.core.android.architecture.StateContainerFactory
import com.lookaround.core.android.ext.roundToDecimalPlaces
import com.lookaround.core.usecase.GetAutocompleteSearchResults
import com.lookaround.core.usecase.GetSearchAroundResults
import com.lookaround.core.usecase.TotalSearchesCountFlow
import com.lookaround.ui.main.model.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withTimeout

@ExperimentalCoroutinesApi
@FlowPreview
class MainStateContainer
@AssistedInject
constructor(
    @Assisted savedStateHandle: SavedStateHandle,
    private val getPlacesOfTypeUpdates: GetPlacesOfTypeUpdates,
    private val getAttractionsUpdates: GetAttractionsUpdates,
    private val locationStateUpdates: LocationStateUpdates,
    private val autocompleteSearchUpdates: AutocompleteSearchUpdates,
    private val totalSearchesCountFlow: TotalSearchesCountFlow,
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
            filterIsInstance<MainIntent.GetAttractions>().mapTo(getAttractionsUpdates),
            filterIsInstance<MainIntent.LocationPermissionGranted>().mapTo(locationStateUpdates),
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
            filterIsInstance<MainIntent.SearchQueryChanged>().mapTo(autocompleteSearchUpdates),
            filterIsInstance()
        )

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
        private const val PLACES_LOADING_TIMEOUT_MILLIS = 10_000L

        private val Double.roundedTo2DecimalPlaces: Double
            get() = roundToDecimalPlaces(2).toDouble()
    }
}
