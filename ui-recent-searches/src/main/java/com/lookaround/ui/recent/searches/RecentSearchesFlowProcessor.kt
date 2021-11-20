package com.lookaround.ui.recent.searches

import com.lookaround.core.android.base.arch.FlowProcessor
import com.lookaround.core.ext.withLatestFrom
import com.lookaround.core.usecase.RecentSearchesFlow
import com.lookaround.core.usecase.SearchesCountFlow
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@ExperimentalCoroutinesApi
class RecentSearchesFlowProcessor
@Inject
constructor(
    private val recentSearchesFlow: RecentSearchesFlow,
    private val searchesCountFlow: SearchesCountFlow
) :
    FlowProcessor<
        RecentSearchesIntent,
        RecentSearchesStateUpdate,
        RecentSearchesState,
        RecentSearchesSignal> {

    override fun updates(
        coroutineScope: CoroutineScope,
        intents: Flow<RecentSearchesIntent>,
        currentState: () -> RecentSearchesState,
        states: Flow<RecentSearchesState>,
        intent: suspend (RecentSearchesIntent) -> Unit,
        signal: suspend (RecentSearchesSignal) -> Unit
    ): Flow<RecentSearchesStateUpdate> =
        merge(
            intents
                .filterIsInstance<RecentSearchesIntent.IncreaseLimit>()
                .withLatestFrom(searchesCountFlow()) { _, totalSearchesCount -> totalSearchesCount }
                .filter { totalSearchesCount ->
                    val (_, limit) = currentState()
                    totalSearchesCount > limit + RecentSearchesState.SEARCHES_LIMIT_INCREMENT
                }
                .map { RecentSearchesStateUpdate.IncreaseLimit() },
            states.map(RecentSearchesState::limit::get).distinctUntilChanged().transformLatest {
                limit ->
                emit(RecentSearchesStateUpdate.LoadingSearches)
                emitAll(recentSearchesFlow(limit).map(RecentSearchesStateUpdate::SearchesLoaded))
            }
        )
}
