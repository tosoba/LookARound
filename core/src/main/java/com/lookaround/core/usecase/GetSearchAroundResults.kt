package com.lookaround.core.usecase

import com.lookaround.core.model.NodeDTO
import com.lookaround.core.repo.ISearchAroundRepo
import dagger.Reusable
import javax.inject.Inject

@Reusable
class GetSearchAroundResults @Inject constructor(private val repo: ISearchAroundRepo) {
    suspend operator fun invoke(searchId: Long): List<NodeDTO> = repo.searchResults(searchId)
}
