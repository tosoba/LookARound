package com.lookaround.core.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize data class BottomSheetState(val state: Int, val changedByUser: Boolean) : Parcelable
