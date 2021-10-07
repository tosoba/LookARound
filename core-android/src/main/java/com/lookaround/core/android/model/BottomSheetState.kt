package com.lookaround.core.android.model

import android.os.Parcelable
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.parcelize.Parcelize

@Parcelize
data class BottomSheetState(
    @BottomSheetBehavior.State val state: Int,
    val changedByUser: Boolean,
) : Parcelable
