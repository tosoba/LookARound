package com.lookaround.ui.camera

import android.location.Location
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize data class CameraState(val location: Location? = null) : Parcelable
