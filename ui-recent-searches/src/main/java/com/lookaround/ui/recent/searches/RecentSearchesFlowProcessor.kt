package com.lookaround.ui.recent.searches

import com.lookaround.core.android.base.arch.FlowProcessor
import com.lookaround.core.usecase.RecentSearchesFlow
import com.lookaround.core.usecase.SearchesCountFlow
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

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
    ): Flow<RecentSearchesStateUpdate> = emptyFlow()
}
