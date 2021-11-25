package com.lookaround.core.usecase

import com.lookaround.core.repo.IPlacesAutocompleteRepo
import com.lookaround.core.repo.IPlacesRepo
import dagger.Reusable
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart

@Reusable
class SearchesCountFlow
@Inject
constructor(
    private val placesRepo: IPlacesRepo,
    private val autocompleteRepo: IPlacesAutocompleteRepo
) {
    operator fun invoke(): Flow<Int> =
        combine(
            placesRepo.searchesAroundCount.onStart { emit(0) },
            autocompleteRepo.autocompleteSearchesCount.onStart { emit(0) }
        ) { aroundSearchesCount, autocompleteSearchesCount ->
            aroundSearchesCount + autocompleteSearchesCount
        }
}
