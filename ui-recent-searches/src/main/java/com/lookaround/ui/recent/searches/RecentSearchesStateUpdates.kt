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
                                is AutocompleteSearchDTO ->
                                    RecentSearchModel(
                                        id = dto.id,
                                        label = dto.query,
                                        type = RecentSearchModel.Type.AUTOCOMPLETE,
                                        location =
                                            if (dto.priorityLat != null && dto.priorityLon != null
                                            ) {
                                                LocationFactory.create(
                                                    latitude = dto.priorityLat!!,
                                                    longitude = dto.priorityLon!!
                                                )
                                            } else {
                                                null
                                            }
                                    )
                                is SearchAroundDTO ->
                                    RecentSearchModel(
                                        id = dto.id,
                                        label = dto.value,
                                        type = RecentSearchModel.Type.AROUND,
                                        location =
                                            LocationFactory.create(
                                                latitude = dto.lat,
                                                longitude = dto.lng
                                            )
                                    )
                            }
                        }
                    )
                )
        )
}
