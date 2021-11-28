package com.lookaround.core.usecase

import com.lookaround.core.model.LocationDataDTO
import com.lookaround.core.repo.IAppRepo
import dagger.Reusable
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@Reusable
class LocationDataFlow @Inject constructor(private val appRepo: IAppRepo) {
    operator fun invoke(intervalMillis: Long): Flow<LocationDataDTO> =
        appRepo.getLocationDataFlow(intervalMillis)
}
