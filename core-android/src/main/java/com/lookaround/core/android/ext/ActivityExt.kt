package com.lookaround.core.android.ext

import android.app.Activity
import android.graphics.Color
import android.view.View
import android.view.WindowManager

fun Activity.setFullScreenWithTransparentBars() {
    window.statusBarColor = Color.TRANSPARENT
    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    window.decorView.systemUiVisibility =
        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
}
