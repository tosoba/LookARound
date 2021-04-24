package com.lookaround.ui.search.exception

import android.os.Parcelable
import java.lang.IllegalArgumentException
import kotlinx.parcelize.Parcelize

@Parcelize
object BlankQueryException : IllegalArgumentException("Query cannot be blank."), Parcelable
