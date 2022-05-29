package com.lookaround

import android.app.Application
import androidx.preference.PreferenceManager
import com.lookaround.core.android.ext.setNightMode
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import timber.log.Timber.DebugTree

@HiltAndroidApp
class LookARoundApp : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) Timber.plant(DebugTree())

        PreferenceManager.getDefaultSharedPreferences(this)
            .getString(
                getString(R.string.preference_theme_key),
                getString(R.string.preference_theme_system_value)
            )
            ?.let(::setNightMode)
    }
}
