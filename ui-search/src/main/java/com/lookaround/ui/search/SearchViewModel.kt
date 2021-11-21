package com.lookaround.ui.search

import androidx.lifecycle.SavedStateHandle
import com.lookaround.core.android.architecture.FlowViewModel
import com.lookaround.core.android.architecture.SavedStateViewModelFactory
import com.lookaround.core.android.ext.initialState
import com.lookaround.ui.search.model.SearchIntent
import com.lookaround.ui.search.model.SearchSignal
import com.lookaround.ui.search.model.SearchState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class SearchViewModel
@AssistedInject
constructor(
    @Assisted savedStateHandle: SavedStateHandle,
    processor: SearchFlowProcessor,
) :
    FlowViewModel<SearchIntent, SearchState, SearchSignal>(
        savedStateHandle.initialState(),
        savedStateHandle,
        processor
    ) {
    @AssistedFactory interface Factory : SavedStateViewModelFactory<SearchViewModel>
}
