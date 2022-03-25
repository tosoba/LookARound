package com.lookaround.core.android.view.recyclerview

import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

interface ColorRecyclerViewAdapterCallbacks {
    fun onViewAttachedToWindow(uuid: UUID, action: (color: Int) -> Unit)
    fun onViewDetachedFromWindow(uuid: UUID)
    fun onDetachedFromRecyclerView()
}

fun CoroutineScope.colorRecyclerViewAdapterCallbacks(
    contrastingColorsFlow: Flow<Int>,
): ColorRecyclerViewAdapterCallbacks =
    object : ColorRecyclerViewAdapterCallbacks {
        private val contrastingColorJobs = mutableMapOf<UUID, Job>()

        override fun onViewAttachedToWindow(uuid: UUID, action: (Int) -> Unit) {
            if (contrastingColorJobs.containsKey(uuid)) return
            contrastingColorJobs[uuid] =
                contrastingColorsFlow.onEach(action).launchIn(this@colorRecyclerViewAdapterCallbacks)
        }

        override fun onViewDetachedFromWindow(uuid: UUID) {
            contrastingColorJobs.remove(uuid)?.cancel()
        }

        override fun onDetachedFromRecyclerView() {
            contrastingColorJobs.values.forEach(Job::cancel)
        }
    }
