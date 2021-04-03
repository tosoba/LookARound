package com.lookaround.core.android.ext

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle

inline fun <reified S : Parcelable> SavedStateHandle.initialState(): S =
    get(S::class.java.simpleName) ?: S::class.java.newInstance()
