package com.lookaround.ui.recent.searches

import com.lookaround.core.android.model.LocationFactory
import com.lookaround.core.android.model.ParcelableList
import com.lookaround.core.android.model.Ready
import com.lookaround.core.model.AutocompleteSearchDTO
import com.lookaround.core.model.SearchAroundDTO
import com.lookaround.core.model.SearchDTO

data class IncreaseLimitUpdate(
    private val increment: Int = RecentSearchesState.SEARCHES_LIMIT_INCREMENT
) : (RecentSearchesState) -> RecentSearchesState {
    override fun invoke(state: RecentSearchesState): RecentSearchesState =
        RecentSearchesState(limit = state.limit + increment)
}

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
                                        type = RecentSearchModel.Type.AUTOCOMPLETE,
                                        location =
                                            if (priorityLat != null && priorityLon != null) {
                                                LocationFactory.create(
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
                                        type = RecentSearchModel.Type.AROUND,
                                        location =
                                            LocationFactory.create(latitude = lat, longitude = lng),
                                        lastSearchedAt = lastSearchedAt
                                    )
                                }
                            }
                        }
                    )
                )
        )
}
