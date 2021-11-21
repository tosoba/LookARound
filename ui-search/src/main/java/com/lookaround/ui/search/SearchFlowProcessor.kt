package com.lookaround.ui.search

import com.lookaround.core.android.architecture.FlowProcessor
import com.lookaround.core.android.ext.roundToDecimalPlaces
import com.lookaround.core.usecase.SearchPoints
import com.lookaround.ui.search.model.*
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.transformLatest

@ExperimentalCoroutinesApi
class SearchFlowProcessor
@Inject
constructor(
    private val searchPoints: SearchPoints,
) : FlowProcessor<SearchIntent, SearchState, SearchSignal> {
    override fun updates(
        coroutineScope: CoroutineScope,
        intents: Flow<SearchIntent>,
        currentState: () -> SearchState,
        states: Flow<SearchState>,
        intent: suspend (SearchIntent) -> Unit,
        signal: suspend (SearchSignal) -> Unit
    ): Flow<(SearchState) -> SearchState> =
        intents.filterIsInstance<SearchIntent.SearchPlaces>().transformLatest {
            (query, priorityLocation) ->
            when {
                query.isBlank() -> emit(BlankQueryUpdate)
                query.count(Char::isLetterOrDigit) <= 3 -> emit(QueryTooShortUpdate)
                else -> {
                    emit(LoadingPlaces)
                    try {
                        emit(
                            PlacesLoaded(
                                points =
                                    searchPoints(
                                        query = query,
                                        priorityLat =
                                            priorityLocation
                                                ?.latitude
                                                ?.roundToDecimalPlaces(3)
                                                ?.toDouble(),
                                        priorityLon =
                                            priorityLocation
                                                ?.longitude
                                                ?.roundToDecimalPlaces(3)
                                                ?.toDouble()
                                    ),
                                withLocationPriority = priorityLocation != null
                            )
                        )
                    } catch (throwable: Throwable) {
                        emit(PlacesLoadingError(throwable))
                    }
                }
            }
        }
}
