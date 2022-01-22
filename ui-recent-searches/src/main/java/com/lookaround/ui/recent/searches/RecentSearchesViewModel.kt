package com.lookaround.ui.recent.searches

import androidx.lifecycle.SavedStateHandle
import com.lookaround.core.android.architecture.FlowViewModel
import com.lookaround.core.android.ext.initialState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@HiltViewModel
class RecentSearchesViewModel
@Inject
constructor(savedStateHandle: SavedStateHandle, processor: RecentSearchesFlowProcessor) :
    FlowViewModel<RecentSearchesIntent, RecentSearchesState, RecentSearchesSignal>(
        savedStateHandle.initialState(),
        savedStateHandle,
        processor
    )
