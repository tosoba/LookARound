package com.lookaround.core.android.ext

import android.content.res.Configuration
import android.os.Build
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import com.lookaround.core.android.R
import kotlin.properties.ReadOnlyProperty

fun FragmentTransaction.setSlideVerticallyAnimations() {
    setCustomAnimations(
        R.anim.slide_in_bottom,
        R.anim.slide_out_top,
        R.anim.slide_in_top,
        R.anim.slide_out_bottom
    )
}

fun FragmentTransaction.setFadeAnimations() {
    setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
}

inline fun <reified T> nullableArgument(name: String? = null): ReadOnlyProperty<Fragment, T?> =
    ReadOnlyProperty { thisRef, property ->
        thisRef.arguments?.get(name ?: property.name) as? T
    }

inline fun <reified T> argument(name: String? = null): ReadOnlyProperty<Fragment, T> =
    ReadOnlyProperty { thisRef, property ->
        thisRef.arguments?.get(name ?: property.name) as? T
            ?: throw RuntimeException(
                "Argument named: ${name?:property.name} not present in bundle."
            )
    }

fun FragmentActivity.fragmentTransaction(
    autoCommit: Boolean = true,
    allowingStateLoss: Boolean = false,
    block: FragmentTransaction.() -> Unit
): FragmentTransaction =
    supportFragmentManager.beginTransaction().apply {
        block()
        if (autoCommit) {
            if (allowingStateLoss) commitAllowingStateLoss() else commit()
        }
    }

val Fragment.rotation: Int
    get() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requireNotNull(requireContext().display) { "Display is null" }.rotation
        } else {
            requireActivity().windowManager.defaultDisplay.rotation
        }

fun Fragment.getListItemDimensionPx(
    spacingDp: Float = 10f,
    itemsPortrait: Int = 2,
    itemsLandscape: Int = 4
): Float {
    val spacingPx = requireContext().dpToPx(spacingDp)
    val displayMetrics = resources.displayMetrics
    val orientation = resources.configuration.orientation
    return (displayMetrics.widthPixels -
        spacingPx *
            (if (orientation == Configuration.ORIENTATION_LANDSCAPE) itemsLandscape + 1
            else itemsPortrait + 1)) /
        (if (orientation == Configuration.ORIENTATION_LANDSCAPE) itemsLandscape else itemsPortrait)
}
