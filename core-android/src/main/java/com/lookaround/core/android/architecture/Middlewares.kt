package com.lookaround.core.android.architecture

import com.lookaround.core.android.BuildConfig
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

abstract class Middleware<T : Any>(val order: Int = 0) : (T) -> Unit

class DebugLoggingMiddleware<T : Any>(val tag: String, order: Int = 0) : Middleware<T>(order) {
    override fun invoke(t: T) {
        if (BuildConfig.DEBUG) Timber.tag(tag).d(t.toString())
    }
}

class IdlingResourceMiddlewares<INTENT : Any, STATE : Any>(
    private val shouldIncrementOn: (INTENT) -> Boolean,
    private val shouldDecrementOn: (STATE) -> Boolean
) {
    private val counter = AtomicInteger(0)

    fun intentMiddleware(order: Int = 0): Middleware<INTENT> =
        object : Middleware<INTENT>(order) {
            override fun invoke(intent: INTENT) {
                if (shouldIncrementOn(intent)) increment()
            }
        }

    fun stateMiddleware(order: Int = 0): Middleware<STATE> =
        object : Middleware<STATE>(order) {
            override fun invoke(state: STATE) {
                if (shouldDecrementOn(state)) decrement()
            }
        }

    private fun increment() {
        counter.incrementAndGet()
    }

    private fun decrement() {
        counter.decrementAndGet()
    }

    fun isIdle(): Boolean = counter.get() == 0
}

fun <T : Any> Flow<T>.runMiddlewares(middlewares: Collection<Middleware<T>>): Flow<T> = run {
    if (middlewares.isEmpty()) {
        this
    } else {
        onEach {
            middlewares.sortedBy(Middleware<T>::order).forEach { middleware ->
                middleware.invoke(it)
            }
        }
    }
}
