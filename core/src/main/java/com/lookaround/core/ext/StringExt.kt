package com.lookaround.core.ext

val String.titleCaseWithSpacesInsteadOfUnderscores: String
    get() =
        replace("_", " ").lowercase().replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }
