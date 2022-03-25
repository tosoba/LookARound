package com.lookaround.core.android.view.recyclerview

import android.location.Location
import java.util.*

interface LocationRecyclerViewAdapterCallbacks {
    fun onBindViewHolder(uuid: UUID, action: (userLocation: Location) -> Unit)
    fun onDetachedFromRecyclerView()
}
