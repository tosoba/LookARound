package com.lookaround.core.android.ar.listener

interface ARStateListener {
    fun onAREnabled()
    fun onARLoading()
    fun onARDisabled(anyPermissionDenied: Boolean, locationDisabled: Boolean)
}
