package com.lookaround.core.android.model

import android.os.Parcelable
import java.util.*
import kotlinx.parcelize.Parcelize

@Parcelize
data class ParcelableList<T : Parcelable>(val items: List<T>) : Parcelable, List<T> by items {
    operator fun plus(other: List<T>): ParcelableList<T> = ParcelableList(items + other)
}

@Parcelize
data class ParcelableSortedSet<T : Parcelable>(
    val items: SortedSet<T>,
) : Parcelable, SortedSet<T> by items {
    operator fun plus(other: Collection<T>): ParcelableSortedSet<T> =
        ParcelableSortedSet(items.apply { addAll(other) })
}

@Parcelize
data class ParcelableLinkedHashMap<K : Parcelable, V : Parcelable>(
    val items: LinkedHashMap<K, V>,
) : Parcelable, Map<K, V> by items {
    operator fun plus(entry: Pair<K, V>): ParcelableLinkedHashMap<K, V> =
        ParcelableLinkedHashMap(items.apply { put(entry.first, entry.second) })
}
