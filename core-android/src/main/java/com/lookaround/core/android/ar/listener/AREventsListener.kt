package com.lookaround.core.android.ar.listener

interface AREventsListener {
    fun onAREnabled() = Unit
    fun onARLoading() = Unit
    fun onARDisabled() = Unit
    fun onCameraTouch(targetVisibility: Int) = Unit
}
