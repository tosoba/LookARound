package com.lookaround.core.android.ar.listener

interface AREventsListener {
    fun onAREnabled() = Unit
    fun onARLoading() = Unit
    fun onARDisabled(anyPermissionDenied: Boolean, locationDisabled: Boolean) = Unit
    fun onCameraTouch() = Unit
}
