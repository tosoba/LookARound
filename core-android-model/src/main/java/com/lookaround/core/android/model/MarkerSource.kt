package com.lookaround.core.android.model

import android.os.Parcelable
import com.lookaround.core.model.IPlaceType
import java.util.*
import kotlinx.parcelize.Parcelize

sealed interface MarkerSource : Parcelable {
    val timestamp: GregorianCalendar

    @Parcelize
    data class Type<T>(
        val type: T,
        override val timestamp: GregorianCalendar,
    ) : MarkerSource where T : IPlaceType, T : Parcelable

    @Parcelize
    data class TextSearch(
        val query: String,
        override val timestamp: GregorianCalendar,
    ) : MarkerSource
}
