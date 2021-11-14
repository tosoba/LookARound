package com.lookaround.core.android.ext

import android.graphics.Color

fun colorContrastingTo(color: Int): Int {
    val hsv = FloatArray(3)
    Color.RGBToHSV(Color.red(color), Color.green(color), Color.blue(color), hsv)
    if (hsv[2] < 0.5) hsv[2] = 0.7f else hsv[2] = 0.3f
    hsv[1] = hsv[1] * 0.2f
    return Color.HSVToColor(hsv)
}
