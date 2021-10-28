package com.lookaround.core.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class Loadable<out T : Parcelable> : Parcelable {
    open val copyWithLoadingInProgress: Loadable<T>
        get() = LoadingFirst

    open val copyWithClearedError: Loadable<T>
        get() = Empty

    open fun copyWithError(error: Throwable?): Loadable<T> = FailedFirst(error)

    inline fun <reified E> isFailedWith(): Boolean = (this as? Failed)?.error is E
}

sealed class WithValue<T : Parcelable> : Loadable<T>() {
    abstract val value: T
    abstract fun map(block: (T) -> T): WithValue<T>
}

sealed class WithoutValue : Loadable<Nothing>()

@Parcelize object Empty : WithoutValue()

interface LoadingInProgress

@Parcelize object LoadingFirst : WithoutValue(), LoadingInProgress

@Parcelize
data class LoadingNext<T : Parcelable>(
    override val value: T,
) : WithValue<T>(), LoadingInProgress {
    override val copyWithLoadingInProgress: Loadable<T>
        get() = this

    override val copyWithClearedError: Loadable<T>
        get() = this

    override fun copyWithError(error: Throwable?): FailedNext<T> = FailedNext(value, error)

    override fun map(block: (T) -> T): WithValue<T> = LoadingNext(block(value))
}

@Parcelize
data class Ready<T : Parcelable>(override val value: T) : WithValue<T>() {
    override val copyWithLoadingInProgress: LoadingNext<T>
        get() = LoadingNext(value)

    override val copyWithClearedError: Loadable<T>
        get() = this

    override fun copyWithError(error: Throwable?): FailedNext<T> = FailedNext(value, error)

    override fun map(block: (T) -> T): WithValue<T> = Ready(block(value))
}

interface Failed {
    val error: Throwable?
}

@Parcelize
data class FailedNext<T : Parcelable>(
    override val value: T,
    override val error: Throwable?,
) : WithValue<T>(), Failed {
    override val copyWithClearedError: Ready<T>
        get() = Ready(value)

    override val copyWithLoadingInProgress: Loadable<T>
        get() = LoadingNext(value)

    override fun copyWithError(error: Throwable?): FailedNext<T> = FailedNext(value, error)

    override fun map(block: (T) -> T): WithValue<T> = FailedNext(block(value), error)
}

@Parcelize
data class FailedFirst(override val error: Throwable?) : WithoutValue(), Failed {
    override val copyWithLoadingInProgress: LoadingFirst
        get() = LoadingFirst
}
