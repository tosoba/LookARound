package com.lookaround.ui.main

import com.lookaround.core.android.base.arch.FlowProcessor
import com.lookaround.core.usecase.GetPlacesOfType
import com.lookaround.ui.main.model.MainIntent
import com.lookaround.ui.main.model.MainState
import com.lookaround.ui.main.model.MainStateUpdate
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.mapLatest

@ExperimentalCoroutinesApi
class MainFlowProcessor @Inject constructor(private val getPlacesOfType: GetPlacesOfType) :
    FlowProcessor<MainIntent, MainStateUpdate, MainState, Unit> {
    override fun updates(
        coroutineScope: CoroutineScope,
        intents: Flow<MainIntent>,
        currentState: () -> MainState,
        states: Flow<MainState>,
        intent: suspend (MainIntent) -> Unit,
        signal: suspend (Unit) -> Unit
    ): Flow<MainStateUpdate> =
        intents.filterIsInstance<MainIntent.LoadPlaces>().mapLatest { (type) ->
            try {
                val places = getPlacesOfType(type, 52.237049, 21.017532, 10_000f)
                MainStateUpdate.PlacesLoaded(places)
            } catch (throwable: Throwable) {
                MainStateUpdate.PlacesError(throwable)
            }
        }
}
