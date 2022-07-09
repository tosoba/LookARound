package com.lookaround.core.android.exception

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
object LocationUpdateFailureException : RuntimeException("Failed to update location."), Parcelable
