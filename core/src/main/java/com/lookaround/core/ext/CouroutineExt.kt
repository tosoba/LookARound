package com.lookaround.core.ext

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
inline fun <T> Deferred<T>.ifCompleted(block: T.() -> Unit) {
    if (isCompleted) getCompleted().block()
}
