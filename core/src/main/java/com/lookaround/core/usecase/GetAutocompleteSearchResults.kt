package com.lookaround.core.usecase

import com.lookaround.core.model.PointDTO
import com.lookaround.core.repo.IAutocompleteSearchRepo
import dagger.Reusable
import javax.inject.Inject

@Reusable
class GetAutocompleteSearchResults @Inject constructor(private val repo: IAutocompleteSearchRepo) {
    suspend operator fun invoke(searchId: Long): List<PointDTO> = repo.searchResults(searchId)
}
