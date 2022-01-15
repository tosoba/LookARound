package com.lookaround.ui.recent.searches

import com.lookaround.core.android.architecture.FlowProcessor
import com.lookaround.core.android.model.Empty
import com.lookaround.core.android.model.Ready
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
        intents
            .filterIsInstance<RecentSearchesIntent.LoadSearches>()
            .onStart { emit(RecentSearchesIntent.LoadSearches) }
            .withLatestFrom(totalSearchesCountFlow()) { _, totalSearchesCount ->
                totalSearchesCount
            }
            .map { totalSearchesCount ->
                val (searches) = currentState()
                when (searches) {
                    is Ready -> {
                        val increment = RecentSearchesState.SEARCHES_LIMIT_INCREMENT
                        val nextLimit =
                            searches.value.items.size / increment * increment + increment
                        if (totalSearchesCount > nextLimit) nextLimit else null
                    }
                    is Empty -> RecentSearchesState.SEARCHES_LIMIT_INCREMENT
                    else -> null
                }
            }
            .filterNotNull()
            .distinctUntilChanged()
            .transformLatest { limit ->
                emit(LoadingSearchesUpdate)
                emitAll(
                    recentSearchesFlow(limit).map(::SearchesLoadedUpdate).distinctUntilChanged()
                )
            }
}
