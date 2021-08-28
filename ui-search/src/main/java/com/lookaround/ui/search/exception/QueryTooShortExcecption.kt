package com.lookaround.ui.search.exception

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
object QueryTooShortExcecption :
    IllegalArgumentException("Query is too short to perform search."), Parcelable
