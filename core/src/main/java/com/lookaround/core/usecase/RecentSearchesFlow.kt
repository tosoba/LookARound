package com.lookaround.core.usecase

import com.lookaround.core.model.SearchDTO
import com.lookaround.core.repo.IPlacesAutocompleteRepo
import com.lookaround.core.repo.IPlacesRepo
import dagger.Reusable
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Reusable
class RecentSearchesFlow
@Inject
constructor(
    private val placesRepo: IPlacesRepo,
    private val autocompleteRepo: IPlacesAutocompleteRepo
) {
    operator fun invoke(limit: Int): Flow<List<SearchDTO>> =
        placesRepo.recentSearchesAround(limit).combine(
                autocompleteRepo.recentAutocompleteSearches(limit)
            ) { recentSearchesAround, recentAutocompleteSearches ->
            recentSearchesAround
                .union(recentAutocompleteSearches)
                .toSortedSet { search1, search2 ->
                    search1.lastSearchedAt.compareTo(search2.lastSearchedAt)
                }
                .take(limit)
        }
}
