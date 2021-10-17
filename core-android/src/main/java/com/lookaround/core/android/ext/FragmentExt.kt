package com.lookaround.core.android.ext

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import com.lookaround.core.android.R
import kotlin.properties.ReadOnlyProperty

fun FragmentTransaction.setSlideInFromBottom() {
    setCustomAnimations(
        R.anim.slide_in_bottom,
        R.anim.slide_out_top,
        R.anim.slide_in_top,
        R.anim.slide_out_bottom
    )
}

inline fun <reified T> nullableArgument(name: String? = null): ReadOnlyProperty<Fragment, T?> =
        ReadOnlyProperty { thisRef, property ->
    thisRef.arguments?.get(name ?: property.name) as? T
}

inline fun <reified T> argument(name: String? = null): ReadOnlyProperty<Fragment, T> =
        ReadOnlyProperty { thisRef, property ->
    thisRef.arguments?.get(name ?: property.name) as? T
        ?: throw RuntimeException("Argument named: ${name?:property.name} not present in bundle.")
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
