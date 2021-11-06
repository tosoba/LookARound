package com.lookaround.ui.search

import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.model.MainState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@FlowPreview
@ExperimentalCoroutinesApi
internal val MainViewModel.searchQueryUpdates: Flow<String>
    get() =
        states
            .map(MainState::searchQuery::get)
            .debounce(500L)
            .map(String::trim)
            .distinctUntilChanged()
