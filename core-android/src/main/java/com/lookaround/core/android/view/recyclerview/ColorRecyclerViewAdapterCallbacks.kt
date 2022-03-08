package com.lookaround.core.android.view.recyclerview

import java.util.*

interface ColorRecyclerViewAdapterCallbacks {
    fun onViewAttachedToWindow(uuid: UUID, action: (color: Int) -> Unit)
    fun onViewDetachedFromWindow(uuid: UUID)
    fun onDetachedFromRecyclerView()
}
