package com.lookaround.core.android.exception

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
object GooglePayServicesNotAvailableException :
    RuntimeException("Google Play Services are not available."), Parcelable
