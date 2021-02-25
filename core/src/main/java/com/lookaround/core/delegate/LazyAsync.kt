package com.lookaround.core.delegate

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

fun <T> CoroutineScope.lazyAsync(
    mode: LazyThreadSafetyMode = LazyThreadSafetyMode.SYNCHRONIZED,
    block: suspend CoroutineScope.() -> T
): Lazy<Deferred<T>> = lazy(mode) { async(start = CoroutineStart.LAZY, block = block) }
