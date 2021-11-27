package com.lookaround.core.android.view.viewpager

import androidx.fragment.app.Fragment

interface ViewPagerFragmentFactory {
    val newInstance: () -> Fragment
    val fragmentId: Long
}
