package com.lookaround.ui.search.model

sealed class SearchIntent {
    data class QueryChanged(val query: String) : SearchIntent()
}
