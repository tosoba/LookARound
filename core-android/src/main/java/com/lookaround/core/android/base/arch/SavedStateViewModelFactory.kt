package com.lookaround.core.android.base.arch

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

interface SavedStateViewModelFactory<VM : ViewModel> {
    fun create(savedStateHandle: SavedStateHandle): VM
}
