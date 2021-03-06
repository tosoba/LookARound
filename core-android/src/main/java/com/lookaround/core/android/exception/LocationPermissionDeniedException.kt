package com.lookaround.core.android.exception

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize object LocationPermissionDeniedException : Throwable("Location permission denied."), Parcelable
