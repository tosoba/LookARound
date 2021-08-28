package com.lookaround.core.android.map.scene

import com.lookaround.core.android.base.arch.FlowProcessor
import com.lookaround.core.android.map.scene.model.MapSceneIntent
import com.lookaround.core.android.map.scene.model.MapSceneSignal
import com.lookaround.core.android.map.scene.model.MapSceneState
import com.lookaround.core.android.map.scene.model.MapSceneStateUpdate
import com.lookaround.core.usecase.IsConnectedFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@ExperimentalCoroutinesApi
class MapSceneFlowProcessor
@Inject
constructor(
    private val isConnectedFlow: IsConnectedFlow,
) : FlowProcessor<MapSceneIntent, MapSceneStateUpdate, MapSceneState, MapSceneSignal> {
    override fun updates(
        coroutineScope: CoroutineScope,
        intents: Flow<MapSceneIntent>,
        currentState: () -> MapSceneState,
        states: Flow<MapSceneState>,
        intent: suspend (MapSceneIntent) -> Unit,
        signal: suspend (MapSceneSignal) -> Unit
    ): Flow<MapSceneStateUpdate> =
        merge(
            intents.filterIsInstance<MapSceneIntent.LoadingScene>().transformLatest {
                emit(MapSceneStateUpdate.LoadingScene(it.scene))
                delay(SCENE_LOADING_TIME_LIMIT_MS)
                if (!currentState().sceneLoaded)
                    emit(MapSceneStateUpdate.SceneLoadingTimeoutOccurred)
            },
            intents.filterIsInstance<MapSceneIntent.SceneLoaded>().map {
                MapSceneStateUpdate.SceneLoaded
            },
        )

    override fun sideEffects(
        coroutineScope: CoroutineScope,
        currentState: () -> MapSceneState,
        states: Flow<MapSceneState>,
        signal: suspend (MapSceneSignal) -> Unit
    ) {
        states
            .filter { it.sceneLoadingTimeoutOccurred }
            .combine(isConnectedFlow().filter { it }) { state, _ ->
                signal(MapSceneSignal.RetryLoadScene(state.scene))
            }
            .launchIn(coroutineScope)
    }

    companion object {
        private const val SCENE_LOADING_TIME_LIMIT_MS: Long = 5_000L
    }
}
