package com.lookaround.core.android.exception

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize object LocationDisabledException : RuntimeException("Location is disabled."), Parcelable
