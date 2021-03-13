package com.lookaround.ui.main

import android.os.Parcelable
import com.lookaround.core.android.model.Marker
import kotlinx.parcelize.Parcelize

@Parcelize data class MainState(val markers: List<Marker>) : Parcelable
