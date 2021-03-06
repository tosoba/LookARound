package com.lookaround.core.android.exception

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize object LocationDisabledException : Throwable("Location is disabled."), Parcelable
