package com.lookaround.ui.recent.searches

import androidx.lifecycle.SavedStateHandle
import com.lookaround.core.android.base.arch.FlowViewModel
import com.lookaround.core.android.base.arch.SavedStateViewModelFactory
import com.lookaround.core.android.ext.initialState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class RecentSearchesViewModel
@AssistedInject
constructor(@Assisted savedStateHandle: SavedStateHandle, processor: RecentSearchesFlowProcessor) :
    FlowViewModel<
        RecentSearchesIntent, RecentSearchesStateUpdate, RecentSearchesState, RecentSearchesSignal>(
        savedStateHandle.initialState(),
        processor,
        savedStateHandle
    ) {
    @AssistedFactory interface Factory : SavedStateViewModelFactory<RecentSearchesViewModel>
}
