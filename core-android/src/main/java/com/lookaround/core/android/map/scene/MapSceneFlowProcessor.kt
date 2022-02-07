package com.lookaround.core.android.map.scene

import com.lookaround.core.android.architecture.FlowProcessor
import com.lookaround.core.android.map.scene.model.*
import com.lookaround.core.usecase.IsConnectedFlow
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

@ExperimentalCoroutinesApi
class MapSceneFlowProcessor
@Inject
constructor(
    private val isConnectedFlow: IsConnectedFlow,
) : FlowProcessor<MapSceneIntent, MapSceneState, MapSceneSignal> {
    override fun updates(
        coroutineScope: CoroutineScope,
        intents: Flow<MapSceneIntent>,
        currentState: () -> MapSceneState,
        signal: suspend (MapSceneSignal) -> Unit
    ): Flow<(MapSceneState) -> MapSceneState> =
        merge(
            intents.filterIsInstance<MapSceneIntent.LoadingScene>().transformLatest<
                    MapSceneIntent.LoadingScene, (MapSceneState) -> MapSceneState> {
                emit(LoadingSceneUpdate(it.scene))
                delay(SCENE_LOADING_TIME_LIMIT_MS)
                if (!currentState().sceneLoaded) emit(SceneLoadingTimeoutUpdate)
            },
            intents.filterIsInstance<MapSceneIntent.SceneLoaded>(),
        )

    override fun sideEffects(
        coroutineScope: CoroutineScope,
        intents: Flow<MapSceneIntent>,
        states: Flow<MapSceneState>,
        currentState: () -> MapSceneState,
        signal: suspend (MapSceneSignal) -> Unit
    ) {
        states
            .filter(MapSceneState::sceneLoadingTimeoutOccurred::get)
            .combine(isConnectedFlow().filter { it }) { state, _ ->
                signal(MapSceneSignal.RetryLoadScene(state.scene))
            }
            .launchIn(coroutineScope)
    }

    companion object {
        private const val SCENE_LOADING_TIME_LIMIT_MS: Long = 5_000L
    }
}
