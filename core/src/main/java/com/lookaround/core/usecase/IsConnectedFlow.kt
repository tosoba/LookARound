package com.lookaround.core.usecase

import com.lookaround.core.repo.IAppRepo
import dagger.Reusable
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@Reusable
class IsConnectedFlow @Inject constructor(private val appRepo: IAppRepo) {
    operator fun invoke(): Flow<Boolean> = appRepo.isConnectedFlow
}
