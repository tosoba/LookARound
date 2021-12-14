package com.lookaround.core.usecase

import com.lookaround.core.model.SearchDTO
import com.lookaround.core.repo.IAutocompleteSearchRepo
import com.lookaround.core.repo.ISearchAroundRepo
import dagger.Reusable
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Reusable
class RecentSearchesFlow
@Inject
constructor(
    private val searchAroundRepo: ISearchAroundRepo,
    private val autocompleteSearchRepo: IAutocompleteSearchRepo
) {
    operator fun invoke(limit: Int): Flow<List<SearchDTO>> =
        searchAroundRepo.recentSearchesAround(limit).combine(
                autocompleteSearchRepo.recentAutocompleteSearches(limit)
            ) { recentSearchesAround, recentAutocompleteSearches ->
            recentSearchesAround
                .union(recentAutocompleteSearches)
                .toSortedSet { search1, search2 ->
                    search1.lastSearchedAt.compareTo(search2.lastSearchedAt)
                }
                .take(limit)
        }
}