package com.lookaround.ui.camera

interface ARStateListener {
    fun onAREnabled()
    fun onARLoading()
    fun onARDisabled(anyPermissionDenied: Boolean, locationDisabled: Boolean)
}
