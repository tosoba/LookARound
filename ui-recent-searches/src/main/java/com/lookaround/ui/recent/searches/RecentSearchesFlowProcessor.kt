package com.lookaround.ui.recent.searches

import com.lookaround.core.android.architecture.FlowProcessor
import com.lookaround.core.ext.withLatestFrom
import com.lookaround.core.usecase.RecentSearchesFlow
import com.lookaround.core.usecase.TotalSearchesCountFlow
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@ExperimentalCoroutinesApi
class RecentSearchesFlowProcessor
@Inject
constructor(
    private val recentSearchesFlow: RecentSearchesFlow,
    private val totalSearchesCountFlow: TotalSearchesCountFlow
) : FlowProcessor<RecentSearchesIntent, RecentSearchesState, RecentSearchesSignal> {
    override fun updates(
        coroutineScope: CoroutineScope,
        intents: Flow<RecentSearchesIntent>,
        currentState: () -> RecentSearchesState,
        states: Flow<RecentSearchesState>,
        intent: suspend (RecentSearchesIntent) -> Unit,
        signal: suspend (RecentSearchesSignal) -> Unit
    ): Flow<(RecentSearchesState) -> RecentSearchesState> =
        merge(
            intents
                .filterIsInstance<RecentSearchesIntent.IncreaseLimit>()
                .withLatestFrom(totalSearchesCountFlow()) { _, totalSearchesCount -> totalSearchesCount }
                .filter { totalSearchesCount ->
                    val (_, limit) = currentState()
                    totalSearchesCount > limit + RecentSearchesState.SEARCHES_LIMIT_INCREMENT
                }
                .map { IncreaseLimitUpdate() },
            states.map(RecentSearchesState::limit::get).distinctUntilChanged().transformLatest {
                limit ->
                emit(LoadingSearchesUpdate)
                emitAll(recentSearchesFlow(limit).map(::SearchesLoadedUpdate))
            }
        )
}
