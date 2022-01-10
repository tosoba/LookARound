package com.lookaround.core.usecase

import com.lookaround.core.repo.IAppRepo
import dagger.Reusable
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

@Reusable
class IsConnectedFlow @Inject constructor(private val appRepo: IAppRepo) {
    operator fun invoke(): Flow<Boolean> = appRepo.isConnectedFlow
}
