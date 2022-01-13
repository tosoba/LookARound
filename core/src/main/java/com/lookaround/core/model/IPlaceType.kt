package com.lookaround.core.model

interface IPlaceType {
    val description: String
    val count: Int

    val label: String
        get() =
            if (this is Enum<*>) {
                name.replace("_", " ").lowercase().replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase() else it.toString()
                }
            } else {
                throw IllegalArgumentException()
            }

    val typeKey: String
        get() = javaClass.simpleName.lowercase()

    val typeValue: String
        get() = if (this is Enum<*>) name.lowercase() else throw IllegalArgumentException()
}
