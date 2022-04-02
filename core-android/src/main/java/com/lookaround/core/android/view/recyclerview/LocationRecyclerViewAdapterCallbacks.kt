package com.lookaround.core.android.view.recyclerview

import android.location.Location
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

interface LocationRecyclerViewAdapterCallbacks {
    fun onBindViewHolder(uuid: UUID, action: (userLocation: Location) -> Unit)
    fun onDetachedFromRecyclerView()
}

fun CoroutineScope.locationRecyclerViewAdapterCallbacks(
    locationFlow: Flow<Location>,
): LocationRecyclerViewAdapterCallbacks =
    object : LocationRecyclerViewAdapterCallbacks {
        private val jobs = mutableMapOf<UUID, Job>()

        override fun onBindViewHolder(uuid: UUID, action: (userLocation: Location) -> Unit) {
            jobs[uuid]?.cancel()
            jobs[uuid] =
                locationFlow.onEach(action).launchIn(this@locationRecyclerViewAdapterCallbacks)
        }

        override fun onDetachedFromRecyclerView() {
            jobs.values.forEach(Job::cancel)
        }
    }
