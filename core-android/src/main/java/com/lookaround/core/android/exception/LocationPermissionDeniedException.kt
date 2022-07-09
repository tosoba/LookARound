package com.lookaround.core.android.exception

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
object LocationPermissionDeniedException :
    RuntimeException("Location permission denied."), Parcelable
