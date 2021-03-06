package com.lookaround.core.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class Loadable<out T : Parcelable> : Parcelable {
    open val copyWithLoadingInProgress: Loadable<T>
        get() = LoadingFirst

    open val copyWithClearedError: Loadable<T>
        get() = Empty

    open fun copyWithError(error: Throwable?): Loadable<T> = FailedFirst(error)
}

sealed class WithValue<out T : Parcelable> : Loadable<T>() {
    abstract val value: T
}

sealed class WithoutValue : Loadable<Nothing>()

@Parcelize object Empty : WithoutValue()

interface LoadingInProgress

@Parcelize object LoadingFirst : WithoutValue(), LoadingInProgress

@Parcelize
data class LoadingNext<out T : Parcelable>(override val value: T) :
    WithValue<T>(), LoadingInProgress {

    override val copyWithLoadingInProgress: Loadable<T>
        get() = this

    override val copyWithClearedError: Loadable<T>
        get() = this

    override fun copyWithError(error: Throwable?): FailedNext<T> = FailedNext(value, error)
}

@Parcelize
data class Ready<out T : Parcelable>(override val value: T) : WithValue<T>() {
    override val copyWithLoadingInProgress: LoadingNext<T>
        get() = LoadingNext(value)

    override val copyWithClearedError: Loadable<T>
        get() = this

    override fun copyWithError(error: Throwable?): FailedNext<T> = FailedNext(value, error)
}

interface Failed {
    val error: Throwable?
}

@Parcelize
data class FailedNext<out T : Parcelable>(override val value: T, override val error: Throwable?) :
    WithValue<T>(), Failed {

    override val copyWithClearedError: Ready<T>
        get() = Ready(value)

    override val copyWithLoadingInProgress: Loadable<T>
        get() = LoadingNext(value)

    override fun copyWithError(error: Throwable?): FailedNext<T> = FailedNext(value, error)
}

@Parcelize
data class FailedFirst(override val error: Throwable?) : WithoutValue(), Failed {
    override val copyWithLoadingInProgress: LoadingFirst
        get() = LoadingFirst
}
