package com.lookaround.core.usecase

import com.lookaround.core.model.SearchType
import com.lookaround.core.repo.IAutocompleteSearchRepo
import com.lookaround.core.repo.ISearchAroundRepo
import dagger.Reusable
import javax.inject.Inject

@Reusable
class DeleteSearch
@Inject
constructor(
    private val autocompleteSearchRepo: IAutocompleteSearchRepo,
    private val searchAroundRepo: ISearchAroundRepo
) {
    suspend operator fun invoke(id: Long, type: SearchType) {
        when (type) {
            SearchType.AROUND -> searchAroundRepo.deleteSearch(id)
            SearchType.AUTOCOMPLETE -> autocompleteSearchRepo.deleteSearch(id)
        }
    }
}
