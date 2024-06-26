package com.lookaround.core.model

import java.util.*

sealed interface SearchDTO {
    val id: Long
    val lastSearchedAt: Date
}
