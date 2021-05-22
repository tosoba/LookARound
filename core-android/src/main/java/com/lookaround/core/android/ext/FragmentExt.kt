package com.lookaround.core.android.ext

import androidx.fragment.app.FragmentTransaction
import com.lookaround.core.android.R

fun FragmentTransaction.setSlideInFromBottom() {
    setCustomAnimations(
        R.anim.slide_in_bottom,
        R.anim.slide_out_top,
        R.anim.slide_in_top,
        R.anim.slide_out_bottom
    )
}
