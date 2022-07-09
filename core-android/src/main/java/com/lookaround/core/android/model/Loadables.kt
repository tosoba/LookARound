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

val <T : Parcelable> Loadable<T>.hasValue: Boolean
    get() = this is WithValue<T>

val <T : Parcelable> Loadable<T>.hasNoValue: Boolean
    get() = this is WithoutValue

inline fun <reified T : Parcelable> Loadable<T>.hasNoValueOrEmpty(): Boolean =
    when (this) {
        is WithoutValue -> true
        is WithValue<T> -> {
            val v = value
            if (v is Collection<*>) v.isEmpty() else false
        }
    }

sealed interface WithValue<T : Parcelable> : Loadable<T> {
    val value: T
}

sealed interface WithoutValue : Loadable<Nothing>

@Parcelize
object Empty : Loadable<Nothing>, WithoutValue {
    override fun <R : Parcelable> map(block: (Nothing) -> R): Empty = this
}

sealed interface Loading<T : Parcelable> : Loadable<T>

@Parcelize
object LoadingFirst : WithoutValue, Loading<Nothing> {
    override fun <R : Parcelable> map(block: (Nothing) -> R): Loadable<R> = this
}

@Parcelize
data class LoadingNext<T : Parcelable>(override val value: T) : WithValue<T>, Loading<T> {
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

sealed interface Failed<T : Parcelable> : Loadable<T> {
    val error: Throwable?
}

@Parcelize
data class FailedFirst(override val error: Throwable?) : Failed<Nothing>, WithoutValue {
    override val copyWithLoadingInProgress: LoadingFirst
        get() = LoadingFirst

    override fun <R : Parcelable> map(block: (Nothing) -> R): Loadable<R> = this
}

@Parcelize
data class FailedNext<T : Parcelable>(
    override val value: T,
    override val error: Throwable?,
) : Failed<T>, WithValue<T> {
    override val copyWithClearedError: Ready<T>
        get() = Ready(value)

    override val copyWithLoadingInProgress: Loadable<T>
        get() = LoadingNext(value)

    override fun copyWithError(error: Throwable?): FailedNext<T> = FailedNext(value, error)

    override fun <R : Parcelable> map(block: (T) -> R): Loadable<R> =
        FailedNext(block(value), error)
}
