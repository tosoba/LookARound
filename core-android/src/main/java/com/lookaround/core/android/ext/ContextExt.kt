package com.lookaround.core.android.ext

import android.content.Context
import android.os.Build
import android.view.Surface
import android.view.WindowManager

val Context.phoneRotation: Int
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        display?.rotation
    } else {
        @Suppress("DEPRECATION")
        (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay?.rotation
    } ?: Surface.ROTATION_0
