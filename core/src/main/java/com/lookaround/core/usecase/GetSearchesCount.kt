package com.lookaround.core.usecase

import com.lookaround.core.repo.IAutocompleteSearchRepo
import com.lookaround.core.repo.ISearchAroundRepo
import dagger.Reusable
import javax.inject.Inject

@Reusable
class GetSearchesCount
@Inject
constructor(
    private val searchAroundRepo: ISearchAroundRepo,
    private val autocompleteSearchRepo: IAutocompleteSearchRepo
) {
    suspend operator fun invoke(query: String? = null): Int =
        searchAroundRepo.getSearchesAroundCount(query) +
            autocompleteSearchRepo.getAutocompleteSearchesCount(query)
}
