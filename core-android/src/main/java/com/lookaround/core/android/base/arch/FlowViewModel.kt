package com.lookaround.core.android.base.arch

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lookaround.core.android.BuildConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import timber.log.Timber

@FlowPreview
@ExperimentalCoroutinesApi
abstract class FlowViewModel<Intent : Any, Update : StateUpdate<State>, State : Any, Signal : Any>(
    initialState: State,
    processor: FlowProcessor<Intent, Update, State, Signal>,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val signalsChannel: BroadcastChannel<Signal> = BroadcastChannel(Channel.BUFFERED)
    val signals: Flow<Signal>
        get() = signalsChannel.asFlow()
    suspend fun signal(signal: Signal) = signalsChannel.send(signal)

    private val intentsChannel: BroadcastChannel<Intent> = BroadcastChannel(Channel.CONFLATED)
    suspend fun intent(intent: Intent) = intentsChannel.send(intent)

    private val mutableStates: MutableStateFlow<State> = MutableStateFlow(initialState)
    val states: StateFlow<State>
        get() = mutableStates
    var state: State
        private set(value) = value.let(mutableStates::value::set)
        get() = mutableStates.value

    init {
        processor
            .updates(
                coroutineScope = viewModelScope,
                intents = intentsChannel.asFlow(),
                currentState = states::value,
                states = states,
                intent = ::intent,
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
            currentState = states::value,
            states = states,
            signal = ::signal
        )
    }

    override fun onCleared() {
        intentsChannel.close()
        signalsChannel.close()
        super.onCleared()
    }
}
