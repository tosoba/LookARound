package com.lookaround.core.android.camera

import android.view.View
import android.view.ViewStub
import com.google.common.util.concurrent.ListenableFuture

internal interface IRenderSurface {
    fun waitForNextFrame(): ListenableFuture<Unit>
    fun inflateWith(viewStub: ViewStub, renderer: OpenGLRenderer): View
}
