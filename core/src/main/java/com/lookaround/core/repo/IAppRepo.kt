package com.lookaround.core.repo

import com.lookaround.core.model.LocationDataDTO
import kotlinx.coroutines.flow.Flow

interface IAppRepo {
    val isConnectedFlow: Flow<Boolean>
    val isLocationAvailable: Boolean
    fun getLocationDataFlow(intervalMillis: Long): Flow<LocationDataDTO>
}
