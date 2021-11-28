package com.lookaround.core.usecase

import com.lookaround.core.model.PointDTO
import com.lookaround.core.repo.IAutocompleteSearchRepo
import dagger.Reusable
import javax.inject.Inject

@Reusable
class AutocompleteSearch
@Inject
constructor(private val autocompleteSearchRepo: IAutocompleteSearchRepo) {
    suspend operator fun invoke(
        query: String,
        priorityLat: Double? = null,
        priorityLon: Double? = null
    ): List<PointDTO> = autocompleteSearchRepo.search(query, priorityLat, priorityLon)
}
