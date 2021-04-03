package com.lookaround.core.usecase

import com.lookaround.core.model.LocationDataDTO
import com.lookaround.core.repo.IAppRepo
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationDataFlow @Inject constructor(private val appRepo: IAppRepo) {
    operator fun invoke(intervalMillis: Long): Flow<LocationDataDTO> =
        appRepo.getLocationDataFlow(intervalMillis)
}
