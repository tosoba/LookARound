package com.lookaround.core.android.architecture

interface ListFragmentHost {
    val itemBackground: ItemBackground

    enum class ItemBackground {
        OPAQUE,
        TRANSPARENT
    }
}
