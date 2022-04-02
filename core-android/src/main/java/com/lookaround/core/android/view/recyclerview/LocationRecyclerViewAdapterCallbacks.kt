package com.lookaround.core.android.view.recyclerview

import android.location.Location
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

interface LocationRecyclerViewAdapterCallbacks<Id> {
    fun onBindViewHolder(id: Id, action: (userLocation: Location) -> Unit)
    fun onDetachedFromRecyclerView()
}

fun <Id> CoroutineScope.locationRecyclerViewAdapterCallbacks(
    locationFlow: Flow<Location>,
): LocationRecyclerViewAdapterCallbacks<Id> =
    object : LocationRecyclerViewAdapterCallbacks<Id> {
        private val jobs = mutableMapOf<Id, Job>()

        override fun onBindViewHolder(id: Id, action: (userLocation: Location) -> Unit) {
            jobs[id]?.cancel()
            jobs[id] =
                locationFlow.onEach(action).launchIn(this@locationRecyclerViewAdapterCallbacks)
        }

        override fun onDetachedFromRecyclerView() {
            jobs.values.forEach(Job::cancel)
        }
    }
