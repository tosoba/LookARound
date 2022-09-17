package com.lookaround.ui.main.transfomer

import com.lookaround.core.android.architecture.FlowTransformer
import com.lookaround.core.usecase.GetAutocompleteSearchResults
import com.lookaround.core.usecase.GetSearchAroundResults
import com.lookaround.ui.main.model.*
import dagger.Reusable
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformLatest

@ExperimentalCoroutinesApi
@FlowPreview
@Reusable
class SearchAroundResultsUpdates
@Inject
constructor(
    private val getSearchAroundResults: GetSearchAroundResults,
) : FlowTransformer<MainState, MainIntent.LoadSearchAroundResults, MainSignal> {
    override fun invoke(
        flow: Flow<MainIntent.LoadSearchAroundResults>,
        currentState: () -> MainState,
        signal: suspend (MainSignal) -> Unit
    ): Flow<MainState.() -> MainState> =
        flow.transformLatest { (searchId) ->
            emit(LoadingSearchResultsUpdate)
            emit(SearchAroundResultsLoadedUpdate(getSearchAroundResults(searchId)))
        }
}

@ExperimentalCoroutinesApi
@FlowPreview
@Reusable
class SearchAutocompleteResults
@Inject
constructor(
    private val getAutocompleteSearchResults: GetAutocompleteSearchResults,
) : FlowTransformer<MainState, MainIntent.LoadSearchAutocompleteResults, MainSignal> {
    override fun invoke(
        flow: Flow<MainIntent.LoadSearchAutocompleteResults>,
        currentState: () -> MainState,
        signal: suspend (MainSignal) -> Unit
    ): Flow<MainState.() -> MainState> =
        flow.transformLatest { (searchId) ->
            emit(LoadingSearchResultsUpdate)
            emit(AutocompleteSearchResultsLoadedUpdate(getAutocompleteSearchResults(searchId)))
        }
}
