package com.lookaround.core.model

import com.lookaround.core.ext.titleCaseWithSpacesInsteadOfUnderscores

interface IPlaceType {
    val description: String
    val count: Int

    val label: String
        get() =
            (if (this is Enum<*>) name.titleCaseWithSpacesInsteadOfUnderscores
            else throw IllegalArgumentException())

    val typeKey: String
        get() = javaClass.simpleName.lowercase()

    val typeValue: String
        get() = if (this is Enum<*>) name.lowercase() else throw IllegalArgumentException()
}
