package com.lookaround.core.android.camera.surface

import android.view.View
import android.view.ViewStub
import com.google.common.util.concurrent.ListenableFuture
import com.lookaround.core.android.camera.OpenGLRenderer

internal interface IRenderSurface {
    fun waitForNextFrame(): ListenableFuture<Unit>
    fun inflateWith(viewStub: ViewStub, renderer: OpenGLRenderer): View
}
