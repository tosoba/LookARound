package com.lookaround.core.usecase

import com.lookaround.core.repo.IAppRepo
import dagger.Reusable
import javax.inject.Inject

@Reusable
class IsLocationAvailable @Inject constructor(private val appRepo: IAppRepo) {
    operator fun invoke(): Boolean = appRepo.isLocationAvailable
}
