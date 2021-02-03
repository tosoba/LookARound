package com.lookaround.core.android.repo

import android.annotation.SuppressLint
import android.content.Context
import com.lookaround.core.repo.IAppRepo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.beryukhov.reactivenetwork.ReactiveNetwork
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepo @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reactiveNetwork: ReactiveNetwork
) : IAppRepo {
    override val isConnectedFlow: Flow<Boolean>
        @SuppressLint("MissingPermission")
        get() = reactiveNetwork.observeNetworkConnectivity(context).map { it.available }
}
