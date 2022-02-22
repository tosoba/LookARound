package com.lookaround.core.android.architecture

interface ListFragmentHost {
    val listItemBackground: ItemBackground

    enum class ItemBackground {
        OPAQUE,
        TRANSPARENT
    }
}
