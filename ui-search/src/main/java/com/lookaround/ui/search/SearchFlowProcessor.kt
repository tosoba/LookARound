package com.lookaround.ui.search

import com.lookaround.core.android.base.arch.FlowProcessor
import com.lookaround.core.android.ext.roundToDecimalPlaces
import com.lookaround.core.usecase.SearchPoints
import com.lookaround.ui.search.model.SearchIntent
import com.lookaround.ui.search.model.SearchSignal
import com.lookaround.ui.search.model.SearchState
import com.lookaround.ui.search.model.SearchStateUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.transformLatest
import javax.inject.Inject

@ExperimentalCoroutinesApi
class SearchFlowProcessor
@Inject
constructor(
    private val searchPoints: SearchPoints,
) : FlowProcessor<SearchIntent, SearchStateUpdate, SearchState, SearchSignal> {
    override fun updates(
        coroutineScope: CoroutineScope,
        intents: Flow<SearchIntent>,
        currentState: () -> SearchState,
        states: Flow<SearchState>,
        intent: suspend (SearchIntent) -> Unit,
        signal: suspend (SearchSignal) -> Unit
    ): Flow<SearchStateUpdate> =
        intents.filterIsInstance<SearchIntent.SearchPlaces>().transformLatest {
            (query, priorityLocation) ->
            when {
                query.isBlank() -> emit(SearchStateUpdate.BlankQueryUpdate)
                query.count(Char::isLetterOrDigit) <= 3 -> {
                    emit(SearchStateUpdate.QueryTooShortUpdate)
                }
                else -> {
                    emit(SearchStateUpdate.LoadingPlaces)
                    try {
                        emit(
                            SearchStateUpdate.PlacesLoaded(
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
                        emit(SearchStateUpdate.PlacesLoadingError(throwable))
                    }
                }
            }
        }
}
