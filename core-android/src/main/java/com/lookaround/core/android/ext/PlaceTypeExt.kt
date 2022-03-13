package com.lookaround.core.android.ext

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat

fun Context.getDrawableOfPlaceTypeBy(name: String): Drawable? =
    ContextCompat.getDrawable(
        this,
        resources.getIdentifier(name.lowercase(), "drawable", packageName)
    )
