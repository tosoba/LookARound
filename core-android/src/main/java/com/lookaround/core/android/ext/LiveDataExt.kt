package com.lookaround.core.android.ext

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

fun <T> LiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, onChanged: (T) -> Boolean) {
    observe(lifecycleOwner, object : Observer<T> {
        override fun onChanged(t: T) {
            if (onChanged(t)) removeObserver(this)
        }
    })
}
