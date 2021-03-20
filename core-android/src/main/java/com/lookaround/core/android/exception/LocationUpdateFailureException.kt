package com.lookaround.core.android.exception

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
object LocationUpdateFailureException : Throwable("Failed to update location."), Parcelable
