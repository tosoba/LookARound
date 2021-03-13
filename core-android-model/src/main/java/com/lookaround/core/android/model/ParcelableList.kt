package com.lookaround.core.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ParcelableList<T : Parcelable>(val items: List<T>) : Parcelable, List<T> by items {
    operator fun plus(other: List<T>): ParcelableList<T> = ParcelableList(items + other)
}
