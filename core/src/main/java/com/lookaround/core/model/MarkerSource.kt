package com.lookaround.core.model

import java.util.*

sealed interface MarkerSource {
    val timestamp: GregorianCalendar

    data class Type(
        val type: IPlaceType,
        override val timestamp: GregorianCalendar,
    ) : MarkerSource

    data class TextSearch(
        val query: String,
        override val timestamp: GregorianCalendar,
    ) : MarkerSource
}
