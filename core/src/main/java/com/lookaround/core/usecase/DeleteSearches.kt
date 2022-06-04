package com.lookaround.core.usecase

import com.lookaround.core.repo.IAutocompleteSearchRepo
import com.lookaround.core.repo.ISearchAroundRepo
import dagger.Reusable
import javax.inject.Inject

@Reusable
class DeleteSearches
@Inject
constructor(
    private val autocompleteSearchRepo: IAutocompleteSearchRepo,
    private val searchAroundRepo: ISearchAroundRepo
) {
    suspend operator fun invoke() {
        autocompleteSearchRepo.deleteAll()
        searchAroundRepo.deleteAll()
    }
}
