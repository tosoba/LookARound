package com.lookaround.core.android.ext

import android.content.Context
import android.content.res.Configuration
import android.graphics.RectF
import android.os.Build
import android.util.Size
import android.util.TypedValue
import android.view.Surface
import android.view.WindowInsets
import android.view.WindowManager
import androidx.preference.PreferenceManager
import androidx.room.Room
import androidx.room.RoomDatabase
import com.lookaround.core.android.R
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

val Context.statusBarHeightPx: Int
    get() {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            val heightDp = 24
            ceil(heightDp * resources.displayMetrics.density).toInt()
        }
    }

val Context.actionBarHeightPx: Float
    get() {
        val actionBarStyledAttributes =
            theme.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))
        val actionBarHeight = actionBarStyledAttributes.getDimension(0, 0f)
        actionBarStyledAttributes.recycle()
        return actionBarHeight
    }

val Context.bottomNavigationViewHeightPx: Int
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

fun Context.pxToDp(value: Float): Float = value / resources.displayMetrics.density

fun Context.spToPx(value: Float): Float =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)

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

inline fun <reified T : RoomDatabase> Context.buildRoom(
    inMemory: Boolean = false,
    name: String = T::class.java.simpleName,
    noinline configure: (RoomDatabase.Builder<T>.() -> RoomDatabase.Builder<T>)? = null
): T {
    val builder =
        if (inMemory) Room.inMemoryDatabaseBuilder(this, T::class.java)
        else Room.databaseBuilder(this, T::class.java, name)
    if (configure != null) builder.configure()
    return builder.build()
}

val Context.bottomNavigationViewRectF: RectF
    get() {
        val screenSize = getScreenSize(includeTopInset = true)
        val bottomNavigationViewHeight = dpToPx(56f)
        return RectF(
            0f,
            screenSize.height - bottomNavigationViewHeight,
            screenSize.width.toFloat(),
            screenSize.height.toFloat()
        )
    }

fun Context.getScreenSize(
    includeLeftInset: Boolean = false,
    includeTopInset: Boolean = false,
    includeRightInset: Boolean = false,
    includeBottomInset: Boolean = false
): Size =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val metrics =
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).currentWindowMetrics
        val insets =
            metrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.navigationBars() or WindowInsets.Type.displayCutout()
            )
        val bounds = metrics.bounds
        var width = bounds.width()
        if (!includeRightInset) width -= insets.right
        if (!includeLeftInset) width -= insets.left
        var height = bounds.height()
        if (!includeTopInset) height -= insets.top
        if (!includeBottomInset) height -= insets.bottom
        Size(width, height)
    } else {
        val displayMetrics = resources.displayMetrics
        Size(displayMetrics.widthPixels, displayMetrics.heightPixels)
    }

fun isRunningOnEmulator(): Boolean {
    return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic") ||
        Build.FINGERPRINT.startsWith("generic") ||
        Build.FINGERPRINT.startsWith("unknown") ||
        Build.HARDWARE.contains("goldfish") ||
        Build.HARDWARE.contains("ranchu") ||
        Build.MODEL.contains("google_sdk") ||
        Build.MODEL.contains("Emulator") ||
        Build.MODEL.contains("Android SDK built for x86") ||
        Build.MANUFACTURER.contains("Genymotion") ||
        Build.PRODUCT.contains("sdk_google") ||
        Build.PRODUCT.contains("google_sdk") ||
        Build.PRODUCT.contains("sdk") ||
        Build.PRODUCT.contains("sdk_x86") ||
        Build.PRODUCT.contains("sdk_gphone64_arm64") ||
        Build.PRODUCT.contains("vbox86p") ||
        Build.PRODUCT.contains("emulator") ||
        Build.PRODUCT.contains("simulator"))
}

val Context.uiMode: Int
    get() = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

val Context.darkMode: Boolean
    get() =
        when (PreferenceManager.getDefaultSharedPreferences(this)
                .getString(
                    getString(R.string.preference_theme_key),
                    getString(R.string.preference_theme_system_value)
                )
        ) {
            getString(R.string.preference_theme_light_value) -> {
                false
            }
            getString(R.string.preference_theme_dark_value) -> {
                true
            }
            getString(R.string.preference_theme_system_value) -> {
                uiMode == Configuration.UI_MODE_NIGHT_YES
            }
            else -> {
                throw IllegalStateException()
            }
        }
