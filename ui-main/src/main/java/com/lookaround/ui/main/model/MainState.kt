package com.lookaround.ui.main.model

import android.os.Parcelable
import com.lookaround.core.android.model.Empty
import com.lookaround.core.android.model.Loadable
import com.lookaround.core.android.model.Marker
import com.lookaround.core.android.model.ParcelableList
import kotlinx.parcelize.Parcelize

@Parcelize data class MainState(val markers: Loadable<ParcelableList<Marker>> = Empty) : Parcelable
