package com.lookaround.core.model

import java.util.*

interface IPlaceType {
    val description: String
    val count: Int

    val label: String
        get() =
            if (this is Enum<*>) {
                name.replace("_", " ").lowercase(Locale.getDefault()).replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
            } else {
                throw IllegalArgumentException()
            }

    val typeKey: String
        get() = javaClass.simpleName

    val typeValue: String
        get() =
            if (this is Enum<*>) name.lowercase(Locale.getDefault())
            else throw IllegalArgumentException()
}
