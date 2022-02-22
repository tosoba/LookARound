package com.lookaround.ui.recent.searches.model

import com.lookaround.core.android.ext.locationWith
import com.lookaround.core.android.model.ParcelableList
import com.lookaround.core.android.model.Ready
import com.lookaround.core.model.AutocompleteSearchDTO
import com.lookaround.core.model.SearchAroundDTO
import com.lookaround.core.model.SearchDTO
import com.lookaround.core.model.SearchType

object LoadingSearchesUpdate : (RecentSearchesState) -> RecentSearchesState {
    override fun invoke(state: RecentSearchesState): RecentSearchesState =
        RecentSearchesState(searches = state.searches.copyWithLoadingInProgress)
}

data class SearchesLoadedUpdate(
    private val searches: List<SearchDTO>,
) : (RecentSearchesState) -> RecentSearchesState {
    override fun invoke(state: RecentSearchesState): RecentSearchesState =
        RecentSearchesState(
            searches =
                Ready(
                    ParcelableList(
                        searches.map { dto ->
                            when (dto) {
                                is AutocompleteSearchDTO -> {
                                    val (id, query, priorityLat, priorityLon, lastSearchedAt) = dto
                                    RecentSearchModel(
                                        id = id,
                                        label = query,
                                        type = SearchType.AUTOCOMPLETE,
                                        location =
                                            if (priorityLat != null && priorityLon != null) {
                                                locationWith(
                                                    latitude = priorityLat,
                                                    longitude = priorityLon
                                                )
                                            } else {
                                                null
                                            },
                                        lastSearchedAt = lastSearchedAt
                                    )
                                }
                                is SearchAroundDTO -> {
                                    val (id, value, lat, lng, lastSearchedAt) = dto
                                    RecentSearchModel(
                                        id = id,
                                        label = value,
                                        type = SearchType.AROUND,
                                        location = locationWith(latitude = lat, longitude = lng),
                                        lastSearchedAt = lastSearchedAt
                                    )
                                }
                            }
                        }
                    )
                )
        )
}
