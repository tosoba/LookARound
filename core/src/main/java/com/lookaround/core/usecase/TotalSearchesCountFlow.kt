package com.lookaround.core.usecase

import com.lookaround.core.repo.IAutocompleteSearchRepo
import com.lookaround.core.repo.ISearchAroundRepo
import dagger.Reusable
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart

@Reusable
class TotalSearchesCountFlow
@Inject
constructor(
    private val searchAroundRepo: ISearchAroundRepo,
    private val autocompleteSearchRepo: IAutocompleteSearchRepo
) {
    operator fun invoke(): Flow<Int> =
        combine(
            searchAroundRepo.searchesAroundCount.onStart { emit(0) },
            autocompleteSearchRepo.autocompleteSearchesCount.onStart { emit(0) }
        ) { aroundSearchesCount, autocompleteSearchesCount ->
            aroundSearchesCount + autocompleteSearchesCount
        }
}
