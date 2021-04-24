package com.lookaround.core.usecase

import com.lookaround.core.model.PointDTO
import com.lookaround.core.repo.IPlacesAutocompleteRepo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchPoints @Inject constructor(private val autocompleteRepo: IPlacesAutocompleteRepo) {
    suspend operator fun invoke(
        query: String,
        priorityLat: Double? = null,
        priorityLon: Double? = null
    ): List<PointDTO> = autocompleteRepo.searchPoints(query, priorityLat, priorityLon)
}
