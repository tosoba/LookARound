package com.lookaround.core.android.view.composable

import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner

private val localBackPressedDispatcher =
    compositionLocalOf<OnBackPressedDispatcher> { error("No OnBackPressedDispatcher found!") }

@Composable
fun BackButtonAction(onBackPressed: () -> Unit) {
    CompositionLocalProvider(
        localBackPressedDispatcher provides
            (LocalLifecycleOwner.current as ComponentActivity).onBackPressedDispatcher
    ) { BackButtonHandler(onBackPressed = onBackPressed::invoke) }
}

@Composable
private fun BackButtonHandler(enabled: Boolean = true, onBackPressed: () -> Unit) {
    val dispatcher = localBackPressedDispatcher.current
    val backCallback = remember {
        object : OnBackPressedCallback(enabled) {
            override fun handleOnBackPressed() {
                onBackPressed.invoke()
            }
        }
    }
    DisposableEffect(dispatcher) {
        dispatcher.addCallback(backCallback)
        onDispose(backCallback::remove)
    }
}
