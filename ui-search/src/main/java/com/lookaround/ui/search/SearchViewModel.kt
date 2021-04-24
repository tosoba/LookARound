package com.lookaround.ui.search

import androidx.lifecycle.SavedStateHandle
import com.lookaround.core.android.base.arch.FlowViewModel
import com.lookaround.core.android.base.arch.SavedStateViewModelFactory
import com.lookaround.core.android.ext.initialState
import com.lookaround.ui.search.model.SearchIntent
import com.lookaround.ui.search.model.SearchSignal
import com.lookaround.ui.search.model.SearchState
import com.lookaround.ui.search.model.SearchStateUpdate
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@ExperimentalCoroutinesApi
@FlowPreview
class SearchViewModel
@AssistedInject
constructor(@Assisted savedStateHandle: SavedStateHandle, processor: SearchFlowProcessor) :
    FlowViewModel<SearchIntent, SearchStateUpdate, SearchState, SearchSignal>(
        savedStateHandle.initialState(),
        processor,
        savedStateHandle
    ) {
    @AssistedFactory interface Factory : SavedStateViewModelFactory<SearchViewModel>
}
