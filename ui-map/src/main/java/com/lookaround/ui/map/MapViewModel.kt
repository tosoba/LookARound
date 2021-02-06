package com.lookaround.ui.map

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.lookaround.core.android.base.arch.FlowViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@ExperimentalCoroutinesApi
@FlowPreview
class MapViewModel @AssistedInject constructor(
    @Assisted initialState: MapState,
    @Assisted savedStateHandle: SavedStateHandle,
    processor: MapFlowProcessor
) : FlowViewModel<MapIntent, MapStateUpdate, MapState, MapSignal>(
    initialState, processor, savedStateHandle
) {
    @AssistedFactory
    interface Factory {
        fun create(initialState: MapState, savedStateHandle: SavedStateHandle): MapViewModel
    }

    companion object {
        fun provideFactory(
            factory: Factory,
            owner: SavedStateRegistryOwner,
            initialState: MapState,
            defaultArgs: Bundle? = null
        ): AbstractSavedStateViewModelFactory =
            object : AbstractSavedStateViewModelFactory(owner, defaultArgs) {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel?> create(
                    key: String, modelClass: Class<T>, handle: SavedStateHandle
                ): T = factory.create(initialState, handle) as T
            }
    }
}
