package com.lookaround.core.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface Loadable<out T : Parcelable> : Parcelable {
    val copyWithLoadingInProgress: Loadable<T>
        get() = LoadingFirst

    val copyWithClearedError: Loadable<T>
        get() = Empty

    fun copyWithError(error: Throwable?): Loadable<T> = FailedFirst(error)

    fun <R : Parcelable> map(block: (T) -> R): Loadable<R>
}

inline fun <reified E> Loadable<*>.isFailedWith(): Boolean = (this as? Failed)?.error is E

sealed interface WithValue<T : Parcelable> : Loadable<T> {
    val value: T
}

sealed interface WithoutValue : Loadable<Nothing>

@Parcelize
object Empty : WithoutValue {
    override fun <R : Parcelable> map(block: (Nothing) -> R): Empty = this
}

interface LoadingInProgress

@Parcelize
object LoadingFirst : WithoutValue, LoadingInProgress {
    override fun <R : Parcelable> map(block: (Nothing) -> R): Loadable<R> = this
}

@Parcelize
data class LoadingNext<T : Parcelable>(override val value: T) : WithValue<T>, LoadingInProgress {
    override val copyWithLoadingInProgress: Loadable<T>
        get() = this

    override val copyWithClearedError: Loadable<T>
        get() = this

    override fun copyWithError(error: Throwable?): FailedNext<T> = FailedNext(value, error)

    override fun <R : Parcelable> map(block: (T) -> R): Loadable<R> = LoadingNext(block(value))
}

@Parcelize
data class Ready<T : Parcelable>(override val value: T) : WithValue<T> {
    override val copyWithLoadingInProgress: LoadingNext<T>
        get() = LoadingNext(value)

    override val copyWithClearedError: Loadable<T>
        get() = this

    override fun copyWithError(error: Throwable?): FailedNext<T> = FailedNext(value, error)

    override fun <R : Parcelable> map(block: (T) -> R): Loadable<R> = Ready(block(value))
}

sealed interface Failed {
    val error: Throwable?
}

@Parcelize
data class FailedFirst(override val error: Throwable?) : WithoutValue, Failed {
    override val copyWithLoadingInProgress: LoadingFirst
        get() = LoadingFirst

    override fun <R : Parcelable> map(block: (Nothing) -> R): Loadable<R> = this
}

@Parcelize
data class FailedNext<T : Parcelable>(
    override val value: T,
    override val error: Throwable?,
) : WithValue<T>, Failed {
    override val copyWithClearedError: Ready<T>
        get() = Ready(value)

    override val copyWithLoadingInProgress: Loadable<T>
        get() = LoadingNext(value)

    override fun copyWithError(error: Throwable?): FailedNext<T> = FailedNext(value, error)

    override fun <R : Parcelable> map(block: (T) -> R): Loadable<R> =
        FailedNext(block(value), error)
}
