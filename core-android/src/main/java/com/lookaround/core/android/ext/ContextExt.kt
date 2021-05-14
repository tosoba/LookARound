package com.lookaround.core.android.ext

import android.content.Context
import android.os.Build
import android.util.TypedValue
import android.view.Surface
import android.view.WindowManager
import java.io.File
import kotlin.math.ceil

val Context.phoneRotation: Int
    get() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.rotation
        } else {
            @Suppress("DEPRECATION")
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay?.rotation
        }
            ?: Surface.ROTATION_0

val Context.statusBarHeight: Int
    get() {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            val heightDp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) 24 else 25
            ceil(heightDp * resources.displayMetrics.density).toInt()
        }
    }

val Context.actionBarHeight: Float
    get() {
        val actionBarStyledAttributes =
            theme.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))
        val actionBarHeight = actionBarStyledAttributes.getDimension(0, 0f)
        actionBarStyledAttributes.recycle()
        return actionBarHeight
    }

val Context.bottomNavigationViewHeight: Int
    get() {
        val resourceId: Int = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            val heightDp = 56
            ceil(heightDp * resources.displayMetrics.density).toInt()
        }
    }

fun Context.dpToPx(value: Float): Float =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)

fun Context.getOrCreateCacheFile(name: String): File? {
    val cacheDir = externalCacheDir
    val tileCacheDir: File?
    if (cacheDir != null) {
        tileCacheDir = File(cacheDir, name)
        if (!tileCacheDir.exists()) tileCacheDir.mkdir()
    } else {
        tileCacheDir = null
    }
    return tileCacheDir
}
