package com.lookaround.ui.search.exception

import android.os.Parcelable
import java.lang.IllegalArgumentException
import kotlinx.parcelize.Parcelize

@Parcelize
object QueryTooShortExcecption :
    IllegalArgumentException("Query is too short to perform search."), Parcelable
