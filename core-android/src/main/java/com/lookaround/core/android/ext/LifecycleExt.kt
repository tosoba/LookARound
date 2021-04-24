package com.lookaround.core.android.ext

import androidx.lifecycle.Lifecycle

val Lifecycle.isResumed: Boolean
    get() = currentState.isAtLeast(Lifecycle.State.RESUMED)
