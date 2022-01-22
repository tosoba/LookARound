package com.lookaround.ui.recent.searches

import com.lookaround.core.android.architecture.FlowProcessor
import com.lookaround.core.android.model.Empty
import com.lookaround.core.android.model.Ready
import com.lookaround.core.usecase.DeleteSearch
import com.lookaround.core.usecase.GetSearchesCount
import com.lookaround.core.usecase.RecentSearchesFlow
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@ExperimentalCoroutinesApi
class RecentSearchesFlowProcessor
@Inject
constructor(
    private val recentSearchesFlow: RecentSearchesFlow,
    private val getSearchesCount: GetSearchesCount,
    private val deleteSearch: DeleteSearch,
) : FlowProcessor<RecentSearchesIntent, RecentSearchesState, RecentSearchesSignal> {
    override fun updates(
        coroutineScope: CoroutineScope,
        intents: Flow<RecentSearchesIntent>,
        currentState: () -> RecentSearchesState,
        states: Flow<RecentSearchesState>,
        intent: suspend (RecentSearchesIntent) -> Unit,
        signal: suspend (RecentSearchesSignal) -> Unit
    ): Flow<(RecentSearchesState) -> RecentSearchesState> =
        intents
            .filterIsInstance<RecentSearchesIntent.LoadSearches>()
            .onStart { emit(RecentSearchesIntent.LoadSearches(null)) }
            .mapLatest { (query) -> query to getSearchesCount(query) }
            .scan(Triple<String?, String?, Int>(null, null, -1)) {
                (_, previousQuery, _),
                (currentQuery, count) ->
                val trimmed = currentQuery?.trim()
                Triple(previousQuery, if (trimmed?.isBlank() == true) null else trimmed, count)
            }
            .map { (previousQuery, currentQuery, totalSearchesCount) ->
                if (previousQuery != currentQuery) {
                    currentQuery to RecentSearchesState.SEARCHES_LIMIT_INCREMENT
                } else {
                    val (searches) = currentState()
                    when (searches) {
                        is Ready -> {
                            val increment = RecentSearchesState.SEARCHES_LIMIT_INCREMENT
                            val nextLimit =
                                searches.value.items.size / increment * increment + increment
                            if (totalSearchesCount > nextLimit) currentQuery to nextLimit else null
                        }
                        is Empty -> currentQuery to RecentSearchesState.SEARCHES_LIMIT_INCREMENT
                        else -> null
                    }
                }
            }
            .filterNotNull()
            .distinctUntilChanged()
            .transformLatest { (query, limit) ->
                emit(LoadingSearchesUpdate)
                emitAll(
                    recentSearchesFlow(limit, query)
                        .map(::SearchesLoadedUpdate)
                        .distinctUntilChanged()
                )
            }

    override fun sideEffects(
        coroutineScope: CoroutineScope,
        intents: Flow<RecentSearchesIntent>,
        currentState: () -> RecentSearchesState,
        states: Flow<RecentSearchesState>,
        signal: suspend (RecentSearchesSignal) -> Unit
    ) {
        intents
            .filterIsInstance<RecentSearchesIntent.DeleteSearch>()
            .onEach { (id, type) -> deleteSearch(id, type) }
            .launchIn(coroutineScope)
    }
}
