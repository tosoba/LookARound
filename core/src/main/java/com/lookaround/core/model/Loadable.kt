package com.lookaround.core.model

sealed class Loadable<out T> {
    open val copyWithLoadingInProgress: Loadable<T> get() = LoadingFirst
    open val copyWithClearedError: Loadable<T> get() = Empty
    open fun copyWithError(error: Any?): Loadable<T> = FailedFirst(error)
}

sealed class WithValue<out T> : Loadable<T>() {
    abstract val value: T
}

sealed class WithoutValue : Loadable<Nothing>()

object Empty : WithoutValue()

interface LoadingInProgress
object LoadingFirst : WithoutValue(), LoadingInProgress
data class LoadingNext<out T>(override val value: T) : WithValue<T>(), LoadingInProgress {
    override val copyWithLoadingInProgress: Loadable<T> get() = this
    override val copyWithClearedError: Loadable<T> get() = this
    override fun copyWithError(error: Any?): FailedNext<T> = FailedNext(value, error)
}

data class Ready<out T>(override val value: T) : WithValue<T>() {
    override val copyWithLoadingInProgress: LoadingNext<T> get() = LoadingNext(value)
    override val copyWithClearedError: Loadable<T> get() = this
    override fun copyWithError(error: Any?): FailedNext<T> = FailedNext(value, error)
}

interface Failed {
    val error: Any?
}

data class FailedNext<out T>(override val value: T, override val error: Any?) : WithValue<T>(), Failed {
    override val copyWithClearedError: Ready<T> get() = Ready(value)
    override val copyWithLoadingInProgress: Loadable<T> get() = LoadingNext(value)
    override fun copyWithError(error: Any?): FailedNext<T> = FailedNext(value, error)
}

data class FailedFirst(override val error: Any?) : WithoutValue(), Failed {
    override val copyWithLoadingInProgress: LoadingFirst get() = LoadingFirst
}
