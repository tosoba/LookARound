package com.lookaround.core.android.architecture

import androidx.lifecycle.SavedStateHandle
import kotlin.reflect.KProperty1
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

interface IFlowStateContainer<STATE> {
    val states: Flow<STATE>
    val state: STATE
}

fun <STATE : Any, V> IFlowStateContainer<STATE>.mapStates(property: KProperty1<STATE, V>): Flow<V> =
    states.map(property::get)

abstract class FlowStateContainer<STATE : Any>(
    initialState: STATE,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    fromSavedState: (SavedStateHandle) -> STATE? = { null },
    private val saveState: SavedStateHandle.(STATE) -> Unit = {}
) : IFlowStateContainer<STATE> {
    private val mutableStates: MutableStateFlow<STATE> =
        MutableStateFlow(fromSavedState(savedStateHandle) ?: initialState)
    final override val states: Flow<STATE> = mutableStates
    final override val state: STATE
        get() = mutableStates.value

    protected fun updateState(state: STATE) {
        mutableStates.value = state.also { savedStateHandle.saveState(it) }
    }
}

interface IMviFlowStateContainer<STATE : Any, INTENT : Any, SIGNAL : Any> :
    IFlowStateContainer<STATE> {
    val signals: Flow<SIGNAL>
    suspend fun signal(effect: SIGNAL)
    suspend fun intent(intent: INTENT)

    fun launch(
        scope: CoroutineScope,
        intentMiddlewares: Collection<Middleware<INTENT>> = emptyList(),
        updateMiddlewares: Collection<Middleware<STATE.() -> STATE>> = emptyList(),
        stateMiddlewares: Collection<Middleware<STATE>> = emptyList()
    )
}

@ExperimentalCoroutinesApi
abstract class MviFlowStateContainer<STATE : Any, INTENT : Any, SIGNAL : Any>(
    initialState: STATE,
    savedStateHandle: SavedStateHandle = SavedStateHandle(),
    fromSavedState: (SavedStateHandle) -> STATE? = { null },
    saveState: SavedStateHandle.(STATE) -> Unit = {}
) :
    FlowStateContainer<STATE>(initialState, savedStateHandle, fromSavedState, saveState),
    IMviFlowStateContainer<STATE, INTENT, SIGNAL> {

    private val mutableSignals: MutableSharedFlow<SIGNAL> = MutableSharedFlow()
    final override val signals: Flow<SIGNAL>
        get() = mutableSignals
    final override suspend fun signal(effect: SIGNAL) = mutableSignals.emit(effect)

    private val mutableIntents: MutableSharedFlow<INTENT> = MutableSharedFlow()
    final override suspend fun intent(intent: INTENT) = mutableIntents.emit(intent)

    protected abstract fun Flow<INTENT>.updates(): Flow<STATE.() -> STATE>

    final override fun launch(
        scope: CoroutineScope,
        intentMiddlewares: Collection<Middleware<INTENT>>,
        updateMiddlewares: Collection<Middleware<STATE.() -> STATE>>,
        stateMiddlewares: Collection<Middleware<STATE>>
    ) {
        mutableIntents
            .runMiddlewares(intentMiddlewares)
            .updates()
            .runMiddlewares(updateMiddlewares)
            .scan(state) { currentState, update -> currentState.update() }
            .onEach(::updateState)
            .runMiddlewares(stateMiddlewares)
            .launchIn(scope)
    }
}

@ExperimentalCoroutinesApi
inline fun <reified S : Any> IMviFlowStateContainer<*, *, *>.filterSignals(): Flow<S> =
    signals.filterIsInstance()

@ExperimentalCoroutinesApi
inline fun <reified S : Any, T> IMviFlowStateContainer<*, *, *>.filterSignals(
    property: KProperty1<S, T>
): Flow<T> = filterSignals<S>().map(property::get)

@ExperimentalCoroutinesApi
inline fun <reified S : Any> IMviFlowStateContainer<*, *, *>.onEachSignal(
    noinline block: suspend (S) -> Unit
): Flow<S> = filterSignals<S>().onEach(block)

@ExperimentalCoroutinesApi
inline fun <reified S : Any, T> IMviFlowStateContainer<*, *, *>.onEachSignal(
    property: KProperty1<S, T>,
    noinline block: suspend (T) -> Unit
): Flow<T> = filterSignals<S>().map(property::get).onEach(block)
