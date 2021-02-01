package com.lookaround.core.android.base.arch

interface StateUpdate<State : Any> {
    operator fun invoke(state: State): State
}