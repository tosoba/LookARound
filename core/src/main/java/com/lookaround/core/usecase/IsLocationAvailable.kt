package com.lookaround.core.usecase

import com.lookaround.core.repo.IAppRepo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IsLocationAvailable @Inject constructor(private val appRepo: IAppRepo) {
    operator fun invoke(): Boolean = appRepo.isLocationAvailable
}
