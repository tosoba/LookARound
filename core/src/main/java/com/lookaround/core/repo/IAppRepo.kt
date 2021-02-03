package com.lookaround.core.repo

import kotlinx.coroutines.flow.Flow

interface IAppRepo {
    val isConnectedFlow: Flow<Boolean>
}
