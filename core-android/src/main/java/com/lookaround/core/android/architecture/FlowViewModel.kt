package com.lookaround.core.android.architecture

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lookaround.core.android.BuildConfig
import kotlin.reflect.KProperty1
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import timber.log.Timber

@ExperimentalCoroutinesApi
abstract class FlowViewModel<Intent : Any, State : Any, Signal : Any>(
    initialState: State,
    savedStateHandle: SavedStateHandle,
    processor: FlowProcessor<Intent, State, Signal>
) : ViewModel() {
    private val mutableSignals: MutableSharedFlow<Signal> = MutableSharedFlow()
    val signals: Flow<Signal>
        get() = mutableSignals
    suspend fun signal(signal: Signal) = mutableSignals.emit(signal)

    private val mutableIntents: MutableSharedFlow<Intent> = MutableSharedFlow()
    suspend fun intent(intent: Intent) = mutableIntents.emit(intent)

    private val mutableStates: MutableStateFlow<State> = MutableStateFlow(initialState)
    val states: StateFlow<State>
        get() = mutableStates
    var state: State
        private set(value) = value.let(mutableStates::value::set)
        get() = mutableStates.value
    fun <V> mapStates(property: KProperty1<State, V>): Flow<V> = states.map(property::get)

    init {
        processor
            .updates(
                intents = mutableIntents,
                currentState = states::value,
                signal = ::signal
            )
            .run {
                if (BuildConfig.DEBUG && BuildConfig.LOG_STATES_UPDATES_FLOW) {
                    onEach { Timber.tag("STATE_UPDATE").d(it.toString()) }
                } else {
                    this
                }
            }
            .scan(initialState) { currentState, update ->
                val nextState = update(currentState)
                processor.stateWillUpdate(currentState, nextState, update, savedStateHandle)
                nextState
            }
            .run {
                if (BuildConfig.DEBUG && BuildConfig.LOG_STATES_FLOW) {
                    onEach { Timber.tag("NEW_STATE").d(it.toString()) }
                } else {
                    this
                }
            }
            .onEach(::state::set)
            .launchIn(viewModelScope)

        processor.sideEffects(
            coroutineScope = viewModelScope,
            intents = mutableIntents,
            states = states,
            currentState = states::value,
            signal = ::signal
        )
    }

    inline fun <reified S : Signal> filterSignals(): Flow<S> = signals.filterIsInstance()

    inline fun <reified S : Signal, V> filterSignals(property: KProperty1<S, V>): Flow<V> =
        filterSignals<S>().map(property::get)

    inline fun <reified S : Signal> onEachSignal(noinline block: suspend (S) -> Unit): Flow<S> =
        filterSignals<S>().onEach(block)

    inline fun <reified S : Signal, V> onEachSignal(
        property: KProperty1<S, V>,
        noinline block: suspend (V) -> Unit
    ): Flow<V> = filterSignals<S>().map(property::get).onEach(block)
}
