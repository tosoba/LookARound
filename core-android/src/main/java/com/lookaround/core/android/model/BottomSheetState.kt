package com.lookaround.core.android.model

import android.os.Parcelable
import biz.laenger.android.vpbs.ViewPagerBottomSheetBehavior
import kotlinx.parcelize.Parcelize

@Parcelize
data class BottomSheetState(
    @ViewPagerBottomSheetBehavior.State val state: Int,
    val changedByUser: Boolean
) : Parcelable
